const configuredApiBaseUrl = String(import.meta.env.VITE_API_BASE_URL || "").trim();
const defaultDevApiBaseUrl =
  import.meta.env.DEV && typeof window !== "undefined"
    ? `${window.location.protocol}//${window.location.hostname}:8080/ccms-backend`
    : "";

export const API_BASE_URL = configuredApiBaseUrl
  ? configuredApiBaseUrl.replace(/\/$/, "")
  : defaultDevApiBaseUrl;
export const CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
export const CLAUDE_MODEL = "claude-sonnet-4-20250514";
export const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
export const GROQ_MODEL = "llama-3.3-70b-versatile";
export const GROQ_VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
export const SERVICE_UNAVAILABLE_MESSAGE =
  "Service is temporarily unavailable. Please try again shortly.";
export const JUDICIARY_WARNING =
  "I only assist with Indian judiciary and legal system related queries.";

export const JUDICIARY_ALLOWED_TOPICS = [
  "Indian Constitution",
  "Supreme Court",
  "High Court",
  "IPC",
  "CrPC",
  "FIR",
  "Court procedures",
  "Hearings",
  "Legal documents",
  "Court case workflow"
];

export const JUDICIARY_BLOCKED_TOPICS = [
  "Coding",
  "Movies",
  "Sports",
  "Gaming",
  "Random general topics"
];

const judiciaryAllowedKeywords = [
  "indian constitution",
  "constitution",
  "constitutional",
  "supreme court",
  "high court",
  "district court",
  "court",
  "judge",
  "judiciary",
  "case",
  "petition",
  "appeal",
  "writ",
  "bail",
  "order",
  "hearing",
  "adjournment",
  "filing",
  "registry",
  "bench",
  "legal",
  "law",
  "litigation",
  "fir",
  "ipc",
  "crpc",
  "evidence",
  "affidavit",
  "pleading",
  "complaint",
  "trial",
  "criminal",
  "civil",
  "constitutional article",
  "article ",
  "section ",
  "advocate",
  "lawyer",
  "legal notice",
  "summons",
  "witness",
  "charge sheet",
  "charge-sheet",
  "court management",
  "judiciary analytics",
  "legal strategy",
  "legal summary",
  "delay",
  "case delay"
];

const judiciaryRejectedKeywords = [
  "movie",
  "movies",
  "cinema",
  "actor",
  "actress",
  "sports",
  "football",
  "cricket score",
  "match",
  "gaming",
  "game",
  "minecraft",
  "playstation",
  "xbox",
  "coding",
  "programming",
  "javascript",
  "python code",
  "react app",
  "weather",
  "recipe",
  "travel plan",
  "celebrity",
  "stock market tips",
  "lottery"
];

async function parseResponse(response) {
  const rawText = await response.text();
  if (!rawText) return null;
  try {
    return JSON.parse(rawText);
  } catch {
    return { message: rawText };
  }
}

