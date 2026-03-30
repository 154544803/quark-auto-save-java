package com.quark.autosave.controller;

import com.quark.autosave.service.WebConsoleAuthService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class WebConsoleAuthController {

    private final WebConsoleAuthService webConsoleAuthService;

    public WebConsoleAuthController(WebConsoleAuthService webConsoleAuthService) {
        this.webConsoleAuthService = webConsoleAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
        if (!webConsoleAuthService.isAuthenticated(request.username(), request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("authenticated", false, "message", "用户名或密码错误"));
        }
        session.setAttribute("webConsoleUser", request.username());
        return ResponseEntity.ok(Map.of("authenticated", true));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("authenticated", false);
    }

    private record LoginRequest(String username, String password) {
    }
}
