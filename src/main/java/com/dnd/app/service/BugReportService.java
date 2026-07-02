package com.dnd.app.service;

import com.dnd.app.domain.BugReport;
import com.dnd.app.domain.User;
import com.dnd.app.dto.request.CreateBugReportRequest;
import com.dnd.app.dto.response.BugReportResponse;
import com.dnd.app.repository.BugReportRepository;
import com.dnd.app.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BugReportService {

    private final BugReportRepository bugReportRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BugReportResponse create(CreateBugReportRequest request, String username, String ip, String userAgent) {
        UUID userId = resolveUserId(username).orElse(null);
        BugReport report = BugReport.builder()
                .userId(userId)
                .userDescription(blankToNull(request.getUserDescription()))
                .severity(blankToNull(request.getSeverity()))
                .source(blankToNull(request.getSource()))
                .errorName(blankToNull(request.getErrorName()))
                .errorMessage(blankToNull(request.getErrorMessage()))
                .errorStack(truncate(blankToNull(request.getErrorStack()), 20000))
                .url(blankToNull(request.getUrl()))
                .route(blankToNull(request.getRoute()))
                .appVersion(blankToNull(request.getAppVersion()))
                .clientTimestamp(blankToNull(request.getClientTimestamp()))
                .userAgent(firstNonBlank(request.getUserAgent(), userAgent))
                .ip(blankToNull(ip))
                .consoleLogsJson(toJson(request.getConsoleLogs()))
                .networkLogsJson(toJson(request.getNetworkLogs()))
                .serviceLogsJson(toJson(request.getServiceLogs()))
                .deviceJson(toJson(request.getDevice()))
                .performanceJson(toJson(request.getPerformance()))
                .breadcrumbsJson(toJson(request.getBreadcrumbs()))
                .extraJson(toJson(request.getExtra()))
                .build();

        BugReport saved = bugReportRepository.save(report);
        log.info("Bug report saved: id={}, userId={}, source={}, url={}",
                saved.getId(), saved.getUserId(), saved.getSource(), saved.getUrl());
        return BugReportResponse.builder()
                .id(saved.getId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private Optional<UUID> resolveUserId(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username).map(User::getId);
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(node), 200000);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize bug report diagnostic payload", e);
            return null;
        }
    }

    private static String firstNonBlank(String first, String second) {
        String normalized = blankToNull(first);
        return normalized != null ? normalized : blankToNull(second);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
