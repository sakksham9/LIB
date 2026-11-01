package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.user.User;
import com.library.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTR = "currentUser";
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // Extract token from Bearer header or X-Auth-Token
        String token = extractToken(request);
        User currentUser = null;
        if (token != null) {
            currentUser = userService.validateToken(token);
            if (currentUser != null) {
                request.setAttribute(CURRENT_USER_ATTR, currentUser);
            }
        }

        // 1. Admin endpoints protection
        if (path.startsWith("/api/admin/")) {
            if (currentUser == null) {
                sendError(response, HttpStatus.UNAUTHORIZED, "Authentication required. Please sign in as Administrator.");
                return false;
            }
            if (!currentUser.isAdmin()) {
                sendError(response, HttpStatus.FORBIDDEN, "Access Denied: Administrator role required.");
                return false;
            }
            return true;
        }

        // 2. Student endpoints protection
        if (path.startsWith("/api/student/")) {
            if (currentUser == null) {
                sendError(response, HttpStatus.UNAUTHORIZED, "Authentication required. Please sign in to access your student portal.");
                return false;
            }
            if (!currentUser.isActive()) {
                sendError(response, HttpStatus.FORBIDDEN, "Your student account is deactivated. Please contact library staff.");
                return false;
            }
            return true;
        }

        // 3. Book modification endpoints protection (Admin only)
        if (path.startsWith("/api/books")) {
            String method = request.getMethod();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                if (currentUser == null) {
                    sendError(response, HttpStatus.UNAUTHORIZED, "Authentication required. Only Administrators can modify books.");
                    return false;
                }
                if (!currentUser.isAdmin()) {
                    sendError(response, HttpStatus.FORBIDDEN, "Access Denied: Only Administrators can add, edit, or delete books.");
                    return false;
                }
            }
        }

        // 4. Global borrow records and notifications (Admin only)
        if (path.equals("/api/borrows") && "GET".equalsIgnoreCase(request.getMethod())) {
            if (currentUser == null || !currentUser.isAdmin()) {
                sendError(response, HttpStatus.FORBIDDEN, "Access Denied: Only Administrators can view all borrowing records.");
                return false;
            }
        }

        if (path.startsWith("/api/notifications/")) {
            if (currentUser == null || !currentUser.isAdmin()) {
                sendError(response, HttpStatus.FORBIDDEN, "Access Denied: Only Administrators can view alert notifications.");
                return false;
            }
        }

        if (path.matches("/api/borrows/\\d+/(report-lost|pay-fine)")) {
            if (currentUser == null || !currentUser.isAdmin()) {
                sendError(response, HttpStatus.FORBIDDEN, "Access Denied: Only Administrators can process lost book reports or fine waivers.");
                return false;
            }
        }

        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        String xAuth = request.getHeader("X-Auth-Token");
        if (xAuth != null && !xAuth.isBlank()) {
            return xAuth.trim();
        }
        return null;
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
