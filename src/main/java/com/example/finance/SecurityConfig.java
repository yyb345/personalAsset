package com.example.finance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 简化开发，生产环境建议启用
            .authorizeHttpRequests(auth -> auth
                // 公开访问的资源
                .requestMatchers("/api/auth/**", "/login.html", "/register.html", 
                               "/style.css", "/actuator/**", 
                               "/news.html", "/api/news/**",
                               "/", "/index.html", "/script.js").permitAll()
                // 需要认证的API - 资产录入和报表查看
                .requestMatchers("/api/assets/**", "/api/reports/**", 
                               "/api/report", "/api/months", "/api/stocks/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    // 对于API请求返回401，对于页面请求重定向到登录页
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"请先登录\",\"message\":\"需要登录才能访问此功能\"}");
                    } else {
                        response.sendRedirect("/login.html");
                    }
                })
            );

        return http.build();
    }
}

