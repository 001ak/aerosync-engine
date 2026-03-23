package com.aerosync.repository;

import com.aerosync.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Used during authentication/login
    Optional<User> findByEmail(String email);

    // Used for SMS verification/lookups
    Optional<User> findByPhoneNumber(String phoneNumber);
}