package com.ccms.servlet;

import com.ccms.dao.UserDAO;
import com.ccms.model.User;
import com.ccms.util.EmailUtil;
import com.ccms.util.PasswordUtil;
import com.ccms.util.ServletUtil;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@WebServlet("/api/forgot-password/*")
public class ForgotPasswordServlet extends HttpServlet {
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(10);
    private static final ConcurrentHashMap<String, OtpEntry> OTP_STORE = new ConcurrentHashMap<>();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String pathInfo = request.getPathInfo();

        try {
            if ("/send-otp".equals(pathInfo)) {
                handleSendOtp(request, response);
                return;
            }

            if ("/reset".equals(pathInfo)) {
                handleResetPassword(request, response);
                return;
            }

            ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Forgot password endpoint not found.");
        } catch (SQLException exception) {
            throw new ServletException("Forgot password request failed.", exception);
        } catch (MessagingException exception) {
            throw new ServletException("Unable to send OTP email.", exception);
        }
    }

    private void handleSendOtp(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, MessagingException {
        SendOtpRequest otpRequest = ServletUtil.readJsonBody(request, SendOtpRequest.class);
        String email = normalizeEmail(otpRequest != null ? otpRequest.email : null);

        if (isBlank(email)) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Email is required.");
            return;
        }

        if (!EmailUtil.isConfigured()) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "SMTP email settings are not configured on the server.");
            return;
        }

        User user = userDAO.findByEmail(email);
        if (user == null) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "No account found for that email address.");
            return;
        }

        pruneExpiredOtps();
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        OTP_STORE.put(email, new OtpEntry(otp, Instant.now().plus(OTP_VALIDITY)));
        EmailUtil.sendOtpEmail(email, otp);

        ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of(
                "message", "OTP sent successfully. It will expire in 10 minutes."
        ));
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        ResetPasswordRequest resetRequest = ServletUtil.readJsonBody(request, ResetPasswordRequest.class);
        String email = normalizeEmail(resetRequest != null ? resetRequest.email : null);
        String otp = clean(resetRequest != null ? resetRequest.otp : null);
        String newPassword = resetRequest != null ? resetRequest.newPassword : null;

        if (isBlank(email) || isBlank(otp) || isBlank(newPassword)) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Email, OTP, and new password are required.");
            return;
        }

        pruneExpiredOtps();
        OtpEntry otpEntry = OTP_STORE.get(email);
        if (otpEntry == null || otpEntry.isExpired() || !otpEntry.code.equals(otp)) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or expired OTP.");
            return;
        }

        boolean updated = userDAO.updatePasswordByEmail(email, PasswordUtil.hashPassword(newPassword));
        if (!updated) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "No account found for that email address.");
            return;
        }

        OTP_STORE.remove(email);
        ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of(
                "message", "Password updated successfully."
        ));
    }

    private void pruneExpiredOtps() {
        Instant now = Instant.now();
        OTP_STORE.entrySet().removeIf((entry) -> entry.getValue().expiresAt.isBefore(now));
    }

    private String normalizeEmail(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class SendOtpRequest {
        private String email;
    }

    private static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
    }

    private static class OtpEntry {
        private final String code;
        private final Instant expiresAt;

        private OtpEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
