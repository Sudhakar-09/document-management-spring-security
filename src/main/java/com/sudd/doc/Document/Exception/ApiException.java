package com.sudd.doc.Document.Exception;

public class ApiException extends RuntimeException{

    public ApiException(String message){ super(message);}

    public ApiException(){ super("An Unexpected Error");}
    
}
