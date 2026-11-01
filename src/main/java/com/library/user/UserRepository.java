package com.library.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentIdNumber(String studentIdNumber);

    @Query("SELECT u FROM User u WHERE lower(u.username) = lower(:identifier) OR lower(coalesce(u.email, '')) = lower(:identifier) OR lower(coalesce(u.studentIdNumber, '')) = lower(:identifier)")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    List<User> findByRoleOrderByCreatedAtDesc(String role);

    List<User> findByRoleAndStatusOrderByCreatedAtDesc(String role, String status);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByStudentIdNumber(String studentIdNumber);

    long countByRole(String role);
}
