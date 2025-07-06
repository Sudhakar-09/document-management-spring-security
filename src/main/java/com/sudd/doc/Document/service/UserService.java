package com.sudd.doc.Document.service;

import com.sudd.doc.Document.entity.RolesEntity;

public interface UserService {

    void CreateUser(String firstName,String lastName, String email, String password);
    RolesEntity getRoleName(String name);
    void VerifyAccountToken(String key);
    
}
