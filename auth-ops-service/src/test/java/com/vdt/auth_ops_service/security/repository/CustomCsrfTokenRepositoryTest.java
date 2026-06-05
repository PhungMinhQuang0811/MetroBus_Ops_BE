package com.vdt.auth_ops_service.security.repository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomCsrfTokenRepositoryTest {

    private CustomCsrfTokenRepository repository;
    private final String cookieName = "XSRF-TOKEN";
    private final String headerName = "X-XSRF-TOKEN";
    private final long maxAge = 86400;
    private final String domain = "localhost";
    private final String contextPath = "/";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        repository = new CustomCsrfTokenRepository(
                cookieName, headerName, maxAge, domain, contextPath);
    }

    @Test
    void generateToken_WithExistingCookie_ShouldReturnExistingValue() {
        // Arrange
        Cookie cookie = new Cookie(cookieName, "existing-uuid");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // Act
        CsrfToken token = repository.generateToken(request);

        // Assert
        assertNotNull(token);
        assertEquals("existing-uuid", token.getToken());
        assertEquals(headerName, token.getHeaderName());
    }

    @Test
    void generateToken_WithEmptyCookie_ShouldReturnNewUuid() {
        // Arrange
        Cookie cookie = new Cookie(cookieName, "");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // Act
        CsrfToken token = repository.generateToken(request);

        // Assert
        assertNotNull(token);
        assertNotEquals("", token.getToken());
        assertTrue(token.getToken().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    void generateToken_WithoutCookie_ShouldReturnNewUuid() {
        // Arrange
        when(request.getCookies()).thenReturn(null);

        // Act
        CsrfToken token = repository.generateToken(request);

        // Assert
        assertNotNull(token);
        assertTrue(token.getToken().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    void saveToken_WhenNewToken_ShouldSetCookie() {
        // Arrange
        CsrfToken token = new DefaultCsrfToken(headerName, "_csrf", "new-uuid");
        when(request.getCookies()).thenReturn(null);

        // Act
        repository.saveToken(token, request, response);

        // Assert
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("new-uuid"));
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("SameSite=None"));
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("Secure"));
    }

    @Test
    void saveToken_WhenDifferentTokenExists_ShouldSetCookie() {
        // Arrange
        CsrfToken token = new DefaultCsrfToken(headerName, "_csrf", "new-uuid");
        Cookie cookie = new Cookie(cookieName, "old-uuid");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // Act
        repository.saveToken(token, request, response);

        // Assert
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("new-uuid"));
    }

    @Test
    void saveToken_WhenSameTokenExists_ShouldNotSetCookie() {
        // Arrange
        CsrfToken token = new DefaultCsrfToken(headerName, "_csrf", "same-uuid");
        Cookie cookie = new Cookie(cookieName, "same-uuid");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // Act
        repository.saveToken(token, request, response);

        // Assert
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    void saveToken_WhenTokenNull_ShouldDoNothing() {
        // Act
        repository.saveToken(null, request, response);

        // Assert
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    void loadToken_WithExistingCookie_ShouldReturnToken() {
        // Arrange
        Cookie cookie = new Cookie(cookieName, "test-token");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // Act
        CsrfToken token = repository.loadToken(request);

        // Assert
        assertNotNull(token);
        assertEquals("test-token", token.getToken());
    }

    @Test
    void loadToken_WithEmptyCookie_ShouldReturnNull() {
        // Arrange
        Cookie cookie = new Cookie(cookieName, "");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        // Act
        CsrfToken token = repository.loadToken(request);

        // Assert
        assertNull(token);
    }

    @Test
    void loadToken_WithoutCookie_ShouldReturnNull() {
        // Arrange
        when(request.getCookies()).thenReturn(null);

        // Act
        CsrfToken token = repository.loadToken(request);

        // Assert
        assertNull(token);
    }
}
