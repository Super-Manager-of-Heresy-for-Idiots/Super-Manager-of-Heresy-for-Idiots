package com.dnd.app.integration.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HttpMapSessionCloser: fail-fast без ключа/URL вне local/test (BTL-06)")
class HttpMapSessionCloserTest {

    private Environment env(String... profiles) {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(profiles);
        return environment;
    }

    @Test
    @DisplayName("prod + пустой ключ → падение при старте")
    void failsWhenKeyMissingInProd() {
        HttpMapSessionCloser closer = new HttpMapSessionCloser("http://map-service:8080", "");
        assertThatThrownBy(() -> closer.setEnvironment(env("prod")))
                .isInstanceOf(BeanCreationException.class);
    }

    @Test
    @DisplayName("prod + пустой base-url → падение при старте")
    void failsWhenBaseUrlMissingInProd() {
        HttpMapSessionCloser closer = new HttpMapSessionCloser("", "secret");
        assertThatThrownBy(() -> closer.setEnvironment(env("prod")))
                .isInstanceOf(BeanCreationException.class);
    }

    @Test
    @DisplayName("local профиль → не падает даже без конфигурации")
    void allowsMissingConfigInLocal() {
        HttpMapSessionCloser closer = new HttpMapSessionCloser("", "");
        assertThatCode(() -> closer.setEnvironment(env("local"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("prod + всё сконфигурировано → не падает")
    void allowsFullyConfiguredInProd() {
        HttpMapSessionCloser closer = new HttpMapSessionCloser("http://map-service:8080", "secret");
        assertThatCode(() -> closer.setEnvironment(env("prod"))).doesNotThrowAnyException();
    }
}
