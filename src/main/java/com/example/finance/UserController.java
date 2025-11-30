package com.example.finance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Map<String, String> response = new HashMap<>();

        // 验证输入
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            response.put("message", "用户名不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            response.put("message", "密码长度至少为6位");
            return ResponseEntity.badRequest().body(response);
        }

        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            response.put("message", "请输入有效的邮箱地址");
            return ResponseEntity.badRequest().body(response);
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            response.put("message", "用户名已存在");
            return ResponseEntity.badRequest().body(response);
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            response.put("message", "邮箱已被注册");
            return ResponseEntity.badRequest().body(response);
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setEnabled(true);

        userRepository.save(user);

        response.put("message", "注册成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // 查找用户
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());

        if (userOptional.isEmpty()) {
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userOptional.get();

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 检查用户是否启用
        if (!user.isEnabled()) {
            response.put("message", "账户已被禁用");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 设置会话
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());

        // 设置 Spring Security 认证信息
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(), 
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 将 Security Context 保存到 Session 中
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );

        response.put("message", "登录成功");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        // 清除 Spring Security 认证信息
        SecurityContextHolder.clearContext();
        
        // 使会话失效
        session.invalidate();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 优先检查 Spring Security 认证状态
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            
            String username = authentication.getName();
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                response.put("authenticated", true);
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("fullName", user.getFullName());
                return ResponseEntity.ok(response);
            }
        }
        
        // 降级检查 Session
        Object userId = session.getAttribute("userId");
        if (userId != null) {
            Optional<User> userOptional = userRepository.findById((Long) userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                response.put("authenticated", true);
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("fullName", user.getFullName());
                return ResponseEntity.ok(response);
            }
        }
        
        response.put("authenticated", false);
        return ResponseEntity.ok(response);
    }
}

// Request DTOs
class LoginRequest {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}

