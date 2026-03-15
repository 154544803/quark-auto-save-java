package com.quark.autosave.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebConsolePageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String dashboardPage(HttpSession session) {
        return session.getAttribute("webConsoleUser") == null ? "redirect:/login" : "dashboard";
    }
}
