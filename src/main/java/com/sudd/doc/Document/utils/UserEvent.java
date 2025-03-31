package com.sudd.doc.Document.utils;

import java.util.Map;

import com.sudd.doc.Document.Event.EventType;
import com.sudd.doc.Document.entity.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserEvent {
    private UserEntity userEntity;
    private EventType eventType;
    // ? , ? data of anything 
    private Map<?,?> data;
    
}
