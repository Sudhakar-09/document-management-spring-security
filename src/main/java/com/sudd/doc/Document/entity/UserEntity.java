package com.sudd.doc.Document.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sudd.doc.Document.Enum.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
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
@Table(name = "users")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UserEntity extends Auditable{
@Column(name = "user_id" , nullable = false , updatable = false,unique = true)    
private String userId;                   

private String firstName;     

private String lastName; 

@Column(name = "email" , nullable = true , unique = true )               
private String email;                  

private Integer LoginAttempts;    

private LocalDateTime lastLoginAt;   

private String phoneNumber;    

private String bio;        

private String profileImageUrl;    

private boolean isAccountNonExpired;   

private boolean isAccountNonLocked; 

private boolean isEnabled;      

private boolean isMfaEnabled;  

@JsonIgnore         
@Column(columnDefinition = "text")

private String qrCodeSecretKey;  

private String qrCodeImageUrl;  

// id(primary key) is coming from the AUDITTABLE CLASS which is inherited by other entity class 
@ManyToOne(fetch = FetchType.EAGER)
@JoinTable(name ="user_role" , 
joinColumns = @JoinColumn(name="user_id", referencedColumnName = "id"),
// forigen key (fk) for role user_role table which is inherited by auditable 
inverseJoinColumns = @JoinColumn(name="role_id" ,referencedColumnName="id"))
private RolesEntity Role ;

// @Enumerated(EnumType.STRING)  
// @Column(name = "role", nullable = false)  

// private Role userRole;                  




}
