package com.sudd.doc.Document.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudd.doc.Document.entity.UserEntity; 

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    Optional<UserEntity> findUserByUserId(String userId);
    
}
