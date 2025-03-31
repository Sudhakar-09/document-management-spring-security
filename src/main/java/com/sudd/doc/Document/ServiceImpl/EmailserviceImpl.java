package com.sudd.doc.Document.ServiceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sudd.doc.Document.Exception.ApiException;
import com.sudd.doc.Document.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sudd.doc.Document.utils.EmailUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailserviceImpl implements EmailService{
    private static final String NEW_USER_ACCOUNT_VERIFICATION= "New User Account Verification";
    private static final String RESET_PASSWORD_REQUEST= "Reset Password Request";
    private final JavaMailSender sender;
    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendNewAccountEmail(String name, String email, String token) {
       try {
        SimpleMailMessage message= new SimpleMailMessage();
        message.setSubject(NEW_USER_ACCOUNT_VERIFICATION);
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setText(EmailUtils.getEmailMessage(name , host,token ));
        sender.send(message);
       } catch (Exception exception) {
        log.error(exception.getMessage());
        throw new ApiException("unable to send an email , try again!");
       }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String name, String email, String token) {
        try {
            SimpleMailMessage message= new SimpleMailMessage();
            message.setSubject(RESET_PASSWORD_REQUEST);
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setText(EmailUtils.getResetPasswordMessage(name , host,token ));
            sender.send(message);
           } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("unable to send an email , try again!");
           }
    
}
}
