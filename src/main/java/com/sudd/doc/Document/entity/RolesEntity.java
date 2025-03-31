package com.sudd.doc.Document.entity;

import com.sudd.doc.Document.Enum.Authority;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false) 
@Entity
@Table(name="ROLES")
public class RolesEntity extends Auditable{
    private String name;
    private Authority Authorities;
    
}
