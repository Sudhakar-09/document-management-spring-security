package com.sudd.doc.Document.Domain;

/**
 * RequestContext is a helper class that uses ThreadLocal to store the current
 * user's ID
 * for each request handled by the application.
 *
 * Why is this useful?
 * - In a web application, each request is handled by a different thread.
 * - Instead of passing the user ID through every method, we store it in a
 * ThreadLocal.
 * - This way, we can access the user ID anywhere during the request lifecycle.
 *
 * How it works:
 * - setUserId(): Called when a request starts to store the user's ID.
 * - getUserId(): Called anywhere in the app to get the current user's ID.
 * - start(): Clears the ThreadLocal data to prevent memory leaks.
 *
 * Always clear the ThreadLocal (via start or at request completion) when the
 * request ends.
 */

 // static values so we can call them anywhere 
public class RequestContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    // CONSTRUCTOR 
    private RequestContext() {
    }
// on start clean it - Cleans up old data from previous requests
    public static void start() {
        USER_ID.remove(); // Clear it — important to prevent memory leaks
    }
//    set user id 
    public static void setUserId(long userId) {
        USER_ID.set(userId);
    }
//   get user id 
    public static Long getUserId() {
        return USER_ID.get();

    }

}
