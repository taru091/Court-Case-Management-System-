package com.ccms.util;

import com.ccms.model.User;
import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServletUtil {
    private static final Gson GSON = JsonUtil.getGson();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ServletUtil() {
    }

    public static <T> T readJsonBody(HttpServletRequest request, Class<T> clazz) throws IOException {
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
        }
        return GSON.fromJson(payload.toString(), clazz);
    }

    public static void sendJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(body));
    }

    public static void sendMessage(HttpServletResponse response, int statusCode, String message) throws IOException {
        Map<String, String> result = new HashMap<>();
        result.put("message", message);
        sendJson(response, statusCode, result);
    }

    public static User getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }

    public static boolean requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (getSessionUser(request) == null) {
            sendMessage(response, HttpServletResponse.SC_UNAUTHORIZED, "Please login to continue.");
            return false;
        }
        return true;
    }

    public static boolean requireRole(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Set<String> allowedRoles) throws IOException {
        User user = getSessionUser(request);
        if (user == null) {
            sendMessage(response, HttpServletResponse.SC_UNAUTHORIZED, "Please login to continue.");
            return false;
        }

        if (!allowedRoles.contains(user.getRole())) {
            sendMessage(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission for this action.");
            return false;
        }
        return true;
    }

    public static String formatNow() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static int parseIdFromPath(String pathInfo) {
        if (pathInfo == null || pathInfo.trim().isEmpty() || "/".equals(pathInfo)) {
            return -1;
        }
        String cleanedPath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return Integer.parseInt(cleanedPath);
    }
}
