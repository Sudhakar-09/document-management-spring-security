package com.sudd.doc.Document.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudd.doc.Document.Domain.Response;
import com.sudd.doc.Document.DtoRequest.UserRequest;
import com.sudd.doc.Document.service.UserService;
import com.sudd.doc.Document.utils.RequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import static java.util.Collections.emptyMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
@RequestMapping(path = { "/user" })
public class UserResource {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Response> saveUser(@RequestBody @Valid UserRequest user, HttpServletRequest request) {
        userService.CreateUser(user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword());
        return ResponseEntity.created(
                getUri())
                .body(RequestUtils.getResponse(request, emptyMap(),
                        "Account created check your Email to enable your account ", HttpStatus.CREATED));
    }


    // User Account Verification - Pending 
    @GetMapping("/verify/account")
    public ResponseEntity<Response> VerifyAccountToken(@RequestParam("key") String key , HttpServletRequest request) {
        userService.VerifyAccountToken(key);
        return ResponseEntity.ok().body(RequestUtils.getResponse(request, emptyMap(),
                        "Account Verified Successfully!", HttpStatus.OK));
    }

    // continue from here
    private URI getUri() {
        return URI.create("");
    }

}
