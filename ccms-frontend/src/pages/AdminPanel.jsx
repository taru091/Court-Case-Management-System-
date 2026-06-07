import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  createAdminNotification,
  getAdminNotifications,
  getCases,
  getFriendlyErrorMessage,
  getJudges,
  getSiteSettings,
  getUsers,
  listChangeRequests,
  markAdminNotificationRead,
  reviewChangeRequest,
  updateManagedUser,
  updateSiteSetting
} from "../api";

const adminTabs = [
  { id: "overview", label: "Overview", icon: "\u25A6" },
  { id: "approvals", label: "Approvals", icon: "\u2713" },
  { id: "notifications", label: "Notifications", icon: "\uD83D\uDD14" },
  { id: "users", label: "Users", icon: "\u2630" },
  { id: "settings", label: "Settings", icon: "\u2699" }
];

const editableSettingKeys = [
  "publicHomeTitle",
  "publicHomeSummary",
  "publicHomeNotice",
  "dashboardNotice",
  "aiReferenceNote",
  "aiBehaviorNote"
];

function AdminPanel({ user, onLogout }) {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");
  const [requests, setRequests] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [settings, setSettings] = useState({});
  const [users, setUsers] = useState([]);
  const [judges, setJudges] = useState([]);
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [notificationForm, setNotificationForm] = useState({
    title: "",
    message: "",
    category: "AdminUpdate",
    targetRole: "ALL"
  });
  const [rejectingRequest, setRejectingRequest] = useState(null);
  const [rejectionReason, setRejectionReason] = useState("");

  const pendingAdminRequests = useMemo(
    () => requests.filter((request) => request.status === "PENDING" && request.approvalRole === "Admin"),
    [requests]
  );

  const adminRequestHistory = useMemo(
    () => requests.filter((request) => request.requestedByRole === "Admin").slice(0, 8),
    [requests]
  );

  const availableJudges = useMemo(
    () => judges.filter((judge) => judge.availabilityStatus === "Available"),
    [judges]
  );

  const unreadNotifications = useMemo(
    () => notifications.filter((notification) => !notification.read).length,
    [notifications]
  );

  const loadPanel = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const [requestData, notificationData, settingData, userData, judgeData, caseData] = await Promise.all([
        listChangeRequests(),
        getAdminNotifications(),
        getSiteSettings(),
        getUsers(),
        getJudges(),
        getCases()
      ]);

      setRequests(Array.isArray(requestData) ? requestData : []);
      setNotifications(Array.isArray(notificationData) ? notificationData : []);
      setSettings(
        Array.isArray(settingData)
          ? settingData.reduce((accumulator, item) => {
              accumulator[item.key] = item.value || "";
              return accumulator;
            }, {})
          : {}
      );
      setUsers(Array.isArray(userData) ? userData : []);
      setJudges(Array.isArray(judgeData) ? judgeData : []);
      setCases(Array.isArray(caseData) ? caseData : []);
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPanel();
  }, []);

  const handleApprove = async (requestId) => {
    setMessage("");
    setErrorMessage("");

    try {
      await reviewChangeRequest(requestId, "approve", "");
      setMessage("Request approved successfully.");
      await loadPanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const handleReject = async () => {
    if (!rejectingRequest || !rejectionReason.trim()) {
      return;
    }

    setMessage("");
    setErrorMessage("");

    try {
      await reviewChangeRequest(rejectingRequest.id, "reject", rejectionReason.trim());
      setMessage("Request rejected successfully.");
      setRejectingRequest(null);
      setRejectionReason("");
      await loadPanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const handleSettingSave = async (key) => {
    setMessage("");
    setErrorMessage("");

    try {
      await updateSiteSetting(key, settings[key] || "");
      setMessage("Setting saved live and sent to the judge approval queue.");
      await loadPanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const handleCreateNotification = async (event) => {
    event.preventDefault();
    setMessage("");
    setErrorMessage("");

    try {
      await createAdminNotification(notificationForm);
      setNotificationForm({
        title: "",
        message: "",
        category: "AdminUpdate",
        targetRole: "ALL"
      });
      setMessage("Notification published live and sent to the judge approval queue.");
      await loadPanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const handleMarkRead = async (notificationId) => {
    try {
      await markAdminNotificationRead(notificationId);
      await loadPanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const handleManagedUserSave = async (managedUser) => {
    setMessage("");
    setErrorMessage("");

    try {
      await updateManagedUser(managedUser.id, managedUser);
      setMessage("User updated live and sent to the judge approval queue.");
      await loadPanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const updateLocalUser = (userId, patch) => {
    setUsers((currentUsers) =>
      currentUsers.map((item) => (item.id === userId ? { ...item, ...patch } : item))
    );
  };

  const renderOverview = () => (
    <div className="admin-two-col">
      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Workflow Snapshot</p>
            <h3>Live approval overview</h3>
          </div>
        </div>

        <div className="admin-card-stack">
          <article className="admin-approval-card">
            <strong className="admin-approval-title">Judge approval queue</strong>
            <p className="admin-approval-meta">{adminRequestHistory.filter((request) => request.status === "PENDING").length} of your live admin changes are still awaiting judge review.</p>
          </article>
          <article className="admin-approval-card">
            <strong className="admin-approval-title">Admin approval queue</strong>
            <p className="admin-approval-meta">{pendingAdminRequests.length} lawyer submissions are waiting for your final approval.</p>
          </article>
          <article className="admin-approval-card">
            <strong className="admin-approval-title">Case operations</strong>
            <p className="admin-approval-meta">{cases.length} total case records are currently live in the system.</p>
          </article>
        </div>

        <div className="button-row quick-link-row">
          <button type="button" className="secondary-button" onClick={() => navigate("/cases")}>
            Go to Cases
          </button>
          <button type="button" className="secondary-button" onClick={() => navigate("/hearings")}>
            Go to Hearings
          </button>
          <button type="button" className="secondary-button" onClick={() => navigate("/documents")}>
            Go to Documents
          </button>
        </div>
      </section>

      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Available Judges</p>
            <h3>Assignment widget</h3>
            <p className="admin-section-desc">Only judges marked Available can receive new hearing assignments.</p>
          </div>
          <div className="admin-badge">{availableJudges.length} available</div>
        </div>

        <div className="stack-list">
          {judges.map((judge) => (
            <article key={judge.id} className="notice-card">
              <div className="document-card-head">
                <strong>{judge.name}</strong>
                <span className={`workflow-badge ${String(judge.availabilityStatus || "").toLowerCase().replace(/\s+/g, "-")}`}>
                  {judge.availabilityStatus}
                </span>
              </div>
              <p>{judge.email}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  );

  const renderApprovals = () => (
    <div className="admin-two-col">
      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Admin Queue</p>
            <h3>Pending lawyer submissions</h3>
          </div>
          <div className="admin-badge">{pendingAdminRequests.length} pending</div>
        </div>

        <div className="admin-card-stack">
          {pendingAdminRequests.map((request) => (
            <article key={request.id} className="admin-approval-card">
              <div className="admin-approval-head">
                <div>
                  <strong className="admin-approval-title">#{request.id} - {request.requestTitle}</strong>
                  <p className="admin-approval-meta">
                    {request.targetEntityType} {request.actionType} from {request.requestedByName} ({request.requestedByRole})
                  </p>
                </div>
                <span className="workflow-badge pending">{request.status}</span>
              </div>
              <div className="admin-approval-actions">
                <button type="button" className="primary-button" onClick={() => handleApprove(request.id)}>
                  Approve
                </button>
                <button type="button" className="secondary-button" onClick={() => setRejectingRequest(request)}>
                  Reject
                </button>
              </div>
            </article>
          ))}
          {!pendingAdminRequests.length && !loading ? <p className="empty-state">No lawyer submissions are waiting for admin review.</p> : null}
        </div>
      </section>

      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Judge Queue Status</p>
            <h3>Your recent admin changes</h3>
          </div>
        </div>

        <div className="admin-card-stack">
          {adminRequestHistory.map((request) => (
            <article key={request.id} className="admin-approval-card">
              <div className="document-card-head">
                <strong>{request.requestTitle}</strong>
                <span className={`workflow-badge ${String(request.status || "").toLowerCase()}`}>
                  {request.status}
                </span>
              </div>
              <p className="admin-approval-meta">
                {request.targetEntityType} {request.actionType} • Created {request.createdAt}
              </p>
              {request.rejectionReason ? <p className="rejection-copy">Reason: {request.rejectionReason}</p> : null}
            </article>
          ))}
        </div>
      </section>
    </div>
  );

  const renderNotifications = () => (
    <div className="admin-two-col">
      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Create Notification</p>
            <h3>Broadcast update</h3>
          </div>
        </div>

        <form className="admin-form" onSubmit={handleCreateNotification}>
          <label className="admin-form-label">
            Title
            <input
              type="text"
              value={notificationForm.title}
              onChange={(event) => setNotificationForm((current) => ({ ...current, title: event.target.value }))}
              required
            />
          </label>
          <label className="admin-form-label">
            Message
            <textarea
              rows="5"
              value={notificationForm.message}
              onChange={(event) => setNotificationForm((current) => ({ ...current, message: event.target.value }))}
              required
            />
          </label>
          <label className="admin-form-label">
            Target Role
            <select
              value={notificationForm.targetRole}
              onChange={(event) => setNotificationForm((current) => ({ ...current, targetRole: event.target.value }))}
            >
              <option value="ALL">All users</option>
              <option value="Admin">Admin</option>
              <option value="Judge">Judge</option>
              <option value="Lawyer">Lawyer</option>
              <option value="Staff">Staff</option>
              <option value="Citizen">Citizen</option>
            </select>
          </label>
          <button type="submit" className="primary-button">
            Publish Notification
          </button>
        </form>
      </section>

      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Notification Log</p>
            <h3>All notifications</h3>
          </div>
          <div className="admin-badge">{unreadNotifications} unread</div>
        </div>

        <div className="admin-notif-stack">
          {notifications.map((notification) => (
            <article key={notification.id} className={`admin-notif-card ${!notification.read ? "unread" : ""}`}>
              <div className="admin-notif-head">
                <strong>{notification.title}</strong>
                <span className="workflow-badge approved">{notification.targetRole || "ALL"}</span>
              </div>
              <p>{notification.message}</p>
              <div className="admin-notif-footer">
                <span className="admin-notif-date">{notification.createdAt}</span>
                {!notification.read ? (
                  <button type="button" className="admin-mark-read-btn" onClick={() => handleMarkRead(notification.id)}>
                    Mark as Read
                  </button>
                ) : (
                  <span className="admin-read-indicator">Read</span>
                )}
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );

  const renderUsers = () => (
    <section className="admin-content-card">
      <div className="admin-section-header">
        <div>
          <p className="eyebrow">User Management</p>
          <h3>Live user administration</h3>
          <p className="admin-section-desc">Saving a user change updates the database immediately and also creates a judge review request.</p>
        </div>
      </div>

      <div className="admin-card-stack">
        {users.map((managedUser) => (
          <article key={managedUser.id} className="admin-approval-card">
            <div className="admin-approval-head">
              <div>
                <strong className="admin-approval-title">{managedUser.name}</strong>
                <p className="admin-approval-meta">{managedUser.username} • {managedUser.email}</p>
              </div>
              <span className={`workflow-badge ${String(managedUser.role || "").toLowerCase()}`}>{managedUser.role}</span>
            </div>

            <div className="admin-form-grid admin-form-grid-3">
              <label className="admin-form-label">
                Name
                <input
                  type="text"
                  value={managedUser.name}
                  onChange={(event) => updateLocalUser(managedUser.id, { name: event.target.value })}
                />
              </label>
              <label className="admin-form-label">
                Role
                <select
                  value={managedUser.role}
                  onChange={(event) => updateLocalUser(managedUser.id, { role: event.target.value })}
                >
                  <option value="Admin">Admin</option>
                  <option value="Judge">Judge</option>
                  <option value="Lawyer">Lawyer</option>
                  <option value="Staff">Staff</option>
                  <option value="Citizen">Citizen</option>
                </select>
              </label>
              <label className="admin-form-label">
                Approval
                <select
                  value={managedUser.approvalStatus}
                  onChange={(event) => updateLocalUser(managedUser.id, { approvalStatus: event.target.value })}
                >
                  <option value="Approved">Approved</option>
                  <option value="Review Hold">Review Hold</option>
                </select>
              </label>
              <label className="admin-form-label">
                Availability
                <select
                  value={managedUser.availabilityStatus}
                  onChange={(event) => updateLocalUser(managedUser.id, { availabilityStatus: event.target.value })}
                >
                  <option value="Available">Available</option>
                  <option value="Busy">Busy</option>
                  <option value="In Hearing">In Hearing</option>
                  <option value="On Leave">On Leave</option>
                </select>
              </label>
              <label className="admin-form-label">
                Court ID
                <input
                  type="text"
                  value={managedUser.courtId || ""}
                  onChange={(event) => updateLocalUser(managedUser.id, { courtId: event.target.value })}
                />
              </label>
            </div>

            <div className="button-row">
              <button type="button" className="primary-button" onClick={() => handleManagedUserSave(managedUser)}>
                Save User
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );

  const renderSettings = () => (
    <section className="admin-content-card admin-settings-card">
      <div className="admin-section-header">
        <div>
          <p className="eyebrow">Site Settings</p>
          <h3>Portal and AI controls</h3>
        </div>
      </div>

      <div className="admin-setting-grid">
        {editableSettingKeys.map((key) => (
          <div key={key} className="admin-setting-block">
            <label className="admin-setting-label">{key.replace(/([A-Z])/g, " $1").replace(/^./, (value) => value.toUpperCase())}</label>
            <textarea
              rows={4}
              value={settings[key] || ""}
              onChange={(event) =>
                setSettings((current) => ({
                  ...current,
                  [key]: event.target.value
                }))
              }
            />
            <button type="button" className="primary-button" onClick={() => handleSettingSave(key)}>
              Save Setting
            </button>
          </div>
        ))}
      </div>
    </section>
  );

  return (
    <ProtectedPageShell
      heading="Admin Panel"
      note="Manage the live workflow, judge review queue, users, notifications, and portal settings without changing the existing CCMS layout."
      user={user}
      onLogout={onLogout}
    >
      <div className="admin-panel-grid">
        <div className="admin-stats-row">
          <div className="admin-stat-card tone-blue">
            <span className="admin-stat-icon">&#9723;</span>
            <div className="admin-stat-info">
              <strong>{cases.length}</strong>
              <span>Total Cases</span>
            </div>
          </div>
          <div className="admin-stat-card tone-green">
            <span className="admin-stat-icon">&#10003;</span>
            <div className="admin-stat-info">
              <strong>{availableJudges.length}</strong>
              <span>Available Judges</span>
            </div>
          </div>
          <div className="admin-stat-card tone-orange">
            <span className="admin-stat-icon">&#9719;</span>
            <div className="admin-stat-info">
              <strong>{pendingAdminRequests.length}</strong>
              <span>Pending Admin Approvals</span>
            </div>
          </div>
          <div className="admin-stat-card tone-gold">
            <span className="admin-stat-icon">&#9878;</span>
            <div className="admin-stat-info">
              <strong>{adminRequestHistory.filter((request) => request.status === "PENDING").length}</strong>
              <span>Pending Judge Reviews</span>
            </div>
          </div>
          <div className="admin-stat-card tone-red">
            <span className="admin-stat-icon">&#128276;</span>
            <div className="admin-stat-info">
              <strong>{unreadNotifications}</strong>
              <span>Unread Notifications</span>
            </div>
          </div>
        </div>

        <div className="admin-tab-nav">
          {adminTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`admin-tab-btn ${activeTab === tab.id ? "active" : ""}`}
              onClick={() => {
                setActiveTab(tab.id);
                setMessage("");
                setErrorMessage("");
              }}
            >
              <span className="admin-tab-icon">{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </div>

        {message ? <div className="admin-success-banner">{message}</div> : null}
        {errorMessage ? <div className="admin-error-banner">{errorMessage}</div> : null}
        {loading ? <div className="admin-loading-bar"><div className="admin-loading-bar-fill"></div></div> : null}

        {activeTab === "overview" ? renderOverview() : null}
        {activeTab === "approvals" ? renderApprovals() : null}
        {activeTab === "notifications" ? renderNotifications() : null}
        {activeTab === "users" ? renderUsers() : null}
        {activeTab === "settings" ? renderSettings() : null}
      </div>

      {rejectingRequest ? (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="section-header">
              <div>
                <p className="eyebrow">Rejection Reason</p>
                <h3>Reject request #{rejectingRequest.id}</h3>
              </div>
            </div>
            <textarea
              rows="5"
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              placeholder="Explain why this lawyer submission should be rejected."
            />
            <div className="button-row">
              <button type="button" className="primary-button" onClick={handleReject}>
                Confirm Reject
              </button>
              <button
                type="button"
                className="secondary-button"
                onClick={() => {
                  setRejectingRequest(null);
                  setRejectionReason("");
                }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </ProtectedPageShell>
  );
}

export default AdminPanel;
