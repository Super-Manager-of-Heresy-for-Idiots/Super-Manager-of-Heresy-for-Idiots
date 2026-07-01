package com.dnd.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * CSRF handler tuned for a JavaScript single-page app using the double-submit
 * cookie pattern: axios reads the XSRF-TOKEN cookie and echoes it back verbatim
 * in the X-XSRF-TOKEN header.
 *
 * Spring Security 6 defaults to {@code XorCsrfTokenRequestAttributeHandler}, which
 * BREACH-masks the token on every render so the value handed to the client differs
 * from the raw token in the cookie. A SPA that returns the raw cookie value would
 * then fail validation. This handler keeps BREACH masking for rendered tokens but,
 * when the token arrives in the request header, resolves it as the raw value — which
 * is exactly what the SPA sends.
 */
final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // Always BREACH-protect the token when it is rendered into a response.
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // Header present → SPA double-submit: the value is the raw token, resolve it plainly.
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        // Otherwise (e.g. an HTML form _csrf parameter) fall back to the XOR handler.
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
