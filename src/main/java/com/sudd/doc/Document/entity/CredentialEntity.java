package com.sudd.doc.Document.entity;


import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false) // when used @data JVM will be in ambugity to create equals() and hascode() for super class
@Entity
@Table(name = "credentials")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CredentialEntity extends Auditable{
// coloumn 1 
private String password;
// colloumn 2  - if user is deleted this record from the credentail also gets deleted
@OneToOne(targetEntity = UserEntity.class , fetch = FetchType.EAGER)
@JoinColumn(name="user_id", nullable=false)
@OnDelete(action=OnDeleteAction.CASCADE)
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class,property = "id")
@JsonIdentityReference(alwaysAsId = true)
@JsonProperty("user_id")
private UserEntity userEntity;

public CredentialEntity(UserEntity userEntity,String password) {
    this.userEntity= userEntity;
    this.password= password;
    
}    
}