function normalizeJudiciaryPrompt(value) {
  return String(value || "")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

export function isJudiciaryPrompt(prompt) {
  const normalizedPrompt = normalizeJudiciaryPrompt(prompt);

  if (!normalizedPrompt) {
    return false;
  }

  const hasAllowedKeyword = judiciaryAllowedKeywords.some((keyword) =>
    normalizedPrompt.includes(keyword)
  );
  const hasRejectedKeyword = judiciaryRejectedKeywords.some((keyword) =>
    normalizedPrompt.includes(keyword)
  );

  if (hasRejectedKeyword && !hasAllowedKeyword) {
    return false;
  }

  if (hasAllowedKeyword) {
    return true;
  }

  return [
    "explain article",
    "what is article",
    "explain ipc",
    "explain crpc",
    "summarize case",
    "summarize fir",
    "analyze fir",
    "predict hearing",
    "predict case delay",
    "generate legal summary",
    "explain constitutional article",
    "suggest legal strategy",
    "legal advice"
  ].some((starter) => normalizedPrompt.startsWith(starter));
}

export function buildRestrictedJudiciaryPrompt(userPrompt) {
  return `
You are an Indian Judiciary AI Assistant for a Court Case Management System.

ONLY answer questions related to:

* Indian Constitution
* Indian Judiciary
* IPC
* CrPC
* FIR
* Court procedures
* Hearings
* Legal documents
* Court case workflow

If question is unrelated, reply:
"I only assist with Indian judiciary and legal system related queries."

User Question:
${userPrompt}
`.trim();
}

function getGroqApiKey() {
  const apiKey = import.meta.env.VITE_GROQ_API_KEY;

  if (
    !apiKey ||
    apiKey === "your_api_key_here" ||
    apiKey === "your_groq_api_key_here" ||
    apiKey === "ADD_YOUR_GROQ_API_KEY_HERE"
  ) {
    throw new Error("VITE_GROQ_API_KEY is not configured in ccms-frontend/.env.");
  }

  return apiKey;
}

async function requestGroqChatCompletion({ systemPrompt, userPrompt, attachments = [] }) {
  const textAttachments = [];
  const imageAttachments = [];

  attachments.forEach((attachment) => {
    if (attachment?.kind === "text" && attachment.text) {
      textAttachments.push(attachment);
    }

    if (attachment?.kind === "image" && attachment.dataUrl) {
      imageAttachments.push(attachment);
    }
  });

  const attachmentText = textAttachments.length
    ? `\n\nAttached reference material:\n${textAttachments
        .map(
          (attachment) =>
            `File: ${attachment.name || "document"}\n${attachment.text || ""}`.trim()
        )
        .join("\n\n")}`
    : "";
  const finalUserPrompt = `${userPrompt}${attachmentText}`.trim();
  const apiKey = getGroqApiKey();
  const response = await fetch(GROQ_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model: imageAttachments.length ? GROQ_VISION_MODEL : GROQ_MODEL,
      temperature: 0.3,
      max_tokens: 1200,
      messages: [
        {
          role: "system",
          content: systemPrompt
        },
        {
          role: "user",
          content: imageAttachments.length
            ? [
                {
                  type: "text",
                  text: finalUserPrompt
                },
                ...imageAttachments.map((attachment) => ({
                  type: "image_url",
                  image_url: {
                    url: attachment.dataUrl
                  }
                }))
              ]
            : finalUserPrompt
        }
      ]
    })
  });
  const data = await parseResponse(response);

  if (!response.ok) {
    const errorMessage = data?.error?.message || data?.message || "Groq request failed.";
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data?.choices?.[0]?.message?.content?.trim() || "";
}

export async function requestJudiciaryAiResponse({
  userPrompt,
  promptToValidate = userPrompt,
  attachments = [],
  aiBehaviorNote = ""
}) {
  if (!isJudiciaryPrompt(promptToValidate)) {
    return JUDICIARY_WARNING;
  }

  const text = await requestGroqChatCompletion({
    systemPrompt:
      `You are an Indian judiciary AI assistant for a Court Case Management System. Only answer questions related to the Indian Constitution, judiciary, IPC, CrPC, FIRs, court procedures, hearings, legal documents, and court case workflow. If the question is unrelated, reply exactly: "I only assist with Indian judiciary and legal system related queries." ${aiBehaviorNote}`.trim(),
    userPrompt: buildRestrictedJudiciaryPrompt(userPrompt),
    attachments
  });

  return text || JUDICIARY_WARNING;
}

function createAppError(message, status = 0, data = null) {
  const error = new Error(message);
  error.status = status;
  error.data = data;
  return error;
}

export function getFriendlyErrorMessage(error, fallbackMessage = SERVICE_UNAVAILABLE_MESSAGE) {
  if (!error) {
    return fallbackMessage;
  }

  const rawMessage = String(error.message || "").trim();
  const loweredMessage = rawMessage.toLowerCase();
  const status = Number(error.status || 0);
  const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(rawMessage);

  if (
    looksLikeHtml ||
    status >= 500 ||
    loweredMessage.includes("failed to fetch") ||
    loweredMessage.includes("networkerror") ||
    loweredMessage.includes("load failed") ||
    loweredMessage.includes("temporarily unavailable") ||
    loweredMessage.includes("network request failed") ||
    loweredMessage.includes("err_connection") ||
    loweredMessage.includes("err_name") ||
    loweredMessage.includes("timeout") ||
    loweredMessage.includes("cors") ||
    loweredMessage.includes("internal server error") ||
    loweredMessage.includes("servlet") ||
    loweredMessage.includes("sqlexception") ||
    loweredMessage.includes("nullpointerexception") ||
    loweredMessage.includes("unable to") ||
    loweredMessage.includes("error 500") ||
    loweredMessage.includes("error 502") ||
    loweredMessage.includes("error 503")
  ) {
    return SERVICE_UNAVAILABLE_MESSAGE;
  }

  return rawMessage || fallbackMessage;
}

