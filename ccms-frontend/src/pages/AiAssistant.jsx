import { useEffect, useMemo, useRef, useState } from "react";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  JUDICIARY_ALLOWED_TOPICS,
  JUDICIARY_BLOCKED_TOPICS,
  JUDICIARY_WARNING,
  SERVICE_UNAVAILABLE_MESSAGE,
  getFriendlyErrorMessage,
  getSiteSettings,
  isJudiciaryPrompt,
  requestJudiciaryAiResponse
} from "../api";

const assistantTabs = [
  {
    id: "chat",
    label: "Legal Chatbot",
    badge: "LC",
    title: "Judiciary Q&A command desk",
    description:
      "Ask about Indian court procedures, constitutional issues, filing flow, hearings, and litigation operations.",
    placeholder:
      "Ask about hearings, filing workflow, constitutional remedies, IPC, CrPC, or court administration...",
    workspaceLabel: "Reference notes",
    workspacePlaceholder:
      "Paste hearing notes, draft issues, legal facts, or a short court-management brief for context.",
    defaultPrompt:
      "Provide a judiciary-focused response using Indian legal procedure and court management best practices.",
    instruction:
      "Mode: Legal Chatbot. Answer as an Indian judiciary operations assistant. Give clear procedural guidance and stay strictly within Indian legal and court-management topics.",
    welcome:
      "Judiciary AI is online. Ask about court workflow, procedural steps, hearings, constitution, IPC, CrPC, FIR handling, or legal documentation."
  },
  {
    id: "summary",
    label: "Case Summary Generator",
    badge: "CS",
    title: "Case synopsis and issue framing",
    description:
      "Transform raw matter notes into a concise case brief with facts, issues, procedural stage, and next actions.",
    placeholder:
      "Describe what you need in the summary, such as key facts, issues, procedural posture, relief sought, or next hearing preparation...",
    workspaceLabel: "Case material",
    workspacePlaceholder:
      "Paste the case facts, order extract, pleadings summary, witness notes, or hearing record here.",
    defaultPrompt:
      "Summarize this case with facts, legal issues, procedural stage, risks, and immediate next steps.",
    instruction:
      "Mode: Case Summary Generator. Produce a premium case summary for an Indian court workflow. Highlight facts, issues, procedural stage, likely bottlenecks, and recommended next actions.",
    welcome:
      "Paste case details and I will organize them into a judiciary-ready summary with key facts, legal questions, and next-step guidance."
  },
  {
    id: "fir",
    label: "FIR Analyzer",
    badge: "FA",
    title: "FIR scrutiny and red-flag detection",
    description:
      "Review FIR text for sections invoked, factual clarity, procedural concerns, and drafting weaknesses.",
    placeholder:
      "Ask for FIR scrutiny, section explanation, procedural gaps, contradictions, or investigation concerns...",
    workspaceLabel: "FIR extract",
    workspacePlaceholder:
      "Paste the FIR narrative, relevant sections, station details, timeline, witness statements, or case diary notes.",
    defaultPrompt:
      "Analyze this FIR, identify applicable provisions, inconsistencies, missing details, and procedural concerns.",
    instruction:
      "Mode: FIR Analyzer. Examine the FIR using Indian criminal procedure context. Focus on FIR sufficiency, invoked sections, chronology, evidentiary gaps, and procedural concerns.",
    welcome:
      "Share the FIR text or a factual extract and I will review sections, chronology, evidentiary concerns, and procedural gaps."
  },
  {
    id: "prediction",
    label: "Case Prediction",
    badge: "CP",
    title: "Hearing trend and delay risk outlook",
    description:
      "Estimate likely hearing friction, case momentum, delay drivers, and courtroom readiness from the available facts.",
    placeholder:
      "Ask for likely hearing delay, litigation risks, procedural bottlenecks, or next-stage predictions...",
    workspaceLabel: "Case analytics notes",
    workspacePlaceholder:
      "Paste case background, adjournment history, stage of trial, document backlog, witness issues, or hearing constraints.",
    defaultPrompt:
      "Predict the likely hearing delay, procedural risks, and courtroom readiness for this matter.",
    instruction:
      "Mode: Case Prediction. Provide a cautious, judiciary-oriented forecast for hearing delays, procedural risk, case trajectory, and operational readiness in the Indian legal system.",
    welcome:
      "Provide hearing history or case context and I will estimate delay risks, readiness signals, and the next likely procedural pressure points."
  }
];

