package com.dnd.app.service;

import com.dnd.app.dto.response.LoginPageStatsResponse;
import com.dnd.app.repository.CampaignRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginPageStatsServiceTest {

    private final CampaignRepository campaignRepository = mock(CampaignRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    @DisplayName("Возвращает количество кампаний, пользователей и прошедшие календарные дни бдения")
    void getStats_returnsCountsAndVigilDays() {
        when(campaignRepository.count()).thenReturn(12L);
        when(userRepository.count()).thenReturn(34L);
        Clock clock = Clock.fixed(Instant.parse("2026-06-26T10:15:30Z"), ZoneOffset.UTC);

        LoginPageStatsService service = new LoginPageStatsService(campaignRepository, userRepository, clock);

        LoginPageStatsResponse stats = service.getStats();

        assertThat(stats.getCampaignCount()).isEqualTo(12L);
        assertThat(stats.getUserCount()).isEqualTo(34L);
        assertThat(stats.getVigilDays()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Дата до начала бдения не дает отрицательное значение")
    void calculateVigilDays_beforeStart_returnsZero() {
        LoginPageStatsService service = new LoginPageStatsService(
                campaignRepository,
                userRepository,
                Clock.systemUTC());

        assertThat(service.calculateVigilDays(LocalDate.of(2026, 6, 22))).isZero();
    }
}
