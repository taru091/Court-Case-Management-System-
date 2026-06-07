package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.DocumentDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.CaseDocument;
import com.ccms.model.User;
import com.ccms.util.ApprovalWorkflowUtil;
import com.ccms.util.JsonUtil;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@WebServlet("/api/documents/*")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 20 * 1024 * 1024,
        maxRequestSize = 25 * 1024 * 1024
)
public class DocumentServlet extends HttpServlet {
    private static final Set<String> DOCUMENT_EDIT_ROLES = Set.of("Admin", "Lawyer");
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".pdf", ".docx", ".png", ".jpg", ".jpeg", ".gif", ".webp");

    private final DocumentDAO documentDAO = new DocumentDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            int caseId = ServletUtil.parseIdFromPath(request.getPathInfo());
            User user = ServletUtil.getSessionUser(request);
            if (user != null && "Lawyer".equals(user.getRole())) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, documentDAO.getDocumentsByCaseIdForUser(caseId, user));
                return;
            }
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, documentDAO.getDocumentsByCaseId(caseId));
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch documents.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, DOCUMENT_EDIT_ROLES)) {
            return;
        }

        try {
            User user = ServletUtil.getSessionUser(request);
            CaseDocument document = isMultipart(request)
                    ? buildDocumentFromMultipart(request, user)
                    : buildDocumentFromJson(request, user);

            if (document == null || document.getCaseId() <= 0 || isBlank(document.getFileUrl()) || isBlank(document.getFileName())) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Case id and a supported document file are required.");
                return;
            }

            validateSupportedDocument(document.getFileName());

            if ("Admin".equals(user.getRole())) {
                document.setApprovalStatus("APPROVED");
                document.setOfficialDocument(true);
                document.setPublicDocument(document.isPublicDocument());
                CaseDocument createdDocument = documentDAO.addDocument(document);

                ApprovalRequestRecord createdRequest = approvalRequestDAO.create(buildApprovalRequest(
                        user,
                        null,
                        createdDocument,
                        ApprovalWorkflowUtil.ACTION_CREATE,
                        "Admin document upload for case #" + createdDocument.getCaseId(),
                        true
                ));

                notificationDAO.createForRole(
                        "New admin approval request pending",
                        "A document upload is waiting for judge review.",
                        "ApprovalWorkflow",
                        createdRequest.getId(),
                        "Judge",
                        null,
                        null,
                        user.getId()
                );
                auditLogDAO.log(user, "Admin uploaded document " + createdDocument.getFileName() + " and submitted it for judge review");
                ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, createdDocument);
                return;
            }

            document.setApprovalStatus("PENDING");
            document.setOfficialDocument(false);
            document.setPublicDocument(false);
            CaseDocument createdDocument = documentDAO.addDocument(document);

            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(buildApprovalRequest(
                    user,
                    null,
                    createdDocument,
                    ApprovalWorkflowUtil.ACTION_CREATE,
                    "Lawyer document upload for case #" + createdDocument.getCaseId(),
                    false
            ));

            notificationDAO.createForRole(
                    "New lawyer approval request pending",
                    "A lawyer document is awaiting admin approval.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Admin",
                    null,
                    null,
                    user.getId()
            );
            notificationDAO.createForUser(
                    "Your document is awaiting admin approval",
                    "Your document upload has been submitted for review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    user,
                    user.getId()
            );
            auditLogDAO.log(user, "Lawyer uploaded document " + createdDocument.getFileName() + " for admin approval");
            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, createdDocument);
        } catch (SQLException exception) {
            throw new ServletException("Unable to upload document.", exception);
        } catch (IllegalStateException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        }
    }

    private ApprovalRequestRecord buildApprovalRequest(User requester,
                                                       CaseDocument before,
                                                       CaseDocument after,
                                                       String actionType,
                                                       String title,
                                                       boolean liveChangeApplied) {
        ApprovalRequestRecord record = new ApprovalRequestRecord();
        record.setRequestType(requester.getRole().toUpperCase(Locale.ROOT) + "_DOCUMENT_" + actionType);
        record.setRequestedByRole(requester.getRole());
        record.setRequestedByUser(requester.getId());
        record.setRequestedByName(requester.getName());
        record.setApprovalRole("Admin".equals(requester.getRole()) ? "Judge" : "Admin");
        record.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_DOCUMENT);
        record.setTargetEntityId(after != null ? after.getDocumentId() : null);
        record.setActionType(actionType);
        record.setRequestTitle(title);
        record.setRequestPayload(JsonUtil.getGson().toJson(after));
        record.setBeforePayload(before == null ? null : JsonUtil.getGson().toJson(before));
        record.setAfterPayload(after == null ? null : JsonUtil.getGson().toJson(after));
        record.setLiveChangeApplied(liveChangeApplied);
        record.setStatus("PENDING");
        return record;
    }

    private CaseDocument buildDocumentFromJson(HttpServletRequest request, User user) throws IOException {
        DocumentInput input = ServletUtil.readJsonBody(request, DocumentInput.class);
        if (input == null) {
            return null;
        }

        CaseDocument document = new CaseDocument();
        document.setCaseId(input.caseId);
        document.setFileUrl(input.fileUrl);
        document.setFileName(resolveFileName(input.fileName, input.fileUrl));
        document.setMimeType(input.mimeType);
        document.setUploadedByUserId(user != null ? user.getId() : null);
        document.setUploadedByRole(user != null ? user.getRole() : null);
        document.setPublicDocument(input.publicDocument == null || input.publicDocument);
        return document;
    }

    private CaseDocument buildDocumentFromMultipart(HttpServletRequest request, User user) throws IOException, ServletException {
        String caseIdValue = request.getParameter("caseId");
        if (isBlank(caseIdValue)) {
            return null;
        }

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() <= 0) {
            return null;
        }

        String submittedFileName = extractSubmittedFileName(filePart);
        validateSupportedDocument(submittedFileName);

        String safeExtension = submittedFileName.substring(submittedFileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String generatedFileName = UUID.randomUUID() + safeExtension;
        Path uploadDirectory = resolveUploadDirectory(user);
        Files.createDirectories(uploadDirectory);
        Path targetPath = uploadDirectory.resolve(generatedFileName);
        Files.copy(filePart.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        CaseDocument document = new CaseDocument();
        document.setCaseId(Integer.parseInt(caseIdValue));
        document.setFileUrl(targetPath.toAbsolutePath().toString());
        document.setFileName(submittedFileName);
        document.setMimeType(filePart.getContentType());
        document.setUploadedByUserId(user != null ? user.getId() : null);
        document.setUploadedByRole(user != null ? user.getRole() : null);
        document.setPublicDocument("Admin".equals(user != null ? user.getRole() : null) && !"false".equalsIgnoreCase(request.getParameter("publicDocument")));
        return document;
    }

    private Path resolveUploadDirectory(User user) {
        String configuredRoot = System.getenv().getOrDefault("CCMS_UPLOAD_DIR", "");
        if (!configuredRoot.trim().isEmpty()) {
            return Paths.get(configuredRoot, user != null ? user.getRole().toLowerCase(Locale.ROOT) : "general");
        }
        return Paths.get(System.getProperty("user.home"), "ccms-uploads", user != null ? user.getRole().toLowerCase(Locale.ROOT) : "general");
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("multipart/form-data");
    }

    private void validateSupportedDocument(String fileName) {
        if (isBlank(fileName)) {
            throw new IllegalStateException("A supported PDF, DOCX, or image file is required.");
        }

        String normalized = fileName.toLowerCase(Locale.ROOT);
        boolean supported = SUPPORTED_EXTENSIONS.stream().anyMatch(normalized::endsWith);
        if (!supported) {
            throw new IllegalStateException("Supported file types are PDF, DOCX, and images.");
        }
    }

    private String resolveFileName(String providedFileName, String fileUrl) {
        if (!isBlank(providedFileName)) {
            return providedFileName.trim();
        }
        if (isBlank(fileUrl)) {
            return null;
        }
        String normalized = fileUrl.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class DocumentInput {
        private int caseId;
        private String fileUrl;
        private String fileName;
        private String mimeType;
        private Boolean publicDocument;
    }
}
