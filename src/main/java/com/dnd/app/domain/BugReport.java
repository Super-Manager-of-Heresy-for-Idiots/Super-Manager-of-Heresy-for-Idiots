package com.dnd.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bug_reports", indexes = {
        @Index(name = "idx_bug_reports_created_at", columnList = "created_at"),
        @Index(name = "idx_bug_reports_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BugReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_description", columnDefinition = "text")
    private String userDescription;

    @Column(name = "severity", columnDefinition = "text")
    private String severity;

    @Column(name = "source", columnDefinition = "text")
    private String source;

    @Column(name = "error_name", columnDefinition = "text")
    private String errorName;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "text")
    private String errorStack;

    @Column(name = "url", columnDefinition = "text")
    private String url;

    @Column(name = "route", columnDefinition = "text")
    private String route;

    @Column(name = "app_version", columnDefinition = "text")
    private String appVersion;

    @Column(name = "client_timestamp", columnDefinition = "text")
    private String clientTimestamp;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "ip", columnDefinition = "text")
    private String ip;

    @Column(name = "console_logs_json", columnDefinition = "text")
    private String consoleLogsJson;

    @Column(name = "network_logs_json", columnDefinition = "text")
    private String networkLogsJson;

    @Column(name = "service_logs_json", columnDefinition = "text")
    private String serviceLogsJson;

    @Column(name = "device_json", columnDefinition = "text")
    private String deviceJson;

    @Column(name = "performance_json", columnDefinition = "text")
    private String performanceJson;

    @Column(name = "breadcrumbs_json", columnDefinition = "text")
    private String breadcrumbsJson;

    @Column(name = "extra_json", columnDefinition = "text")
    private String extraJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
