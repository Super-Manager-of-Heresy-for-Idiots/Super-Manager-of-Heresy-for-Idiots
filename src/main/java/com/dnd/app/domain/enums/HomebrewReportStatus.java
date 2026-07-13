package com.dnd.app.domain.enums;

/**
 * Перечисление HomebrewReportStatus описывает состояние жалобы на homebrew-пакет (P2-6).
 * OPEN — новая жалоба в очереди; RESOLVED — жалоба обработана с действием (пакет отклонён/скрыт);
 * DISMISSED — жалоба отклонена как необоснованная.
 */
public enum HomebrewReportStatus {
    OPEN,
    RESOLVED,
    DISMISSED
}
