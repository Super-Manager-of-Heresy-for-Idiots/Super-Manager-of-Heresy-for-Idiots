package com.dnd.app.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Класс ClientIps извлекает реальный IP клиента для ключа rate-limit'а.
 *
 * <p>Вынесено без изменения поведения из {@code AuthRateLimitFilter.clientIp} (N13), чтобы логику
 * можно было переиспользовать любым фильтром лимитирования. Левый край {@code X-Forwarded-For}
 * контролируется атакующим (клиент может дописать произвольные хопы), поэтому крайнее левое значение
 * доверять нельзя. При {@code trustedProxyCount} обратных прокси перед приложением — каждый дописывает
 * адрес нижестоящего пира (например, nginx {@code $proxy_add_x_forwarded_for}) — настоящий IP клиента
 * находится в хопе, дописанном нашим внешним прокси, по индексу {@code length - trustedProxyCount}.
 * Заголовок короче ожидаемого (подделан/срезан либо прокси нет) откатывается на транспортный адрес.
 */
public final class ClientIps {

    private ClientIps() {
    }

    /**
     * Разрешает IP клиента по правилу доверенных прокси.
     * @param request входящий HTTP-запрос
     * @param trustedProxyCount число доверенных обратных прокси перед приложением (0 — прокси нет)
     * @return IP-адрес клиента, пригодный как ключ лимита
     */
    public static String resolve(HttpServletRequest request, int trustedProxyCount) {
        if (trustedProxyCount > 0) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                int idx = hops.length - trustedProxyCount;
                if (idx >= 0 && idx < hops.length) {
                    String candidate = hops[idx].trim();
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }
}
