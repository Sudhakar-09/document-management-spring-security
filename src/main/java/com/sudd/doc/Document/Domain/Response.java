package com.sudd.doc.Document.Domain;

import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Response(String time, int code, String path, HttpStatus status, String message, String exception,
        Map<?, ?> data) {

}
