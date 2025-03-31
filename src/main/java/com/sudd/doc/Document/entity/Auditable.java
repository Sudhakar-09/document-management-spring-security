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
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


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
    private long id;
    private String referenceId = new AlternativeJdkIdGenerator().generateId().toString();
    @NotNull
    private long  createdBy;
    @NotNull
    private long  updatedBy;
    @NotNull
    @CreatedDate
    @Column(name = "CREATED_AT" , nullable = false , updatable = false)
    private LocalDateTime  createdAt;
    @NotNull
    @CreatedDate
    @Column(name = "UPDATED_AT" , nullable = false)
    private LocalDateTime  updatedAt;

    @PrePersist
    public void beforePersist(){
        var userId =RequestContext.getUserId(); // Long
        if(userId == null) {throw new ApiException("cannot persist entity without user id in RequestContext for this Thread ");};
        setCreatedAt(LocalDateTime.now());
        setCreatedBy(userId);
        setUpdatedBy(userId);
        setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    public void beforeUpdate(){
        var userId =RequestContext.getUserId(); // Long
        if(userId == null) {throw new ApiException ("cannot Update entity without user id in RequestContext for this Thread ");};
        setUpdatedBy(userId);
        setUpdatedAt(LocalDateTime.now());
        
    }

    
}
