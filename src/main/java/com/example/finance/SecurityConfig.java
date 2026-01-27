package com.example.finance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow Chrome Extension, localhost, and production domain
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*", 
            "http://127.0.0.1:*",
            "http://www.xlearning.top",
            "http://xlearning.top",
            "https://www.xlearning.top",
            "https://xlearning.top",
            "chrome-extension://*"  // Allow Chrome extensions
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // 简化开发，生产环境建议启用
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                // 公开访问的资源 - 必须在最前面，确保优先级
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**", "/login.html", "/register.html", 
                               "/style.css", "/actuator/**",
                               "/", "/index.html", "/script.js").permitAll()
                // 前端路由（公开访问）
                .requestMatchers("/login", "/register", "/dashboard", "/dashboard/**", "/shadowing").permitAll()
                // Chrome 插件 API（公开访问）
                .requestMatchers("/api/youtube/**").permitAll()
                // 需要认证的API
                .requestMatchers("/api/stocks/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .permitAll()
                .loginProcessingUrl("/login.html") // 明确指定formLogin只处理/login.html的POST请求
            )
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestUri = request.getRequestURI();
                    // 对于需要认证的API请求返回401，对于页面请求重定向到登录页
                    if (requestUri.startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"error\":\"请先登录\",\"message\":\"需要登录才能访问此功能\"}");
                    } else {
                        response.sendRedirect("/login.html");
                    }
                })
            );

        return http.build();
    }
}

