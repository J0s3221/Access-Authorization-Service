package com.accessauth.repository;

import com.accessauth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findById(Long id);

    Optional<User> findByIdAndActiveTrue(Long id);

    boolean existsById(Long id);

    boolean existsByIdAndActiveTrue(Long id);

    @Query("SELECT u.symKey FROM User u WHERE u.id = :id AND u.active = true")
    Optional<String> getSymKeyById(@Param("id") Long id);
}
