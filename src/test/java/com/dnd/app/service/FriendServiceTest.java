package com.dnd.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dnd.app.domain.User;
import com.dnd.app.domain.UserRelationship;
import com.dnd.app.domain.enums.RelationshipStatus;
import com.dnd.app.domain.enums.RelationshipView;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.UserSearchResultResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.UserRelationshipRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.media.MediaUrlResolver;
import com.dnd.app.util.UuidOrdering;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendService: normalization, transitions, permissions, no-oracle")
class FriendServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRelationshipRepository relationshipRepository;
    @Mock private MediaUrlResolver mediaUrlResolver;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private FriendRateLimiter rateLimiter;
    @Mock private MessengerClient messengerClient;

    @InjectMocks private FriendService friendService;

    private User user(String name) {
        return User.builder().id(UUID.randomUUID()).username(name).email(name + "@x.test")
                .passwordHash("h").role(Role.PLAYER).build();
    }

    @Test
    @DisplayName("Sending a request stores a normalized PENDING row and notifies the recipient")
    void sendRequest_normalizesAndNotifies() {
        User actor = user("alice");
        User target = user("bob");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(relationshipRepository.findByUserAIdAndUserBId(any(), any())).thenReturn(Optional.empty());
        when(relationshipRepository.save(any(UserRelationship.class))).thenAnswer(inv -> {
            UserRelationship r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        friendService.sendFriendRequest("alice", target.getId());

        ArgumentCaptor<UserRelationship> captor = ArgumentCaptor.forClass(UserRelationship.class);
        verify(relationshipRepository).save(captor.capture());
        UserRelationship saved = captor.getValue();
        UUID[] expected = UuidOrdering.normalizedPair(actor.getId(), target.getId());
        assertThat(saved.getUserAId()).isEqualTo(expected[0]);
        assertThat(saved.getUserBId()).isEqualTo(expected[1]);
        assertThat(UuidOrdering.compareUnsigned(saved.getUserAId(), saved.getUserBId())).isNegative();
        assertThat(saved.getStatus()).isEqualTo(RelationshipStatus.PENDING);
        assertThat(saved.getRequesterId()).isEqualTo(actor.getId());
        verify(webSocketEventService).sendUserEvent(eq("bob"), eq(WebSocketEventType.FRIEND_REQUEST_RECEIVED),
                any(), any(), eq(actor.getId()));
    }

    @Test
    @DisplayName("Cannot friend yourself")
    void sendRequest_self_rejected() {
        User actor = user("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        assertThatThrownBy(() -> friendService.sendFriendRequest("alice", actor.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Any existing relationship (incl. blocked) yields the same 409 — no oracle")
    void sendRequest_existing_conflict() {
        User actor = user("alice");
        User target = user("bob");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(relationshipRepository.findByUserAIdAndUserBId(any(), any()))
                .thenReturn(Optional.of(UserRelationship.builder().status(RelationshipStatus.BLOCKED).build()));

        assertThatThrownBy(() -> friendService.sendFriendRequest("alice", target.getId()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Only the recipient can accept; requester accepting is forbidden")
    void accept_byRequester_forbidden() {
        User requester = user("alice");
        UUID relId = UUID.randomUUID();
        UserRelationship rel = UserRelationship.builder()
                .id(relId).userAId(requester.getId()).userBId(UUID.randomUUID())
                .status(RelationshipStatus.PENDING).requesterId(requester.getId()).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(requester));
        when(relationshipRepository.findById(relId)).thenReturn(Optional.of(rel));

        assertThatThrownBy(() -> friendService.acceptFriendRequest("alice", relId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Recipient accepting transitions PENDING -> FRIENDS and notifies the requester")
    void accept_byRecipient_transitions() {
        User requester = user("alice");
        User recipient = user("bob");
        UUID relId = UUID.randomUUID();
        UUID[] pair = UuidOrdering.normalizedPair(requester.getId(), recipient.getId());
        UserRelationship rel = UserRelationship.builder()
                .id(relId).userAId(pair[0]).userBId(pair[1])
                .status(RelationshipStatus.PENDING).requesterId(requester.getId()).build();
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(relationshipRepository.findById(relId)).thenReturn(Optional.of(rel));
        when(relationshipRepository.save(any(UserRelationship.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));

        friendService.acceptFriendRequest("bob", relId);

        assertThat(rel.getStatus()).isEqualTo(RelationshipStatus.FRIENDS);
        verify(webSocketEventService).sendUserEvent(eq("alice"), eq(WebSocketEventType.FRIEND_REQUEST_ACCEPTED),
                any(), any(), eq(recipient.getId()));
    }

    @Test
    @DisplayName("Blocking overwrites the row to BLOCKED and asks the messenger to close the pair")
    void block_setsBlockedAndClosesMessengerPair() {
        User actor = user("alice");
        User target = user("bob");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(relationshipRepository.findByUserAIdAndUserBId(any(), any())).thenReturn(Optional.empty());
        when(relationshipRepository.save(any(UserRelationship.class))).thenAnswer(inv -> inv.getArgument(0));

        friendService.blockUser("alice", target.getId());

        ArgumentCaptor<UserRelationship> captor = ArgumentCaptor.forClass(UserRelationship.class);
        verify(relationshipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RelationshipStatus.BLOCKED);
        assertThat(captor.getValue().getBlockedById()).isEqualTo(actor.getId());
        verify(messengerClient).closeSessionForPair(any(), any(), eq("RELATIONSHIP_ENDED"));
    }

    @Test
    @DisplayName("Removing a non-friend is a 404")
    void removeFriend_notFriends_notFound() {
        User actor = user("alice");
        UUID other = UUID.randomUUID();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        when(relationshipRepository.findByUserAIdAndUserBId(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> friendService.removeFriend("alice", other))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Search shorter than 3 chars returns empty and never hits the DB")
    void search_tooShort_empty() {
        User actor = user("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        assertThat(friendService.searchUsers("alice", "ab", 10)).isEmpty();
        verify(userRepository, never()).searchByUsernamePrefix(any(), any(), any());
    }

    @Test
    @DisplayName("A block placed by the other user is hidden (shown as NONE) in search")
    void search_blockedByOther_hiddenAsNone() {
        User actor = user("alice");
        User candidate = user("bob");
        UUID[] pair = UuidOrdering.normalizedPair(actor.getId(), candidate.getId());
        UserRelationship rel = UserRelationship.builder()
                .userAId(pair[0]).userBId(pair[1])
                .status(RelationshipStatus.BLOCKED).blockedById(candidate.getId()).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        when(userRepository.searchByUsernamePrefix(eq("bob"), eq(actor.getId()), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(relationshipRepository.findByUserAIdAndUserBId(pair[0], pair[1])).thenReturn(Optional.of(rel));

        List<UserSearchResultResponse> results = friendService.searchUsers("alice", "bob", 10);
        assertThat(results).singleElement()
                .satisfies(r -> assertThat(r.getRelationship()).isEqualTo(RelationshipView.NONE));
    }
}