export async function apiRequest(path, options = {}) {
  const headers = {
    ...(options.headers || {})
  };

  if (
    options.body &&
    typeof options.body === "string" &&
    !headers["Content-Type"] &&
    !headers["content-type"]
  ) {
    headers["Content-Type"] = "application/json";
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: "include",
      ...options,
      headers
    });
  } catch (error) {
    throw createAppError(SERVICE_UNAVAILABLE_MESSAGE, 0, {
      cause: error?.message || "Network failure"
    });
  }

  const data = await parseResponse(response);
  if (!response.ok) {
    const serverMessage = String(data?.message || "").trim();
    const isHtmlMessage = /<\/?[a-z][\s\S]*>/i.test(serverMessage);
    throw createAppError(
      response.status >= 500 || isHtmlMessage
        ? SERVICE_UNAVAILABLE_MESSAGE
        : serverMessage || "Request failed.",
      response.status,
      data
    );
  }
  return data;
}

export async function loginUser(username, password) {
  const body = new URLSearchParams({ username, password });
  const data = await apiRequest("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body.toString()
  });

  if (!data?.user) {
    throw createAppError("Login response did not include a user session.", 500, data);
  }

  return data;
}

export async function registerUser(formData) {
  return apiRequest("/api/register", {
    method: "POST",
    body: formData
  });
}

export async function requestPasswordOtp(email) {
  return apiRequest("/api/forgot-password/send-otp", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ email })
  });
}

export async function resetPasswordWithOtp(email, otp, newPassword) {
  return apiRequest("/api/forgot-password/reset", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ email, otp, newPassword })
  });
}

export async function logoutUser() {
  return apiRequest("/api/auth/logout", { method: "POST" });
}

export async function getSessionUser() {
  return apiRequest("/api/auth/session", { method: "GET" });
}

export async function getCases() {
  return apiRequest("/api/cases", { method: "GET" });
}

export async function getHearingsByCaseId(caseId) {
  return apiRequest(`/api/hearings/${caseId}`, { method: "GET" });
}

export async function getDocumentsByCaseId(caseId) {
  return apiRequest(`/api/documents/${caseId}`, { method: "GET" });
}

