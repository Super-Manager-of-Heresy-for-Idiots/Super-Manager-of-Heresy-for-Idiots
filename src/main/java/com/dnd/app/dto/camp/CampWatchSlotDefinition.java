package com.dnd.app.dto.camp;

/**
 * Запись CampWatchSlotDefinition описывает определение слота дозора, хранимое в
 * {@code camp_sessions.watch_schedule_json}: номер слота и свободную метку времени.
 * Назначение персонажа живёт отдельно — в {@code camp_participants.watch_slot}.
 * @param slot номер слота дозора
 * @param label метка времени слота, задаваемая мастером
 */
public record CampWatchSlotDefinition(Integer slot, String label) {
}
