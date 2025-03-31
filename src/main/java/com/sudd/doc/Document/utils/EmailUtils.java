package com.sudd.doc.Document.utils;

public class EmailUtils {

    public static String getEmailMessage(String name, String host, String token) {

        return "Dear " + name + ",\n\n" +
                "Welcome to SecureDoc! 🎉 Your account has been successfully created.\n\n" +
                "To ensure the security of your account, please verify your email address by clicking the link below:\n\n"
                +
                "🔗 " + getverificationUrl(host, token) + "\n\n" +
                "If you did not sign up for SecureDoc, please ignore this email. This link will expire in **24 hours** for security reasons.\n\n"
                +
                "Best regards,\n" +
                "🔒 The SecureDoc Team\n\n" +
                "📩 Need help? Contact us at support@securedoc.com";
    }


    public static String getResetPasswordMessage(String name, String host, String token) {
        return "Dear " + name + ",\n\n" +
               "We received a request to reset your password for your SecureDoc account. If this was you, please click the link below to reset your password:\n\n" +
               "🔗 " + getResetPasswordUrl(host, token) + "\n\n" +
               "For security reasons, this link will expire in **24 hours**.\n\n" +
               "If you did not request a password reset, please ignore this email. Your account remains secure.\n\n" +
               "Best regards,\n" +
               "🔒 The SecureDoc Team\n\n" +
               "📩 Need help? Contact us at support@securedoc.com";
    }
    
    private static String getResetPasswordUrl(String host, String token) {
        return host + "/password?token=" + token;
    }
    

    private static String getverificationUrl(String host, String token) {
        return host + "/verify/account?token=" + token;

    }
}
