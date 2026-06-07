package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.UserDAO;
import com.ccms.model.User;
import com.ccms.util.PasswordUtil;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@WebServlet("/api/register")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 10 * 1024 * 1024
)
public class RegisterServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private static final Set<String> ALLOWED_OCCUPATIONS = Set.of("Lawyer", "Judge", "Admin", "Staff", "Citizen");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            String fullName = clean(request.getParameter("name"));
            String occupation = normalizeRole(clean(request.getParameter("occupation")));
            String mobile = clean(request.getParameter("mobile"));
            String email = clean(request.getParameter("email"));
            String password = request.getParameter("password");
            String aadhaarNumber = clean(request.getParameter("aadhaarNumber"));
            String barCouncilNumber = clean(request.getParameter("barCouncilNumber"));
            String courtId = clean(request.getParameter("courtId"));

            if (isBlank(fullName) || isBlank(occupation) || isBlank(mobile) || isBlank(email) || isBlank(password) || isBlank(aadhaarNumber)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Full name, occupation, mobile, email, password, and Aadhaar / National ID are required.");
                return;
            }

            if (!ALLOWED_OCCUPATIONS.contains(occupation)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Occupation must be Lawyer, Judge, Admin, Staff, or Citizen.");
                return;
            }

            if ("Lawyer".equals(occupation) && isBlank(barCouncilNumber)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Bar Council Number is required for lawyers.");
                return;
            }

            if ("Judge".equals(occupation) && isBlank(courtId)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Court ID is required for judges.");
                return;
            }

            if (userDAO.emailExists(email)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_CONFLICT, "Email already exists.");
                return;
            }

            if (!isBlank(mobile) && userDAO.mobileExists(mobile)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_CONFLICT, "Mobile number already exists.");
                return;
            }

            User user = new User();
            user.setName(fullName);
            user.setOccupation(occupation);
            user.setRole(occupation);
            user.setMobile(mobile);
            user.setEmail(email);
            user.setPassword(PasswordUtil.hashPassword(password));
            user.setAadhaarNumber(aadhaarNumber);
            user.setBarCouncilNumber("Lawyer".equals(occupation) ? barCouncilNumber : null);
            user.setCourtId("Judge".equals(occupation) ? courtId : null);
            user.setProfilePhotoUrl(storeProfilePhoto(request));
            user.setApprovalStatus("Approved");

            User createdUser = userDAO.createUser(user);

            notificationDAO.create(
                    "New user registered",
                    fullName + " registered as " + occupation + ".",
                    "UserRegistration",
                    null
            );

            HttpSession session = request.getSession(true);
            session.setAttribute("user", createdUser);
            session.setMaxInactiveInterval(30 * 60);

            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, Map.of(
                    "message", "Registration successful.",
                    "user", createdUser
            ));
        } catch (SQLException exception) {
            throw new ServletException("Unable to register user.", exception);
        }
    }

    private String storeProfilePhoto(HttpServletRequest request) throws IOException, ServletException {
        Part photoPart = request.getPart("profilePhoto");
        if (photoPart == null || photoPart.getSize() <= 0) {
            return null;
        }

        String submittedFileName = extractSubmittedFileName(photoPart);
        if (isBlank(submittedFileName)) {
            return null;
        }

        String safeExtension = "";
        int extensionIndex = submittedFileName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            safeExtension = submittedFileName.substring(extensionIndex).replaceAll("[^A-Za-z0-9.]", "");
        }

        String generatedFileName = "profile-" + UUID.randomUUID() + safeExtension;
        String uploadFolder = request.getServletContext().getRealPath("/uploads/profile-photos");
        Path uploadDirectory = uploadFolder != null
                ? Paths.get(uploadFolder)
                : Paths.get(System.getProperty("java.io.tmpdir"), "ccms-profile-photos");

        Files.createDirectories(uploadDirectory);
        Path targetPath = uploadDirectory.resolve(generatedFileName);
        Files.copy(photoPart.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return request.getContextPath() + "/uploads/profile-photos/" + generatedFileName;
    }

    private String extractSubmittedFileName(Part part) {
        String submittedFileName = part.getSubmittedFileName();
        if (submittedFileName == null) {
            return null;
        }

        String normalized = submittedFileName.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRole(String value) {
        if (isBlank(value)) {
            return value;
        }

        switch (value.trim().toUpperCase(Locale.ROOT)) {
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
                return value.trim();
        }
    }
}
