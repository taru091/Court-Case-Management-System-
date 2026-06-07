package com.ccms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.UserDAO;
import com.ccms.model.LoginRequest;
import com.ccms.model.User;
import com.ccms.util.PasswordUtil;
import com.ccms.util.ServletUtil;

@WebServlet({"/api/auth/*", "/login", "/logout", "/session"})
public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private static final Set<String> ALLOWED_ROLES = Set.of("Admin", "Lawyer", "Judge", "Staff", "Citizen");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = resolveAction(request);

        if ("session".equals(action)) {
            User user = ServletUtil.getSessionUser(request);
            if (user == null) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of("authenticated", false));
                return;
            }

            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of(
                    "authenticated", true,
                    "user", user
            ));
            return;
        }

        ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Auth endpoint not found.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String action = resolveAction(request);

        try {
            if ("login".equals(action)) {
                handleLogin(request, response);
                return;
            }

            if ("register".equals(action)) {
                handleRegister(request, response);
                return;
            }

            if ("logout".equals(action)) {
                handleLogout(request, response);
                return;
            }

            ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Auth endpoint not found.");
        } catch (SQLException exception) {
            throw new ServletException("Auth request failed.", exception);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        LoginRequest loginRequest = readLoginRequest(request);
        String identifier = firstFilled(
                loginRequest != null ? loginRequest.getIdentifier() : null,
                loginRequest != null ? loginRequest.getUsername() : null,
                loginRequest != null ? loginRequest.getEmail() : null
        );
        String password = loginRequest != null ? loginRequest.getPassword() : null;

        if (isBlank(identifier) || isBlank(password)) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Username, email, or phone number and password are required.");
            return;
        }

        User user = userDAO.findByLoginIdentifier(identifier.trim());

        if (user == null || !PasswordUtil.matchesPassword(password, user.getPassword())) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            return;
        }

        if (!ALLOWED_ROLES.contains(user.getRole())) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_FORBIDDEN, "This account type is not enabled for sign in.");
            return;
        }

        if (user.getApprovalStatus() != null && !"Approved".equalsIgnoreCase(user.getApprovalStatus())) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_FORBIDDEN, "Your account is awaiting approval.");
            return;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        if (!hashedPassword.equalsIgnoreCase(user.getPassword())) {
            userDAO.updatePasswordByEmail(user.getEmail(), hashedPassword);
        }

        user.setPassword(null);
        createAuthenticatedSession(request, user);

        ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of(
                "authenticated", true,
                "message", "Login successful.",
                "role", user.getRole(),
                "user", user
        ));
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        User user = ServletUtil.readJsonBody(request, User.class);

        if (user == null || isBlank(user.getName()) || isBlank(user.getEmail()) || isBlank(user.getPassword())) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Name, email, and password are required.");
            return;
        }

        String normalizedRole = normalizeRole(user.getRole());
        if (normalizedRole == null || !ALLOWED_ROLES.contains(normalizedRole)) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Role must be Admin, Lawyer, Judge, Staff, or Citizen.");
            return;
        }

        if (userDAO.emailExists(user.getEmail())) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_CONFLICT, "Email already exists.");
            return;
        }

        user.setRole(normalizedRole);
        user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        user.setApprovalStatus("Approved");
        User createdUser = userDAO.createUser(user);

        notificationDAO.create(
                "New user registered",
                user.getName() + " registered as " + user.getRole() + ".",
                "UserRegistration",
                null
        );

        createAuthenticatedSession(request, createdUser);

        ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, Map.of(
                "authenticated", true,
                "message", "Registration successful.",
                "role", createdUser.getRole(),
                "user", createdUser
        ));
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ServletUtil.sendMessage(response, HttpServletResponse.SC_OK, "Logged out successfully.");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstFilled(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveAction(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();

        if ("/login".equals(servletPath) || "/login".equals(pathInfo)) {
            return "login";
        }

        if ("/logout".equals(servletPath) || "/logout".equals(pathInfo)) {
            return "logout";
        }

        if ("/session".equals(servletPath) || "/session".equals(pathInfo)) {
            return "session";
        }

        if ("/register".equals(pathInfo)) {
            return "register";
        }

        return "";
    }

    private LoginRequest readLoginRequest(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            return ServletUtil.readJsonBody(request, LoginRequest.class);
        }

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(request.getParameter("identifier"));
        loginRequest.setUsername(request.getParameter("username"));
        loginRequest.setEmail(request.getParameter("email"));
        loginRequest.setPassword(request.getParameter("password"));
        return loginRequest;
    }

    private void createAuthenticatedSession(HttpServletRequest request, User user) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(30 * 60);
    }

    private String normalizeRole(String rawRole) {
        if (isBlank(rawRole)) {
            return null;
        }

        switch (rawRole.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN":
                return "Admin";
            case "LAWYER":
                return "Lawyer";
            case "JUDGE":
                return "Judge";
            case "STAFF":
                return "Staff";
            case "CITIZEN":
                return "Citizen";
            default:
                return null;
        }
    }
}
