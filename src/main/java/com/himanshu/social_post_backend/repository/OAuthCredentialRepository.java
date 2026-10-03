package com.himanshu.social_post_backend.repository;

import com.himanshu.social_post_backend.model.OAuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthCredentialRepository extends JpaRepository<OAuthCredential, Long> {

    Optional<OAuthCredential> findByServiceName(String serviceName);

    boolean existsByServiceName(String serviceName);
}
