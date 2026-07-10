package com.dnd.app.controller;

import com.dnd.app.domain.enums.FriendRequestDirection;
import com.dnd.app.dto.request.SendFriendRequestRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BlockedUserResponse;
import com.dnd.app.dto.response.FriendRequestResponse;
import com.dnd.app.dto.response.FriendResponse;
import com.dnd.app.dto.response.UserSearchResultResponse;
import com.dnd.app.exception.BadRequestException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Класс FriendController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequiredArgsConstructor
public class FriendController {

    private final com.dnd.app.service.FriendService friendService;

    /**
     * Выполняет операции "search users" в рамках бизнес-логики API.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param limit ограничение размера результата бизнес-операции
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/users/search")
    public ResponseEntity<ApiResponse<List<UserSearchResultResponse>>> searchUsers(
            @RequestParam("username") String username,
            @RequestParam(value = "limit", required = false) Integer limit,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                friendService.searchUsers(authentication.getName(), username, limit)));
    }

    /**
     * Публикует событие операции "send request" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/friends/requests")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication) {
        FriendRequestResponse response = friendService.sendFriendRequest(authentication.getName(), request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Friend request sent"));
    }

    /**
     * Возвращает список для операции "list requests" в рамках бизнес-логики API.
     * @param direction входящее значение direction, используемое бизнес-сценарием
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/friends/requests")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> listRequests(
            @RequestParam(value = "direction", defaultValue = "incoming") String direction,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                friendService.listRequests(authentication.getName(), parseDirection(direction))));
    }

    /**
     * Выполняет операции "accept request" в рамках бизнес-логики API.
     * @param relationshipId идентификатор relationship, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/friends/requests/{relationshipId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptRequest(
            @PathVariable UUID relationshipId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                friendService.acceptFriendRequest(authentication.getName(), relationshipId), "Friend request accepted"));
    }

    /**
     * Выполняет операции "decline request" в рамках бизнес-логики API.
     * @param relationshipId идентификатор relationship, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/friends/requests/{relationshipId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineRequest(
            @PathVariable UUID relationshipId,
            Authentication authentication) {
        friendService.declineFriendRequest(authentication.getName(), relationshipId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Friend request declined"));
    }

    /**
     * Проверяет условие операции "cancel request" в рамках бизнес-логики API.
     * @param relationshipId идентификатор relationship, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/api/friends/requests/{relationshipId}")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @PathVariable UUID relationshipId,
            Authentication authentication) {
        friendService.cancelFriendRequest(authentication.getName(), relationshipId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Friend request cancelled"));
    }

    /**
     * Возвращает список для операции "list friends" в рамках бизнес-логики API.
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/friends")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> listFriends(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.listFriends(authentication.getName())));
    }

    /**
     * Удаляет результат операции "remove friend" в рамках бизнес-логики API.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/api/friends/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable UUID userId,
            Authentication authentication) {
        friendService.removeFriend(authentication.getName(), userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Friend removed"));
    }

    /**
     * Выполняет операции "block" в рамках бизнес-логики API.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/friends/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> block(
            @PathVariable UUID userId,
            Authentication authentication) {
        friendService.blockUser(authentication.getName(), userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "User blocked"));
    }

    /**
     * Выполняет операции "unblock" в рамках бизнес-логики API.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/api/friends/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> unblock(
            @PathVariable UUID userId,
            Authentication authentication) {
        friendService.unblockUser(authentication.getName(), userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "User unblocked"));
    }

    /**
     * Возвращает список для операции "list blocked" в рамках бизнес-логики API.
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/friends/blocked")
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> listBlocked(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.listBlocked(authentication.getName())));
    }

    private FriendRequestDirection parseDirection(String direction) {
        try {
            return FriendRequestDirection.valueOf(direction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("direction must be 'incoming' or 'outgoing'.");
        }
    }
}