const quickActions = [
  {
    label: "Summarize this case",
    tabId: "summary",
    prompt:
      "Summarize this case. Cover the facts, legal issues, procedural stage, relief sought, risks, and immediate next legal steps."
  },
  {
    label: "Predict hearing delay",
    tabId: "prediction",
    prompt:
      "Predict the likely hearing delay for this matter and explain the operational and procedural reasons behind the estimate."
  },
  {
    label: "Analyze FIR",
    tabId: "fir",
    prompt:
      "Analyze this FIR. List applicable sections, inconsistencies, missing factual details, procedural concerns, and recommended follow-up."
  },
  {
    label: "Generate legal advice",
    tabId: "chat",
    prompt:
      "Generate judiciary-focused legal guidance for this matter, including procedural next steps, compliance risks, and hearing preparation advice."
  },
  {
    label: "Explain IPC section",
    tabId: "chat",
    prompt:
      "Explain the relevant IPC section for this matter, including ingredients of the offence, practical interpretation, and courtroom relevance."
  },
  {
    label: "Explain constitutional article",
    tabId: "chat",
    prompt:
      "Explain the relevant constitutional article in the Indian legal context, including its meaning, judicial relevance, and typical use in litigation."
  }
];

function createMessage(role, content) {
  return {
    id: `${role}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    role,
    content,
    time: new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit"
    })
  };
}

function createValueMap(initialValue) {
  return assistantTabs.reduce((accumulator, tab) => {
    accumulator[tab.id] = initialValue;
    return accumulator;
  }, {});
}

function createThreadMap() {
  return assistantTabs.reduce((accumulator, tab) => {
    accumulator[tab.id] = [createMessage("assistant", tab.welcome)];
    return accumulator;
  }, {});
}

function createArrayMap() {
  return assistantTabs.reduce((accumulator, tab) => {
    accumulator[tab.id] = [];
    return accumulator;
  }, {});
}

function isTextDocument(file) {
  const extension = String(file?.name || "")
    .split(".")
    .pop()
    ?.toLowerCase();

  return (
    file?.type?.startsWith("text/") ||
    ["txt", "md", "csv", "json", "xml", "html", "pdf", "doc", "docx"].includes(extension || "")
  );
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Unable to read the selected file."));
    reader.readAsDataURL(file);
  });
}

async function processAttachmentFiles(files) {
  const processedAttachments = [];

  for (const file of files || []) {
    if (file.type?.startsWith("image/")) {
      processedAttachments.push({
        kind: "image",
        name: file.name,
        mimeType: file.type,
        dataUrl: await fileToDataUrl(file)
      });
      continue;
    }

    if (isTextDocument(file)) {
      processedAttachments.push({
        kind: "text",
        name: file.name,
        text: await file.text()
      });
      continue;
    }

    processedAttachments.push({
      kind: "text",
      name: file.name,
      text:
        `Attached file: ${file.name}. ` +
        "This document could not be read as text automatically. The AI will use the filename and context to provide the best possible analysis. Add key excerpts in the reference notes for best results."
    });
  }

  return processedAttachments;
}

function buildPromptPayload(tabConfig, draftPrompt, workspaceNotes, currentThread) {
  const recentConversation = currentThread
    .slice(-6)
    .map((message) => `${message.role === "user" ? "User" : "Assistant"}: ${message.content}`)
    .join("\n");

  return [
    tabConfig.instruction,
    workspaceNotes
      ? `${tabConfig.workspaceLabel}:\n${workspaceNotes.trim()}`
      : "",
    recentConversation ? `Recent Conversation Context:\n${recentConversation}` : "",
    `User Request:\n${draftPrompt.trim() || tabConfig.defaultPrompt}`
  ]
    .filter(Boolean)
    .join("\n\n");
}

function AiAssistant({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState("chat");
  const [threads, setThreads] = useState(createThreadMap);
  const [draftPrompts, setDraftPrompts] = useState(() => createValueMap(""));
  const [workspaceNotes, setWorkspaceNotes] = useState(() => createValueMap(""));
  const [attachmentFiles, setAttachmentFiles] = useState(createArrayMap);
  const [assistantSettings, setAssistantSettings] = useState({
    aiReferenceNote: "",
    aiBehaviorNote: ""
  });
  const [warningMessage, setWarningMessage] = useState("");
  const [apiError, setApiError] = useState("");
  const [sendingTabId, setSendingTabId] = useState("");
  const threadEndRef = useRef(null);
  const promptRef = useRef(null);

  const activeTabConfig = useMemo(
    () => assistantTabs.find((tab) => tab.id === activeTab) || assistantTabs[0],
    [activeTab]
  );
  const activeMessages = threads[activeTab] || [];
  const activePrompt = draftPrompts[activeTab] || "";
  const activeWorkspaceNotes = workspaceNotes[activeTab] || "";
  const activeAttachments = attachmentFiles[activeTab] || [];
  const tabIsSending = sendingTabId === activeTab;

  const insightCards = useMemo(
    () => [
      {
        label: "Judiciary Scope",
        value: "Indian Legal Domain",
        detail: "Constitution, IPC, CrPC, FIR, hearings, legal workflow",
        tone: "gold"
      },
      {
        label: "Guardrail Engine",
        value: "Restricted",
        detail: JUDICIARY_WARNING,
        tone: "blue"
      },
      {
        label: "Active Mode",
        value: activeTabConfig.label,
        detail: activeTabConfig.description,
        tone: "green"
      },
      {
        label: "Vision Support",
        value: "Enabled",
        detail: "Attach images or documents for AI-powered analysis and understanding",
        tone: "purple"
      }
    ],
    [activeTabConfig]
  );

  useEffect(() => {
    threadEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [activeMessages, tabIsSending]);

  useEffect(() => {
    let cancelled = false;

    const loadAssistantSettings = async () => {
      try {
        const response = await getSiteSettings(true);
        if (!Array.isArray(response) || cancelled) {
          return;
        }

        const settingMap = response.reduce((accumulator, item) => {
          accumulator[item.key] = item.value || "";
          return accumulator;
        }, {});

        setAssistantSettings({
          aiReferenceNote: settingMap.aiReferenceNote || "",
          aiBehaviorNote: settingMap.aiBehaviorNote || ""
        });
        setWorkspaceNotes((currentNotes) => {
          const nextNotes = { ...currentNotes };
          assistantTabs.forEach((tab) => {
            if (!nextNotes[tab.id]) {
              nextNotes[tab.id] = settingMap.aiReferenceNote || "";
            }
          });
          return nextNotes;
        });
      } catch {
        if (!cancelled) {
          setAssistantSettings({
            aiReferenceNote: "",
            aiBehaviorNote: ""
          });
        }
      }
    };

    loadAssistantSettings();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleTabChange = (tabId) => {
    setActiveTab(tabId);
    setWarningMessage("");
    setApiError("");
  };

  const handleQuickAction = (action) => {
    setActiveTab(action.tabId);
    setWarningMessage("");
    setApiError("");
    setDraftPrompts((currentDrafts) => ({
      ...currentDrafts,
      [action.tabId]: action.prompt
    }));
    setTimeout(() => {
      promptRef.current?.focus();
    }, 0);
  };

  const handlePromptChange = (event) => {
    const nextValue = event.target.value;
    setDraftPrompts((currentDrafts) => ({
      ...currentDrafts,
      [activeTab]: nextValue
    }));
  };

  const handleWorkspaceChange = (event) => {
    const nextValue = event.target.value;
    setWorkspaceNotes((currentNotes) => ({
      ...currentNotes,
      [activeTab]: nextValue
    }));
  };

  const handleAttachmentChange = (event) => {
    const nextFiles = Array.from(event.target.files || []).slice(0, 3);
    setAttachmentFiles((currentFiles) => ({
      ...currentFiles,
      [activeTab]: nextFiles
    }));
  };

  const handlePromptKeyDown = async (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
      event.preventDefault();
      await handleSubmit(event);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (sendingTabId) {
      return;
    }

    const draftPrompt = activePrompt.trim() || activeTabConfig.defaultPrompt;

    if (!isJudiciaryPrompt(draftPrompt)) {
      setWarningMessage(JUDICIARY_WARNING);
      setApiError("");
      return;
    }

    const userMessage = createMessage("user", draftPrompt);
    const nextThread = [...activeMessages, userMessage];
    const requestPrompt = buildPromptPayload(
      activeTabConfig,
      draftPrompt,
      activeWorkspaceNotes,
      nextThread
    );

    setThreads((currentThreads) => ({
      ...currentThreads,
      [activeTab]: nextThread
    }));
    setDraftPrompts((currentDrafts) => ({
      ...currentDrafts,
      [activeTab]: ""
    }));
    setWarningMessage("");
    setApiError("");
    setSendingTabId(activeTab);

    try {
      const processedAttachments = await processAttachmentFiles(activeAttachments);
      const replyText =
        (await requestJudiciaryAiResponse({
          userPrompt: requestPrompt,
          promptToValidate: draftPrompt,
          attachments: processedAttachments,
          aiBehaviorNote: assistantSettings.aiBehaviorNote
        })) ||
        "No legal response was returned. Please refine the judiciary prompt and try again.";

      setThreads((currentThreads) => ({
        ...currentThreads,
        [activeTab]: [...nextThread, createMessage("assistant", replyText)]
      }));
      setAttachmentFiles((currentFiles) => ({
        ...currentFiles,
        [activeTab]: []
      }));
    } catch (error) {
      setApiError(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setSendingTabId("");
    }
  };

  return (
    <ProtectedPageShell
      heading="AI Assistant"
      note="Domain-restricted judiciary workspace with image and document understanding. Use the right sidebar for reference notes."
      user={user}
      onLogout={onLogout}
    >
      <div className="premium-page">
        <section className="premium-hero premium-ai-hero">
          <div className="premium-hero-copy">
            <p className="eyebrow">Judicial Intelligence Desk</p>
            <h3>Premium AI assistance with image and document understanding, case workflow, FIR review, hearing readiness, and legal analysis.</h3>
            <p>
              Operate inside a restricted Indian judiciary knowledge lane with fast
              legal drafting support, structured summaries, and courtroom-oriented
              procedural guidance.
            </p>

            <div className="premium-action-row">
              {quickActions.map((action) => (
                <button
                  key={action.label}
                  type="button"
                  className="quick-action-button"
                  onClick={() => handleQuickAction(action)}
                >
                  {action.label}
                </button>
              ))}
            </div>
          </div>

          <div className="assistant-spotlight-card">
            <div className="assistant-orb-shell" aria-hidden="true">
              <span className="assistant-orb-ring ring-one" />
              <span className="assistant-orb-ring ring-two" />
              <span className="assistant-orb-core">AI</span>
            </div>

            <div className="assistant-spotlight-copy">
              <p className="eyebrow">Restricted Assistant</p>
              <h4>Judiciary-only response engine</h4>
              <ul className="spotlight-list">
                <li>Blocks unrelated prompts before any API request is sent.</li>
                <li>Optimized for Indian legal procedure, courts, IPC, CrPC, and FIR workflows.</li>
                <li>Supports structured case summaries and hearing intelligence prompts.</li>
              </ul>
            </div>
          </div>
        </section>

        <section className="premium-widget-grid">
          {insightCards.map((card) => (
            <article key={card.label} className={`signal-card tone-${card.tone}`}>
              <span>{card.label}</span>
              <strong>{card.value}</strong>
              <p>{card.detail}</p>
            </article>
          ))}
        </section>

        <section className="ai-workspace-grid">
          <div className="ai-primary-column">
            <article className="content-card premium-panel">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Assistant Modes</p>
                  <h3>{activeTabConfig.title}</h3>
                </div>
                <span className="workspace-badge">{activeTabConfig.badge}</span>
              </div>

              <div className="tab-strip premium-tab-strip">
                {assistantTabs.map((tab) => (
                  <button
                    key={tab.id}
                    type="button"
                    className={`tab-button ${activeTab === tab.id ? "active" : ""}`}
                    onClick={() => handleTabChange(tab.id)}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              <p className="workspace-description">{activeTabConfig.description}</p>
            </article>

            <article className="content-card premium-panel ai-chat-stage">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Live Judiciary Chat</p>
                  <h3>Context-aware legal conversation</h3>
                </div>
                <span className="record-count">{activeMessages.length} message(s)</span>
              </div>

              {warningMessage ? <div className="warning-banner">{warningMessage}</div> : null}
              {apiError ? <div className="error-banner">{apiError}</div> : null}

              <div className="chat-thread premium-chat-thread">
                {activeMessages.map((message) => (
                  <div
                    key={message.id}
                    className={`ai-message-row ${message.role === "user" ? "user" : "assistant"}`}
                  >
                    <div className={`ai-avatar ${message.role === "user" ? "user" : "assistant"}`}>
                      {message.role === "user" ? "YOU" : "AI"}
                    </div>
                    <article
                      className={`chat-bubble ${
                        message.role === "user" ? "user" : "assistant"
                      }`}
                    >
                      <span>
                        {message.role === "user" ? "Court User" : "Judiciary AI"} . {message.time}
                      </span>
                      <p>{message.content}</p>
                    </article>
                  </div>
                ))}

                {tabIsSending ? (
                  <div className="ai-message-row assistant">
                    <div className="ai-avatar assistant">AI</div>
                    <article className="chat-bubble assistant typing-bubble">
                      <span>Judiciary AI . typing</span>
                      <div className="typing-indicator" aria-hidden="true">
                        <span />
                        <span />
                        <span />
                      </div>
                    </article>
                  </div>
                ) : null}

                <div ref={threadEndRef} />
              </div>

              <form className="ai-compose-shell" onSubmit={handleSubmit}>
                <div className="chat-compose">
                  <textarea
                    ref={promptRef}
                    value={activePrompt}
                    onChange={handlePromptChange}
                    onKeyDown={handlePromptKeyDown}
                    placeholder={activeTabConfig.placeholder}
                    rows="4"
                  />

                  <label className="assistant-form-label">
                    Attach image or document for analysis
                    <input
                      type="file"
                      multiple
                      accept="image/*,.txt,.md,.csv,.json,.xml,.html,.pdf,.doc,.docx"
                      onChange={handleAttachmentChange}
                    />
                  </label>

                  {activeAttachments.length ? (
                    <div className="topic-chip-grid">
                      {activeAttachments.map((file) => (
                        <span key={`${file.name}-${file.size}`} className={`topic-chip ${file.type?.startsWith("image/") ? "allowed" : "allowed"}`}>
                          {file.type?.startsWith("image/") ? "\uD83D\uDDBC " : "\uD83D\uDCC4 "}{file.name}
                        </span>
                      ))}
                    </div>
                  ) : null}

                  <div className="compose-toolbar">
                    <span className="compose-hint">
                      {tabIsSending
                        ? "The judiciary assistant is generating a response..."
                        : "Press Ctrl/Cmd + Enter to send"}
                    </span>

                    <button
                      type="submit"
                      className="primary-button"
                      disabled={Boolean(sendingTabId)}
                    >
                      {tabIsSending ? "Sending..." : "Send"}
                    </button>
                  </div>
                </div>
              </form>
            </article>
          </div>

          <div className="ai-secondary-column">
            <article className="content-card premium-panel">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Reference Notes</p>
                  <h3>Right-side legal context</h3>
                </div>
              </div>

              <label className="assistant-form-label">
                {activeTabConfig.workspaceLabel}
                <textarea
                  value={activeWorkspaceNotes}
                  onChange={handleWorkspaceChange}
                  placeholder={activeTabConfig.workspacePlaceholder}
                  rows="10"
                />
              </label>

              <p className="muted-text">
                {assistantSettings.aiReferenceNote ||
                  "Use this sidebar to add notes, timelines, witness details, FIR excerpts, or document text before sending."}
              </p>
            </article>

            <article className="content-card premium-panel premium-dark-panel">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Assistant Playbook</p>
                  <h3>Current mode guidance</h3>
                </div>
              </div>

              <div className="playbook-stack">
                <div className="playbook-card">
                  <span>Mode</span>
                  <strong>{activeTabConfig.label}</strong>
                  <p>{activeTabConfig.description}</p>
                </div>
                <div className="playbook-card">
                  <span>Suggested workflow</span>
                  <p>
                    Paste factual material into the workspace, define the exact legal
                    output in the prompt, then send for a judiciary-only answer.
                  </p>
                </div>
              </div>
            </article>

            <article className="content-card premium-panel">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Allowed Topics</p>
                  <h3>Accepted legal scope</h3>
                </div>
              </div>

              <div className="topic-chip-grid">
                {JUDICIARY_ALLOWED_TOPICS.map((topic) => (
                  <span key={topic} className="topic-chip allowed">
                    {topic}
                  </span>
                ))}
              </div>
            </article>

            <article className="content-card premium-panel">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Blocked Topics</p>
                  <h3>Non-judiciary prompt filter</h3>
                </div>
              </div>

              <div className="topic-chip-grid">
                {JUDICIARY_BLOCKED_TOPICS.map((topic) => (
                  <span key={topic} className="topic-chip blocked">
                    {topic}
                  </span>
                ))}
              </div>
            </article>
          </div>
        </section>
      </div>
    </ProtectedPageShell>
  );
}

export default AiAssistant;
