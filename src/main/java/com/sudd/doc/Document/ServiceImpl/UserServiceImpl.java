package com.sudd.doc.Document.ServiceImpl;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.sudd.doc.Document.Enum.Authority;
import com.sudd.doc.Document.Event.EventType;
import com.sudd.doc.Document.Exception.ApiException;
import com.sudd.doc.Document.entity.ConfirmationEntity;
import com.sudd.doc.Document.entity.CredentialEntity;
import com.sudd.doc.Document.entity.RolesEntity;
import com.sudd.doc.Document.entity.UserEntity;
import com.sudd.doc.Document.repository.ConfirmationRepository;
import com.sudd.doc.Document.repository.CredentialRepository;
import com.sudd.doc.Document.repository.RoleRepository;
import com.sudd.doc.Document.repository.UserRepository;
import com.sudd.doc.Document.service.UserService;
import com.sudd.doc.Document.utils.UserEvent;
import com.sudd.doc.Document.utils.UserUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ConfirmationRepository confirmationRepository;
    private final CredentialRepository credentialRepository;
    private final RoleRepository roleRepository;
    // private BycryptPasswordEncoder encoder;
    // used in scenarios where one component needs to notify others about something
    // happening, without having a direct dependency on them.
    private final ApplicationEventPublisher publisher;

    @Override
    public void CreateUser(String firstName, String lastName, String email, String password) {

        // save the user // userRepository--> managing userEntity
        var userEntity = userRepository.save(CreateNewUser(firstName, lastName, email));
        var credentialEntity = new CredentialEntity(userEntity, password);
        credentialRepository.save(credentialEntity);
        var confirmationEntity = new ConfirmationEntity(userEntity);
        confirmationRepository.save(confirmationEntity);
        publisher.publishEvent(
                new UserEvent(userEntity, EventType.REGISTRATION, Map.of("key", confirmationEntity.getKey())));

    }

    private UserEntity CreateNewUser(String firstName, String lastName, String email) {
        // create a new role // seed the database (0)
        var role = getRoleName(Authority.USER.name());
        return UserUtils.createUserEntity(firstName, lastName, email, role);
    }

    @Override
    public RolesEntity getRoleName(String name) {

        var role = roleRepository.findByNameIgnoreCase(name);

        return role.orElseThrow(() -> new ApiException("Role not found"));
    }
}
