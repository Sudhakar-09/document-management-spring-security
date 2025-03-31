package com.sudd.doc.Document.Event.Listener;


import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sudd.doc.Document.service.EmailService;
import com.sudd.doc.Document.utils.UserEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final EmailService emailService;

    @EventListener
    public void onUserEvent(UserEvent userEvent){
        switch (userEvent.getEventType()) {
            case REGISTRATION -> emailService.sendNewAccountEmail(userEvent.getUserEntity().getFirstName(),
            userEvent.getUserEntity().getEmail(),
            (String)userEvent.getData().get("key"));

            case RESETPASSWORD -> emailService.sendPasswordResetEmail(userEvent.getUserEntity().getFirstName(),
            userEvent.getUserEntity().getEmail(),
            (String)userEvent.getData().get("key"));
            
            default -> {}
            
        }
    }
    
}
