package com.example.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // OAuth2 相关查询
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByEmailAndProvider(String email, String provider);
}

