package com.vdt.afc_ops_service.security.repository;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.util.WebUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomCsrfTokenRepositoryTest {

    private final CustomCsrfTokenRepository repository = new CustomCsrfTokenRepository(
            "XSRF-TOKEN",
            "X-XSRF-TOKEN",
            3600,
            "example.com",
            "/"
    );

    @Test
    void generateToken_CreatesTokenWithConfiguredHeader() {
        CsrfToken token = repository.generateToken(new MockHttpServletRequest());

        assertEquals("X-XSRF-TOKEN", token.getHeaderName());
        assertNotNull(token.getToken());
    }

    @Test
    void saveToken_AddsCookieHeaderWhenTokenChanges() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(request);

        repository.saveToken(token, request, response);

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("XSRF-TOKEN=" + token.getToken()));
        assertTrue(cookieHeader.contains("Domain=example.com"));
    }

    @Test
    void saveToken_DoesNotAddCookieWhenSameValueExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Cookie cookie = new Cookie("XSRF-TOKEN", "same-token");
        request.setCookies(cookie);
        CsrfToken token = new org.springframework.security.web.csrf.DefaultCsrfToken(
                "X-XSRF-TOKEN",
                "_csrf",
                "same-token"
        );

        repository.saveToken(token, request, response);

        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    void saveToken_NullToken_DoesNothing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveToken(null, request, response);

        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    void loadToken_ReturnsTokenFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("XSRF-TOKEN", "cookie-token"));

        CsrfToken token = repository.loadToken(request);

        assertNotNull(token);
        assertEquals("cookie-token", token.getToken());
        assertEquals("X-XSRF-TOKEN", token.getHeaderName());
    }

    @Test
    void loadToken_WhenCookieMissing_ReturnsNull() {
        assertNull(repository.loadToken(new MockHttpServletRequest()));
    }

    @Test
    void loadToken_WhenCookieBlank_ReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("XSRF-TOKEN", ""));

        assertNull(repository.loadToken(request));
    }
}
