package com.chen.chat_backend.repository;


import com.chen.chat_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

// Database lookup used to register users and validate login email addresses.
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}
