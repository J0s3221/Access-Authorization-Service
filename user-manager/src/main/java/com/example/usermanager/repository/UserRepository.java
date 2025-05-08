package com.example.usermanager.repository;

import com.example.usermanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Custom queries can be added here if necessary
}
