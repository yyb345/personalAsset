package com.example.finance;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端路由控制器
 * 处理所有非 API 请求，返回 index.html，让 Vue Router 处理前端路由
 */
@RestController
public class FrontendController {

    /**
     * 处理所有非 API 的前端路由请求
     * 返回 index.html，让 Vue Router 处理路由
     * 
     * 注意：Spring Boot 的路径匹配不支持 /** 通配符在 @GetMapping 中
     * 所以我们需要列出所有可能的前端路由
     */
    @GetMapping(value = {
        "/",
        "/login",
        "/register",
        "/dashboard",
        "/shadowing"
    })
    public ResponseEntity<Resource> index() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            if (resource.exists()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
            } else {
                // 如果找不到 index.html，返回 404
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 处理 dashboard 的子路由
     * 例如：/dashboard/youtube, /dashboard/xiaohongshu 等
     */
    @GetMapping("/dashboard/**")
    public ResponseEntity<Resource> dashboard() {
        return index();
    }
}

