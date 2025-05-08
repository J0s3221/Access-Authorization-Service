package com.accessAuthorization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccessKeyRepository extends JpaRepository<AccessKey, String> {
    // Find by public key
    Optional<AccessKey> findByPublicKey(String publicKey);
    
    // Check if public key exists
    boolean existsByPublicKey(String publicKey);
}
