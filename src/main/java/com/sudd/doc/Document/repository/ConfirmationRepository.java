package com.sudd.doc.Document.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudd.doc.Document.entity.ConfirmationEntity;
import com.sudd.doc.Document.entity.UserEntity;

@Repository
public interface ConfirmationRepository extends JpaRepository<ConfirmationEntity,Long> {
    // token 
    Optional<ConfirmationEntity> findByKey(String key);
    Optional<ConfirmationEntity> findByUserEntity(UserEntity userEntity);
    
}
