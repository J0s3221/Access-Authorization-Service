package com.accessAuthorization.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessKeyRepository extends JpaRepository<AccessKey, String> {
}
