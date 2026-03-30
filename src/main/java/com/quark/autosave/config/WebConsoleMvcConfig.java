package com.quark.autosave.config;

import com.quark.autosave.service.WebConsoleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConsoleMvcConfig implements WebMvcConfigurer {

    private final WebConsoleAuthService webConsoleAuthService;

    public WebConsoleMvcConfig(WebConsoleAuthService webConsoleAuthService) {
        this.webConsoleAuthService = webConsoleAuthService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WebConsoleLoginInterceptor(webConsoleAuthService))
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/login");
    }

    private static final class WebConsoleLoginInterceptor implements HandlerInterceptor {

        private final WebConsoleAuthService webConsoleAuthService;

        private WebConsoleLoginInterceptor(WebConsoleAuthService webConsoleAuthService) {
            this.webConsoleAuthService = webConsoleAuthService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (!webConsoleAuthService.isEnabled()) {
                return true;
            }
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("webConsoleUser") != null) {
                return true;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"未登录\"}");
            return false;
        }
    }
}
