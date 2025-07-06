package com.sudd.doc.Document.utils;

public class EmailUtils {

    public static String getEmailMessage(String name, String host, String key) {

        return "Dear " + name + ",\n\n" +
                "Welcome to SecureDoc! 🎉 Your account has been successfully created.\n\n" +
                "To ensure the security of your account, please verify your email address by clicking the link below:\n\n"
                +
                "🔗 " + getverificationUrl(host, key) + "\n\n" +
                "If you did not sign up for SecureDoc, please ignore this email. This link will expire in **24 hours** for security reasons.\n\n"
                +
                "Best regards,\n" +
                "🔒 The SecureDoc Team\n\n" +
                "📩 Need help? Contact us at support@securedoc.com";
    }


    public static String getResetPasswordMessage(String name, String host, String key) {
        return "Dear " + name + ",\n\n" +
               "We received a request to reset your password for your SecureDoc account. If this was you, please click the link below to reset your password:\n\n" +
               "🔗 " + getResetPasswordUrl(host, key) + "\n\n" +
               "For security reasons, this link will expire in **24 hours**.\n\n" +
               "If you did not request a password reset, please ignore this email. Your account remains secure.\n\n" +
               "Best regards,\n" +
               "🔒 The SecureDoc Team\n\n" +
               "📩 Need help? Contact us at support@securedoc.com";
    }
    
    private static String getResetPasswordUrl(String host, String key) {
        return host + "/password?key=" + key;
    }
    

    private static String getverificationUrl(String host, String key) {
        return host + "/verify/account?key=" + key;

    }


     public static String getVerificationHtml(String name, String host, String key) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Verify Your SecureDoc Account</title>
            <style>
                body { font-family: 'Inter', sans-serif; background-color: #f9fbfc; margin: 0; padding: 0; }
                .email-container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px;
                    box-shadow: 0 5px 30px rgba(0, 0, 0, 0.08); padding: 40px; text-align: center; border-top: 4px solid #2d89ef; }
                .logo { width: 160px; margin-bottom: 20px; }
                .header { font-size: 24px; font-weight: 600; color: #1a1a2e; }
                .subtext { font-size: 18px; color: #555; margin: 8px 0 20px; }
                .content { font-size: 16px; color: #444; line-height: 1.5; margin: 20px 0; }
                .cta-button { display: inline-block; background: linear-gradient(135deg, #2d89ef, #1e63c4); color: #ffffff;
                    text-decoration: none; padding: 16px 32px; font-size: 17px; font-weight: 600; border-radius: 8px;
                    margin-top: 20px; box-shadow: 0px 4px 10px rgba(45, 137, 239, 0.3); }
                .cta-button:hover { background: linear-gradient(135deg, #1e63c4, #0c48a1); }
                .footer { font-size: 14px; color: #777; margin-top: 30px; }
                .footer a { color: #2d89ef; text-decoration: none; }
                .footer a:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <div class="email-container">
                <img src="https://cdn-icons-png.flaticon.com/512/942/942799.png" class="logo" alt="SecureDoc Logo" />
                <p class="header">Verify Your SecureDoc Account</p>
                <p class="subtext">You're almost there! Just one more step to secure your account.</p>
                <div class="content">
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Thank you for signing up with <strong>SecureDoc</strong>! To get started, click the button below to verify your email address.</p>
                    <a href="%s/verify?key=%s" class="cta-button">Verify Your Email</a>
                    <p>If the button above doesn’t work, use this link:</p>
                    <p><a href="%s/verify?key=%s">%s/verify?key=%s</a></p>
                </div>
                <p class="footer">Best regards,<br><strong>The SecureDoc Team</strong><br>
                ✉ <a href="mailto:support@securedoc.com">Contact Support</a></p>
            </div>
        </body>
        </html>
        """.formatted(name, host, key, host, key, host, key);
    }

}
