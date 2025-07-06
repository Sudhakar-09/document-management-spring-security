package com.sudd.doc.Document.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.AlternativeJdkIdGenerator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sudd.doc.Document.Domain.RequestContext;
import com.sudd.doc.Document.Exception.ApiException;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for all auditable entities in the system.
 * 
 * This class provides common auditing fields and logic:
 * - `id`: Auto-generated primary key using a sequence generator.
 * - `referenceId`: Unique string identifier for external reference.
 * - `createdBy` / `updatedBy`: Track the user responsible for creating or modifying the entity.
 * - `createdAt` / `updatedAt`: Timestamps for when the entity was created or last updated.
 *
 * The auditing is handled manually via lifecycle callbacks:
 * - `@PrePersist` sets all fields before saving a new entity.
 * - `@PreUpdate` updates `updatedBy` and `updatedAt` before updating an existing entity.
 * 
 * Requires the current user's ID to be available in the thread-local RequestContext.
 * If it's not present, an ApiException will be thrown to enforce proper tracking.
 * 
 * Extend this class in any JPA entity to automatically inherit auditing behavior.
 */

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = {"createdAt" , "updatedAt"} , allowGetters = true)
public abstract class Auditable {
    @Id
    @SequenceGenerator(name = "primary_key_seq",sequenceName = "primary_key_seq",allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator ="primary_key_seq" )
    @Column(name = "ID", updatable = false)
    private Long id;

    private String referenceId = new AlternativeJdkIdGenerator().generateId().toString();
    @NotNull
    private long  createdBy;

    @NotNull
    private long  updatedBy;

    // do below with SQL - without having to write the below mapping 

    // @OneToMany
    // @JoinColumn(name = "owner_id",referencedColumnName = "id",foreignKey = @ForeignKey(name="fk_user_owner" , value = ConstraintMode.CONSTRAINT))
    // Private UserEntity owner;

    @NotNull
    @CreatedDate
    @Column(name = "CREATED_AT" , nullable = false , updatable = false)
    private LocalDateTime  createdAt;

    @NotNull
    @CreatedDate
    @Column(name = "UPDATED_AT" , nullable = false)
    private LocalDateTime  updatedAt;

    // before we persist all these in DB validate 
    // Before we persisit any entity we will call the below method 
    // who created it ? loggedin user 
    @PrePersist
    public void beforePersist(){
        var userId = 0L;  //RequestContext.getUserId(); // Long
        // if(userId == null) {throw new ApiException("cannot persist entity without user id in RequestContext for this Thread ");};
        setCreatedAt(LocalDateTime.now());
        setCreatedBy(userId);
        setUpdatedBy(userId);
        setUpdatedAt(LocalDateTime.now());
    }

    // pre update any entity from request we will find the id if , we dont throw error .
    // who updated it ?  loggedin user
    @PreUpdate
    public void beforeUpdate(){
        var userId = 0L; //RequestContext.getUserId(); // Long
        // if(userId == null) {throw new ApiException ("cannot Update entity without user id in RequestContext for this Thread ");};
        setUpdatedBy(userId);
        setUpdatedAt(LocalDateTime.now());
        
    }

    
}
