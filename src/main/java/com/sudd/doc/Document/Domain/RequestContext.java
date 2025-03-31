package com.sudd.doc.Document.Domain;

public class RequestContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private RequestContext(){}

    public static void start(){
        USER_ID.remove(); // null 
    }

    public static void setUserId(long userId){
        USER_ID.set(userId);
    }

    public static Long  getUserId(){
        return USER_ID.get();

    }

  
}
