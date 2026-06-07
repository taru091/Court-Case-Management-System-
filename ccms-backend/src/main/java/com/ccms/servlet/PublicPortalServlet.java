package com.ccms.servlet;

import com.ccms.dao.CaseDAO;
import com.ccms.dao.DocumentDAO;
import com.ccms.dao.SiteSettingDAO;
import com.ccms.model.CaseDocument;
import com.ccms.model.CaseRecord;
import com.ccms.model.SiteSettingRecord;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/public/*")
public class PublicPortalServlet extends HttpServlet {
    private final SiteSettingDAO siteSettingDAO = new SiteSettingDAO();
    private final CaseDAO caseDAO = new CaseDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = request.getPathInfo();

        try {
            if (path == null || "/".equals(path) || "/home".equals(path)) {
                Map<String, String> settings = siteSettingDAO.getAll().stream()
                        .collect(Collectors.toMap(SiteSettingRecord::getKey, SiteSettingRecord::getValue));
                List<Map<String, Object>> featuredCases = caseDAO.getAllCases().stream()
                        .limit(5)
                        .map(this::toPublicCaseSummary)
                        .collect(Collectors.toList());

                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of(
                        "settings", settings,
                        "featuredCases", featuredCases
                ));
                return;
            }

            if ("/cases/search".equals(path)) {
                String query = request.getParameter("query");
                List<CaseRecord> cases = (query == null || query.trim().isEmpty())
                        ? caseDAO.getAllCases()
                        : caseDAO.searchCases(query.trim());
                List<Map<String, Object>> publicCases = cases.stream()
                        .limit(20)
                        .map(this::toPublicCaseSummary)
                        .collect(Collectors.toList());

                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, publicCases);
                return;
            }

            if ("/cases/citizen-search".equals(path)) {
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String identifier = phone != null && !phone.trim().isEmpty() ? phone.trim() : (email != null ? email.trim() : "");
                List<CaseRecord> cases = identifier.isEmpty()
                        ? caseDAO.getAllCases()
                        : caseDAO.searchCases(identifier);
                List<Map<String, Object>> publicCases = cases.stream()
                        .limit(20)
                        .map(this::toPublicCaseSummary)
                        .collect(Collectors.toList());

                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, publicCases);
                return;
            }

            if (path.startsWith("/cases/")) {
                int caseId = Integer.parseInt(path.substring("/cases/".length()));
                CaseRecord caseRecord = caseDAO.getCaseById(caseId);
                if (caseRecord == null) {
                    ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Case not found.");
                    return;
                }

                List<CaseDocument> publicDocuments = documentDAO.getPublicDocumentsByCaseId(caseId);
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of(
                        "caseId", caseRecord.getCaseId(),
                        "caseName", caseRecord.getCaseName(),
                        "status", caseRecord.getStatus(),
                        "judgeName", caseRecord.getJudgeName(),
                        "hearingDate", caseRecord.getNextHearingDate() == null ? "Not scheduled" : caseRecord.getNextHearingDate(),
                        "courtDetails", caseRecord.getNextCourtroom() == null ? caseRecord.getCourtDetails() : caseRecord.getNextCourtroom(),
                        "documents", publicDocuments.stream()
                                .map(document -> Map.of(
                                        "documentId", document.getDocumentId(),
                                        "fileName", document.getFileName(),
                                        "fileUrl", document.getFileUrl()
                                ))
                                .collect(Collectors.toList())
                ));
                return;
            }

            ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Public endpoint not found.");
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch public portal data.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    private Map<String, Object> toPublicCaseSummary(CaseRecord caseRecord) {
        return Map.of(
                "caseId", caseRecord.getCaseId(),
                "caseName", caseRecord.getCaseName(),
                "judgeName", caseRecord.getJudgeName(),
                "status", caseRecord.getStatus(),
                "courtDetails", caseRecord.getNextCourtroom() == null ? caseRecord.getCourtDetails() : caseRecord.getNextCourtroom(),
                "nextHearingDate", caseRecord.getNextHearingDate() == null ? "Not scheduled" : caseRecord.getNextHearingDate()
        );
    }
}
