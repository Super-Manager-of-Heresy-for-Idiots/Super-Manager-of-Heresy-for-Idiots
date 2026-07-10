package com.dnd.app.service;

import com.dnd.app.dto.response.LoginPageStatsResponse;
import com.dnd.app.repository.CampaignRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Класс LoginPageStatsService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class LoginPageStatsService {

    static final LocalDate VIGIL_START_DATE = LocalDate.of(2026, 6, 23);

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final Clock appClock;

    /**
     * Возвращает результат операции "get stats" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public LoginPageStatsResponse getStats() {
        return LoginPageStatsResponse.builder()
                .campaignCount(campaignRepository.count())
                .userCount(userRepository.count())
                .vigilDays(calculateVigilDays(LocalDate.now(appClock)))
                .build();
    }

    long calculateVigilDays(LocalDate currentDate) {
        return Math.max(0, ChronoUnit.DAYS.between(VIGIL_START_DATE, currentDate));
    }
}
