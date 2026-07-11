package com.dnd.app.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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

    /**
     * Регистрирует команду как обработанную, если её ещё не видели (в пределах TTL).
     *
     * @param clientCommandId идемпотентный ключ команды; {@code null} — дедуп не применяется (возвращает true)
     * @return {@code true}, если это первое появление ключа (команду нужно выполнить); {@code false} — дубликат
     */
    public boolean firstSeen(UUID clientCommandId) {
        if (clientCommandId == null) {
            return true;
        }
        evictExpired();
        return seen.putIfAbsent(clientCommandId, Instant.now()) == null;
    }

    /** Удаляет из кэша записи старше TTL (ленивая очистка при каждом обращении). */
    private void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        seen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
