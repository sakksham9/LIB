package com.library.user;

import com.library.security.AuthInterceptor;
import com.library.user.dto.LoginRequest;
import com.library.user.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @GetMapping("/me")
    public User me(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (currentUser == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return currentUser;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else {
            token = request.getHeader("X-Auth-Token");
        }
        if (token != null) {
            userService.logout(token);
        }
        return Map.of("message", "Logged out successfully");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(Exception e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleUnauthorized(Exception e) {
        return Map.of("error", e.getMessage());
    }
}
