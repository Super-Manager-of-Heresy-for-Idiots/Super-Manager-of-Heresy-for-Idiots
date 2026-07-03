package com.dnd.app.service;

import com.dnd.app.domain.User;
import com.dnd.app.domain.UserRelationship;
import com.dnd.app.domain.enums.FriendRequestDirection;
import com.dnd.app.domain.enums.RelationshipStatus;
import com.dnd.app.domain.enums.RelationshipView;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.BlockedUserResponse;
import com.dnd.app.dto.response.FriendNotificationData;
import com.dnd.app.dto.response.FriendRequestResponse;
import com.dnd.app.dto.response.FriendResponse;
import com.dnd.app.dto.response.InternalRelationshipResponse;
import com.dnd.app.dto.response.UserSearchResultResponse;
import com.dnd.app.dto.response.UserSummaryResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.UserRelationshipRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.UuidOrdering;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int MAX_SEARCH_RESULTS = 10;
    private static final String REQUEST_CONFLICT_MESSAGE = "A friend request could not be created.";
    private static final String RELATIONSHIP_ENDED = "RELATIONSHIP_ENDED";

    private final UserRepository userRepository;
    private final UserRelationshipRepository relationshipRepository;
    private final WebSocketEventService webSocketEventService;
    private final FriendRateLimiter rateLimiter;
    private final MessengerClient messengerClient;

    // --- User search ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserSearchResultResponse> searchUsers(String actorUsername, String query, Integer limit) {
        User actor = requireUser(actorUsername);
        rateLimiter.checkUserSearch(actor.getId());
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }
        int pageSize = limit == null || limit <= 0 ? MAX_SEARCH_RESULTS : Math.min(limit, MAX_SEARCH_RESULTS);
        return userRepository.searchByUsernamePrefix(trimmed, actor.getId(), PageRequest.of(0, pageSize)).stream()
                .map(candidate -> UserSearchResultResponse.builder()
                        .id(candidate.getId())
                        .username(candidate.getUsername())
                        .role(candidate.getRole().name())
                        .relationship(relationshipView(actor.getId(), candidate.getId()))
                        .build())
                .toList();
    }

    // --- Friend requests -----------------------------------------------------

    @Transactional
    public FriendRequestResponse sendFriendRequest(String actorUsername, UUID targetUserId) {
        User actor = requireUser(actorUsername);
        rateLimiter.checkFriendRequest(actor.getId());
        if (actor.getId().equals(targetUserId)) {
            throw new BadRequestException("You cannot send a friend request to yourself.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Any existing row (pending, friends or blocked — either direction) yields the SAME conflict,
        // so a blocked user cannot be distinguished from an already-related one (no oracle; TZ 3.3).
        if (findPair(actor.getId(), targetUserId).isPresent()) {
            throw new DuplicateResourceException(REQUEST_CONFLICT_MESSAGE);
        }

        UUID[] pair = UuidOrdering.normalizedPair(actor.getId(), targetUserId);
        UserRelationship relationship = relationshipRepository.save(UserRelationship.builder()
                .userAId(pair[0])
                .userBId(pair[1])
                .status(RelationshipStatus.PENDING)
                .requesterId(actor.getId())
                .build());
        log.info("Friend request created: from={}, to={}", actor.getUsername(), target.getUsername());

        webSocketEventService.sendUserEvent(target.getUsername(), WebSocketEventType.FRIEND_REQUEST_RECEIVED, null,
                new FriendNotificationData(relationship.getId(), actor.getId(), actor.getUsername()), actor.getId());

        return FriendRequestResponse.builder()
                .relationshipId(relationship.getId())
                .userId(target.getId())
                .username(target.getUsername())
                .role(target.getRole().name())
                .direction(FriendRequestDirection.OUTGOING)
                .createdAt(relationship.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listRequests(String actorUsername, FriendRequestDirection direction) {
        User actor = requireUser(actorUsername);
        return relationshipRepository.findByStatusAndMember(RelationshipStatus.PENDING, actor.getId()).stream()
                .filter(rel -> direction == FriendRequestDirection.OUTGOING
                        ? actor.getId().equals(rel.getRequesterId())
                        : !actor.getId().equals(rel.getRequesterId()))
                .map(rel -> toRequestResponse(rel, actor.getId(), direction))
                .toList();
    }

    @Transactional
    public FriendResponse acceptFriendRequest(String actorUsername, UUID relationshipId) {
        User actor = requireUser(actorUsername);
        UserRelationship relationship = requirePendingForRecipient(actor.getId(), relationshipId);
        relationship.setStatus(RelationshipStatus.FRIENDS);
        relationshipRepository.save(relationship);

        UUID requesterId = relationship.getRequesterId();
        User requester = requesterId == null ? null : userRepository.findById(requesterId).orElse(null);
        if (requester != null) {
            webSocketEventService.sendUserEvent(requester.getUsername(), WebSocketEventType.FRIEND_REQUEST_ACCEPTED,
                    null, new FriendNotificationData(relationship.getId(), actor.getId(), actor.getUsername()),
                    actor.getId());
        }
        return requester == null ? null : FriendResponse.builder()
                .id(requester.getId())
                .username(requester.getUsername())
                .role(requester.getRole().name())
                .friendsSince(relationship.getUpdatedAt())
                .build();
    }

    @Transactional
    public void declineFriendRequest(String actorUsername, UUID relationshipId) {
        User actor = requireUser(actorUsername);
        UserRelationship relationship = requirePendingForRecipient(actor.getId(), relationshipId);
        relationshipRepository.delete(relationship);
    }

    @Transactional
    public void cancelFriendRequest(String actorUsername, UUID relationshipId) {
        User actor = requireUser(actorUsername);
        UserRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found."));
        if (relationship.getStatus() != RelationshipStatus.PENDING
                || !actor.getId().equals(relationship.getRequesterId())) {
            throw new AccessDeniedException("You can only cancel your own outgoing request.");
        }
        relationshipRepository.delete(relationship);
    }

    // --- Friends -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(String actorUsername) {
        User actor = requireUser(actorUsername);
        List<FriendResponse> friends = new ArrayList<>();
        for (UserRelationship rel : relationshipRepository.findByStatusAndMember(RelationshipStatus.FRIENDS, actor.getId())) {
            userRepository.findById(otherId(rel, actor.getId())).ifPresent(other -> friends.add(FriendResponse.builder()
                    .id(other.getId())
                    .username(other.getUsername())
                    .role(other.getRole().name())
                    .friendsSince(rel.getUpdatedAt())
                    .build()));
        }
        return friends;
    }

    @Transactional
    public void removeFriend(String actorUsername, UUID otherUserId) {
        User actor = requireUser(actorUsername);
        UserRelationship relationship = findPair(actor.getId(), otherUserId)
                .filter(rel -> rel.getStatus() == RelationshipStatus.FRIENDS)
                .orElseThrow(() -> new ResourceNotFoundException("You are not friends with this user."));
        relationshipRepository.delete(relationship);

        userRepository.findById(otherUserId).ifPresent(other ->
                webSocketEventService.sendUserEvent(other.getUsername(), WebSocketEventType.FRIEND_REMOVED, null,
                        new FriendNotificationData(relationship.getId(), actor.getId(), actor.getUsername()),
                        actor.getId()));
        messengerClient.closeSessionForPair(relationship.getUserAId(), relationship.getUserBId(), RELATIONSHIP_ENDED);
    }

    // --- Blocking ------------------------------------------------------------

    @Transactional
    public void blockUser(String actorUsername, UUID otherUserId) {
        User actor = requireUser(actorUsername);
        if (actor.getId().equals(otherUserId)) {
            throw new BadRequestException("You cannot block yourself.");
        }
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        UUID[] pair = UuidOrdering.normalizedPair(actor.getId(), otherUserId);
        UserRelationship relationship = findPair(actor.getId(), otherUserId)
                .orElseGet(() -> UserRelationship.builder().userAId(pair[0]).userBId(pair[1]).build());
        relationship.setStatus(RelationshipStatus.BLOCKED);
        relationship.setBlockedById(actor.getId());
        relationship.setRequesterId(null);
        relationshipRepository.save(relationship);
        log.info("User blocked: by={}, target={}", actor.getUsername(), otherUserId);

        messengerClient.closeSessionForPair(pair[0], pair[1], RELATIONSHIP_ENDED);
    }

    @Transactional
    public void unblockUser(String actorUsername, UUID otherUserId) {
        User actor = requireUser(actorUsername);
        UserRelationship relationship = findPair(actor.getId(), otherUserId)
                .filter(rel -> rel.getStatus() == RelationshipStatus.BLOCKED
                        && actor.getId().equals(rel.getBlockedById()))
                .orElseThrow(() -> new ResourceNotFoundException("You have no block to remove for this user."));
        relationshipRepository.delete(relationship);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> listBlocked(String actorUsername) {
        User actor = requireUser(actorUsername);
        List<BlockedUserResponse> blocked = new ArrayList<>();
        for (UserRelationship rel : relationshipRepository.findByStatusAndBlockedById(RelationshipStatus.BLOCKED, actor.getId())) {
            userRepository.findById(otherId(rel, actor.getId())).ifPresent(other -> blocked.add(BlockedUserResponse.builder()
                    .id(other.getId())
                    .username(other.getUsername())
                    .role(other.getRole().name())
                    .build()));
        }
        return blocked;
    }

    // --- Internal (service-to-service) --------------------------------------

    @Transactional(readOnly = true)
    public InternalRelationshipResponse resolveRelationship(UUID userId, UUID otherUserId) {
        Optional<UserRelationship> pair = findPair(userId, otherUserId);
        boolean friends = pair.map(rel -> rel.getStatus() == RelationshipStatus.FRIENDS).orElse(false);
        boolean blocked = pair.map(rel -> rel.getStatus() == RelationshipStatus.BLOCKED).orElse(false);

        List<UserSummaryResponse> summaries = new ArrayList<>();
        userRepository.findById(userId).ifPresent(u -> summaries.add(new UserSummaryResponse(u.getId(), u.getUsername())));
        userRepository.findById(otherUserId).ifPresent(u -> summaries.add(new UserSummaryResponse(u.getId(), u.getUsername())));

        return InternalRelationshipResponse.builder()
                .friends(friends)
                .blocked(blocked)
                .userSummaries(summaries)
                .build();
    }

    // --- Helpers -------------------------------------------------------------

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private Optional<UserRelationship> findPair(UUID first, UUID second) {
        UUID[] pair = UuidOrdering.normalizedPair(first, second);
        return relationshipRepository.findByUserAIdAndUserBId(pair[0], pair[1]);
    }

    private UUID otherId(UserRelationship rel, UUID selfId) {
        return rel.getUserAId().equals(selfId) ? rel.getUserBId() : rel.getUserAId();
    }

    private RelationshipView relationshipView(UUID actorId, UUID candidateId) {
        return findPair(actorId, candidateId).map(rel -> switch (rel.getStatus()) {
            case FRIENDS -> RelationshipView.FRIENDS;
            // Only reveal a block to the blocker; if the candidate blocked the actor, show NONE.
            case BLOCKED -> actorId.equals(rel.getBlockedById()) ? RelationshipView.BLOCKED : RelationshipView.NONE;
            case PENDING -> actorId.equals(rel.getRequesterId())
                    ? RelationshipView.PENDING_OUTGOING
                    : RelationshipView.PENDING_INCOMING;
        }).orElse(RelationshipView.NONE);
    }

    private UserRelationship requirePendingForRecipient(UUID actorId, UUID relationshipId) {
        UserRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found."));
        boolean member = actorId.equals(relationship.getUserAId()) || actorId.equals(relationship.getUserBId());
        if (relationship.getStatus() != RelationshipStatus.PENDING || !member
                || actorId.equals(relationship.getRequesterId())) {
            throw new AccessDeniedException("Only the request recipient can perform this action.");
        }
        return relationship;
    }

    private FriendRequestResponse toRequestResponse(UserRelationship rel, UUID actorId, FriendRequestDirection direction) {
        UUID otherId = otherId(rel, actorId);
        User other = userRepository.findById(otherId).orElse(null);
        return FriendRequestResponse.builder()
                .relationshipId(rel.getId())
                .userId(otherId)
                .username(other == null ? null : other.getUsername())
                .role(other == null ? null : other.getRole().name())
                .direction(direction)
                .createdAt(rel.getCreatedAt())
                .build();
    }
}
