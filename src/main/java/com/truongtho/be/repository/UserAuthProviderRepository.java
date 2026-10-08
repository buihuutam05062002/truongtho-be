package com.truongtho.be.repository;

import com.truongtho.be.entity.UserAuthProvider;
import com.truongtho.be.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, String> {
    Optional<UserAuthProvider> findByProviderAndProviderId(AuthProvider provider, String providerId);

}
