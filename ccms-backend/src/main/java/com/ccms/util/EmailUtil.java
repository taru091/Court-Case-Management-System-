package com.ccms.util;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {
    private static final String SMTP_HOST = System.getenv().getOrDefault("CCMS_SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT = System.getenv().getOrDefault("CCMS_SMTP_PORT", "587");
    private static final String SMTP_EMAIL = System.getenv().getOrDefault("CCMS_SMTP_EMAIL", "");
    private static final String SMTP_PASSWORD = System.getenv().getOrDefault("CCMS_SMTP_PASSWORD", "");

    private EmailUtil() {
    }

    public static boolean isConfigured() {
        return !isBlank(SMTP_EMAIL) && !isBlank(SMTP_PASSWORD);
    }

    public static void sendOtpEmail(String recipientEmail, String otp) throws MessagingException {
        if (!isConfigured()) {
            throw new MessagingException("SMTP settings are not configured.");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_EMAIL, SMTP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("CCMS Password Reset OTP");
        message.setText("Your CCMS password reset OTP is " + otp + ". It expires in 10 minutes.");

        Transport.send(message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
