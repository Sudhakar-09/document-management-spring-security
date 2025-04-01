package com.sudd.doc.Document.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudd.doc.Document.Domain.Response;
import com.sudd.doc.Document.DtoRequest.UserRequest;
import com.sudd.doc.Document.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.http.HttpClient;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping(path = {"/user"})
public class UserResource {
    private final UserService userService;
    @PostMapping("/register")
    public ResponseEntity<Response> saveUser(@RequestBody @Valid UserRequest user , HttpServletRequest request){
        userService.CreateUser(user.getFirstName(),user.getLastName(),user.getEmail(),user.getPassword());
        return ResponseEntity.created(
            getUri()).body(getResponse(request, emptyMap(), "Account created check your Email to enable your account " , CREATED));
    }
    // continue from here 
    private URI getUri() {
     return URI.create("");
    }
    
    
}
