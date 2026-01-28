package com.example.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 自定义 OAuth2 用户服务
 * 处理第三方登录（Google, GitHub 等）用户的创建和更新
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // google, github 等
        Map<String, Object> attributes = oauth2User.getAttributes();

        log.info("OAuth2 登录: provider={}, attributes={}", provider, attributes);

        // 根据不同的 provider 提取用户信息
        String providerId;
        String email;
        String name;
        String avatarUrl;

        if ("google".equals(provider)) {
            providerId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            avatarUrl = (String) attributes.get("picture");
        } else if ("github".equals(provider)) {
            providerId = String.valueOf(attributes.get("id"));
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            if (name == null) {
                name = (String) attributes.get("login");
            }
            avatarUrl = (String) attributes.get("avatar_url");
        } else {
            // 通用处理
            providerId = String.valueOf(attributes.get("sub"));
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            avatarUrl = (String) attributes.get("picture");
        }

        // 查找或创建用户
        User user = findOrCreateUser(provider, providerId, email, name, avatarUrl);

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("OAuth2 用户登录成功: userId={}, username={}, provider={}",
            user.getId(), user.getUsername(), provider);

        return oauth2User;
    }

    /**
     * 根据 OAuth2 信息查找或创建用户
     */
    private User findOrCreateUser(String provider, String providerId, String email, String name, String avatarUrl) {
        // 1. 先通过 provider + providerId 查找
        Optional<User> userByProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        if (userByProvider.isPresent()) {
            User user = userByProvider.get();
            // 更新可能变化的信息
            if (name != null) user.setFullName(name);
            if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
            return user;
        }

        // 2. 通过邮箱查找已有用户（可能是之前用密码注册的）
        if (email != null) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                // 如果是 local 用户，绑定第三方账号
                if ("local".equals(user.getProvider())) {
                    log.info("将本地用户绑定到 OAuth2: userId={}, provider={}", user.getId(), provider);
                    user.setProvider(provider);
                    user.setProviderId(providerId);
                    if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
                    return userRepository.save(user);
                }
                return user;
            }
        }

        // 3. 创建新用户
        log.info("创建新 OAuth2 用户: provider={}, email={}", provider, email);

        User newUser = new User();
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setEmail(email != null ? email : providerId + "@" + provider + ".oauth");
        newUser.setFullName(name);
        newUser.setAvatarUrl(avatarUrl);

        // 生成唯一用户名
        String baseUsername = generateUsername(email, name, provider);
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }
        newUser.setUsername(username);

        // OAuth2 用户不需要密码，设置为随机值
        newUser.setPassword(UUID.randomUUID().toString());
        newUser.setEnabled(true);

        return userRepository.save(newUser);
    }

    /**
     * 生成用户名
     */
    private String generateUsername(String email, String name, String provider) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        if (name != null && !name.isEmpty()) {
            return name.toLowerCase().replaceAll("\\s+", "");
        }
        return provider + "_user";
    }
}
