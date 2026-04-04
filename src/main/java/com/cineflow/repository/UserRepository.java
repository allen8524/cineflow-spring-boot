package com.cineflow.repository;

import com.cineflow.domain.User;
import com.cineflow.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByLoginIdIgnoreCase(String loginId);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByLoginIdIgnoreCase(String loginId);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(UserRole role);
}
