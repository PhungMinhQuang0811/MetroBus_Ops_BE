package com.vdt.authservice.security.repository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomCsrfTokenRepository implements CsrfTokenRepository {
    String cookieName;
    String headerName;
    long maxAge;
    String domain;
    String contextPath;
    String logoutPath;

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        // Thử lấy mã đang có từ Cookie trước
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie != null && StringUtils.hasLength(cookie.getValue())) {
            return new DefaultCsrfToken(headerName, "_csrf", cookie.getValue());
        }
        // Nếu chưa có tí gì thì mới gen cái mới để dùng lâu dài
        return new DefaultCsrfToken(headerName, "_csrf", UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        // 1. Xử lý trường hợp Spring muốn xóa Token (token == null)
        if (token == null) {
            return; //  return để chặn Spring gửi lệnh xóa mặc định
        }

        String tokenValue = token.getToken();
        Cookie existingCookie = WebUtils.getCookie(request, cookieName);

        // 2. Chỉ set lại Cookie khi thực sự chưa có hoặc mã bị lệch
        if (existingCookie == null || !tokenValue.equals(existingCookie.getValue())) {
            ResponseCookie cookie = ResponseCookie.from(cookieName, tokenValue)
                    .path(contextPath)
                    .domain(domain)
                    .maxAge(maxAge)
                    .httpOnly(false)
                    .secure(true)
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }
    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie == null || !StringUtils.hasLength(cookie.getValue())) {
            return null;
        }
        return new DefaultCsrfToken(headerName, "_csrf", cookie.getValue());
    }
}
