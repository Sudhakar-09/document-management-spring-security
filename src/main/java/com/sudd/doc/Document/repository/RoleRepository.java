package com.sudd.doc.Document.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sudd.doc.Document.entity.RolesEntity;

@Repository
public interface RoleRepository extends JpaRepository<RolesEntity,Long>{
    Optional<RolesEntity> findByNameIgnoreCase(String name);
    
}