export async function uploadCaseDocument(payload) {
  if (payload?.file instanceof File) {
    const formData = new FormData();
    formData.append("caseId", String(payload.caseId));
    formData.append("file", payload.file);
    formData.append("publicDocument", String(Boolean(payload.publicDocument)));
    return apiRequest("/api/documents", {
      method: "POST",
      body: formData
    });
  }

  return apiRequest("/api/documents", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchPublicPortalData() {
  return apiRequest("/api/public/home", { method: "GET" });
}

export async function searchPublicCases(query) {
  return apiRequest(`/api/public/cases/search?query=${encodeURIComponent(query || "")}`, {
    method: "GET"
  });
}

export async function getPublicCaseById(caseId) {
  return apiRequest(`/api/public/cases/${encodeURIComponent(caseId)}`, {
    method: "GET"
  });
}

export async function citizenSearchCases(phone, email) {
  const params = new URLSearchParams();
  if (phone) params.set("phone", phone);
  if (email) params.set("email", email);
  return apiRequest(`/api/public/cases/citizen-search?${params.toString()}`, {
    method: "GET"
  });
}

export async function listChangeRequests() {
  return apiRequest("/api/approval-requests", { method: "GET" });
}

export async function submitChangeRequest(payload) {
  return apiRequest("/api/change-requests", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function reviewChangeRequest(requestId, decision, note = "") {
  return apiRequest(`/api/approval-requests/${requestId}/review`, {
    method: "PUT",
    body: JSON.stringify({
      decision,
      note,
      rejectionReason: decision === "reject" ? note : ""
    })
  });
}

export async function getAdminNotifications() {
  return apiRequest("/api/admin-notifications", { method: "GET" });
}

export async function createAdminNotification(payload) {
  return apiRequest("/api/admin-notifications", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function markAdminNotificationRead(notificationId) {
  return apiRequest(`/api/admin-notifications/${notificationId}`, {
    method: "PUT"
  });
}

export async function getNotifications() {
  return apiRequest("/api/notifications", { method: "GET" });
}

export async function markNotificationRead(notificationId) {
  return apiRequest(`/api/notifications/${notificationId}`, {
    method: "PUT"
  });
}

export async function getSiteSettings(publicOnly = false) {
  return apiRequest(publicOnly ? "/api/site-settings/public" : "/api/site-settings", {
    method: "GET"
  });
}

export async function updateSiteSetting(key, value) {
  return apiRequest(`/api/site-settings/${encodeURIComponent(key)}`, {
    method: "PUT",
    body: JSON.stringify({ value })
  });
}

export async function getUsers() {
  return apiRequest("/api/users", { method: "GET" });
}

export async function getJudges({ availableOnly = false } = {}) {
  const suffix = availableOnly ? "?availableOnly=true" : "";
  return apiRequest(`/api/users/judges${suffix}`, { method: "GET" });
}

export async function updateJudgeAvailability(availabilityStatus) {
  return apiRequest("/api/users/me/availability", {
    method: "PUT",
    body: JSON.stringify({ availabilityStatus })
  });
}

export async function updateManagedUser(userId, payload) {
  return apiRequest(`/api/users/${userId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function getLawyerNotes(caseId) {
  const suffix = caseId ? `?caseId=${encodeURIComponent(caseId)}` : "";
  return apiRequest(`/api/lawyer-notes${suffix}`, {
    method: "GET"
  });
}

export async function createLawyerNote(payload) {
  return apiRequest("/api/lawyer-notes", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateLawyerNote(noteId, payload) {
  return apiRequest(`/api/lawyer-notes/${noteId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function getDefaultRouteForUser(user) {
  if (!user) {
    return "/";
  }

  if (user.role === "Citizen") {
    return "/citizen-cases";
  }

  if (user.role === "Admin") {
    return "/admin-panel";
  }

  if (user.role === "Judge") {
    return "/judge-approvals";
  }

  if (user.role === "Staff") {
    return "/dashboard";
  }

  return "/dashboard";
}

export function formatDateDisplay(dateValue) {
  if (!dateValue) return "Not scheduled";
  const normalizedValue = dateValue.includes("T") ? dateValue : dateValue.replace(" ", "T");
  const parsedDate = new Date(normalizedValue);
  if (Number.isNaN(parsedDate.getTime())) return dateValue;
  return parsedDate.toLocaleString();
}

export function matchesSearch(caseItem, query) {
  if (!query.trim()) return true;
  const searchText = query.trim().toLowerCase();
  return [caseItem.caseId, caseItem.caseName, caseItem.clientName, caseItem.lawyerName, caseItem.judgeName, caseItem.status]
    .join(" ").toLowerCase().includes(searchText);
}

export function getDateValue(dateValue) {
  if (!dateValue) return null;
  const normalizedValue = dateValue.includes("T") ? dateValue : dateValue.replace(" ", "T");
  const parsedDate = new Date(normalizedValue);
  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
}

export function getNextNumericId(items, key) {
  if (!Array.isArray(items) || !items.length) {
    return 1;
  }

  const highestValue = items.reduce((highest, item) => {
    const currentValue = Number(item?.[key]) || 0;
    return Math.max(highest, currentValue);
  }, 0);

  return highestValue + 1;
}

export async function callClaudeMessages({ system, messages, maxTokens = 1200 }) {
  const apiKey = import.meta.env.VITE_ANTHROPIC_API_KEY;

  if (!apiKey) {
    throw new Error("VITE_ANTHROPIC_API_KEY is not configured.");
  }

  const response = await fetch(CLAUDE_API_URL, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
      "anthropic-dangerous-direct-browser-access": "true"
    },
    body: JSON.stringify({
      model: CLAUDE_MODEL,
      system,
      max_tokens: maxTokens,
      messages
    })
  });

  const data = await parseResponse(response);
  if (!response.ok) {
    const errorMessage = data?.error?.message || data?.message || "Claude request failed.";
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

export function extractClaudeText(responseData) {
  const blocks = Array.isArray(responseData?.content) ? responseData.content : [];
  return blocks
    .filter((block) => block?.type === "text")
    .map((block) => block.text || "")
    .join("\n\n")
    .trim();
}
