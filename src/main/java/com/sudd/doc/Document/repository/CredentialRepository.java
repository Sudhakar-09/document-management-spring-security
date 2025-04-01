package com.sudd.doc.Document.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudd.doc.Document.entity.CredentialEntity;

@Repository
public interface CredentialRepository extends JpaRepository<CredentialEntity,Long>{
    Optional<CredentialEntity> getCredentailByUserEntityId(Long userId);
    
}
