package com.dnd.app.service;

import com.dnd.app.domain.BattleCommandIdempotencyRecord;
import com.dnd.app.repository.BattleCommandIdempotencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Идемпотентность боевых команд по {@code clientCommandId} (фаза 2.14). Хранит недавно обработанные
 * идентификаторы в памяти с TTL, чтобы повторная отправка той же команды (даблклик, ретрай сети,
 * дубль WS) не применялась дважды. In-memory реализация рассчитана на один инстанс; при масштабировании
 * заменяется на распределённый кэш/таблицу, интерфейс менять не придётся.
 */
@Service
public class CommandDedupService {

    /** Сколько хранить обработанный id — с запасом перекрывает окно ретраев клиента. */
    private static final Duration TTL = Duration.ofMinutes(5);

    private final Map<UUID, Instant> seen = new ConcurrentHashMap<>();
    private final Map<UUID, ReplayEntry> replay = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private BattleCommandIdempotencyRepository repository;

    /**
     * Регистрирует команду как обработанную, если её ещё не видели (в пределах TTL).
     *
     * @param clientCommandId идемпотентный ключ команды; {@code null} — дедуп не применяется (возвращает true)
     * @return {@code true}, если это первое появление ключа (команду нужно выполнить); {@code false} — дубликат
     */
    @Transactional
    public boolean firstSeen(UUID clientCommandId) {
        if (clientCommandId == null) {
            return true;
        }
        if (repository != null) {
            return firstSeenPersistent(clientCommandId);
        }
        evictExpired();
        return seen.putIfAbsent(clientCommandId, Instant.now()) == null;
    }

    private boolean firstSeenPersistent(UUID clientCommandId) {
        repository.deleteByCreatedAtBefore(Instant.now().minus(TTL));
        if (repository.existsByClientCommandId(clientCommandId)) {
            return false;
        }
        repository.save(BattleCommandIdempotencyRecord.builder()
                .clientCommandId(clientCommandId)
                .createdAt(Instant.now())
                .build());
        return true;
    }

    /**
     * Сохраняет тело успешного ответа для идемпотентного повтора команды.
     *
     * @param clientCommandId идемпотентный ключ команды
     * @param commandType тип команды, чтобы не вернуть ответ другого endpoint
     * @param responseBody сериализованный DTO ответа
     */
    @Transactional
    public void storeResponse(UUID clientCommandId, String commandType, String responseBody) {
        if (clientCommandId == null || responseBody == null) {
            return;
        }
        if (repository != null) {
            repository.findByClientCommandId(clientCommandId).ifPresent(record -> {
                record.setCommandType(commandType);
                record.setResponseBody(responseBody);
                repository.save(record);
            });
            return;
        }
        replay.put(clientCommandId, new ReplayEntry(commandType, responseBody, Instant.now()));
    }

    /**
     * Возвращает ранее сохраненный ответ команды, если ключ и тип совпадают.
     *
     * @param clientCommandId идемпотентный ключ команды
     * @param commandType ожидаемый тип команды
     * @return сериализованный DTO ответа, если он уже записан
     */
    @Transactional(readOnly = true)
    public Optional<String> replayResponse(UUID clientCommandId, String commandType) {
        if (clientCommandId == null) {
            return Optional.empty();
        }
        if (repository != null) {
            return repository.findByClientCommandId(clientCommandId)
                    .filter(record -> commandType == null || commandType.equals(record.getCommandType()))
                    .map(BattleCommandIdempotencyRecord::getResponseBody)
                    .filter(body -> body != null && !body.isBlank());
        }
        evictExpired();
        ReplayEntry entry = replay.get(clientCommandId);
        if (entry == null || (commandType != null && !commandType.equals(entry.commandType()))) {
            return Optional.empty();
        }
        return Optional.ofNullable(entry.responseBody());
    }

    /** Удаляет из кэша записи старше TTL (ленивая очистка при каждом обращении). */
    private void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        seen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
        replay.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }

    private record ReplayEntry(String commandType, String responseBody, Instant createdAt) {
    }
}
