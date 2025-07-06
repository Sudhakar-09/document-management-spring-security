package com.sudd.doc.Document.utils;

import java.time.LocalDateTime;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import com.sudd.doc.Document.Domain.Response;
import jakarta.servlet.http.HttpServletRequest;

public class RequestUtils {

    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return new Response(LocalDateTime.now().toString(), status.value(), request.getRequestURI(), HttpStatus.valueOf(
                status.value()), message, StringUtils.EMPTY, data);

                
    }
}
