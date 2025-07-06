package com.sudd.doc.Document.utils;

import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import com.sudd.doc.Document.entity.RolesEntity;
import com.sudd.doc.Document.entity.UserEntity;

public class UserUtils {

    public static UserEntity createUserEntity(String firstName, String lastName, String email, RolesEntity role) {
        // default user saving
        return UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .lastLoginAt(LocalDateTime.now())
                .role(role)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isEnabled(false)
                .isMfaEnabled(false)
                .LoginAttempts(0)
                .qrCodeSecretKey(StringUtils.EMPTY)
                .phoneNumber(StringUtils.EMPTY)
                .bio(StringUtils.EMPTY)
                .profileImageUrl("https://cdn-icons-png.flaticon.com/512/149/149071.png")
                .build();

    }

}
