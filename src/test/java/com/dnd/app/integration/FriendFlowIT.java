package com.dnd.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.FriendRequestDirection;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.FriendRequestResponse;
import com.dnd.app.dto.response.InternalRelationshipResponse;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.FriendService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Real integration test for the friends feature against a throwaway Postgres (Testcontainers, needs
 * Docker). Booting also validates migration 065 (Liquibase + ddl-auto=validate against the entity).
 * Named *IT so it runs only via {@code gradlew integrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FriendFlowIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-1234");
    }

    @Autowired private FriendService friendService;
    @Autowired private UserRepository userRepository;

    private User createUser(String name) {
        return userRepository.save(User.builder()
                .username(name)
                .email(name + "@example.test")
                .passwordHash("not-a-real-hash")
                .role(Role.PLAYER)
                .build());
    }

    @Test
    void fullRequestCycleThenBlockAndInternalProjection() {
        User alice = createUser("alice-" + UUID.randomUUID().toString().substring(0, 8));
        User bob = createUser("bob-" + UUID.randomUUID().toString().substring(0, 8));

        // Alice sends, Bob accepts.
        FriendRequestResponse request = friendService.sendFriendRequest(alice.getUsername(), bob.getId());
        assertThat(friendService.listRequests(bob.getUsername(), FriendRequestDirection.INCOMING)).hasSize(1);
        assertThat(friendService.listRequests(alice.getUsername(), FriendRequestDirection.OUTGOING)).hasSize(1);

        friendService.acceptFriendRequest(bob.getUsername(), request.getRelationshipId());
        assertThat(friendService.listFriends(alice.getUsername())).extracting(f -> f.getId()).containsExactly(bob.getId());
        assertThat(friendService.listFriends(bob.getUsername())).extracting(f -> f.getId()).containsExactly(alice.getId());

        // Internal projection sees them as friends with both username snapshots.
        InternalRelationshipResponse rel = friendService.resolveRelationship(alice.getId(), bob.getId());
        assertThat(rel.isFriends()).isTrue();
        assertThat(rel.isBlocked()).isFalse();
        assertThat(rel.getUserSummaries()).hasSize(2);

        // A second request while related is rejected with the generic conflict (no oracle).
        assertThatThrownBy(() -> friendService.sendFriendRequest(alice.getUsername(), bob.getId()))
                .isInstanceOf(DuplicateResourceException.class);

        // Alice blocks Bob: friendship gone, block visible only to the blocker.
        friendService.blockUser(alice.getUsername(), bob.getId());
        assertThat(friendService.listFriends(alice.getUsername())).isEmpty();
        assertThat(friendService.listBlocked(alice.getUsername())).extracting(b -> b.getId()).containsExactly(bob.getId());

        InternalRelationshipResponse afterBlock = friendService.resolveRelationship(alice.getId(), bob.getId());
        assertThat(afterBlock.isFriends()).isFalse();
        assertThat(afterBlock.isBlocked()).isTrue();

        // Unblock clears the row.
        friendService.unblockUser(alice.getUsername(), bob.getId());
        assertThat(friendService.listBlocked(alice.getUsername())).isEmpty();
        assertThat(friendService.resolveRelationship(alice.getId(), bob.getId()).isBlocked()).isFalse();
    }

    @Test
    void searchFindsUsersByUsernamePrefix() {
        User alice = createUser("searcher-" + UUID.randomUUID().toString().substring(0, 8));
        String unique = "targetuser" + UUID.randomUUID().toString().substring(0, 6);
        createUser(unique);

        assertThat(friendService.searchUsers(alice.getUsername(), unique.substring(0, 6), 10))
                .anySatisfy(r -> assertThat(r.getUsername()).isEqualTo(unique));
    }
}
