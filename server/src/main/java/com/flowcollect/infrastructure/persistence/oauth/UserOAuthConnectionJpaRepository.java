package com.flowcollect.infrastructure.persistence.oauth;

import com.flowcollect.domain.oauth.OAuthProvider;
import com.flowcollect.domain.oauth.UserOAuthConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOAuthConnectionJpaRepository extends JpaRepository<UserOAuthConnection, UUID> {

    Optional<UserOAuthConnection> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
