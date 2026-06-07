import { useEffect, useMemo, useState } from "react";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  formatDateDisplay,
  getCases,
  getFriendlyErrorMessage,
  listChangeRequests,
  reviewChangeRequest,
  updateJudgeAvailability
} from "../api";

const availabilityOptions = ["Available", "Busy", "In Hearing", "On Leave"];

function JudgeApprovals({ user, onLogout }) {
  const [requests, setRequests] = useState([]);
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [availabilityStatus, setAvailabilityStatus] = useState(user?.availabilityStatus || "Available");
  const [rejectingRequest, setRejectingRequest] = useState(null);
  const [rejectionReason, setRejectionReason] = useState("");

  const pendingJudgeRequests = useMemo(
    () => requests.filter((request) => request.status === "PENDING" && request.approvalRole === "Judge"),
    [requests]
  );

  const judgeHearings = useMemo(() => {
    return cases
      .filter((caseItem) => caseItem.judgeUserId === user?.id || caseItem.judgeName === user?.name)
      .filter((caseItem) => caseItem.nextHearingDate)
      .sort((first, second) => String(first.nextHearingDate).localeCompare(String(second.nextHearingDate)));
  }, [cases, user?.id, user?.name]);

  const loadJudgePanel = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const [requestData, caseData] = await Promise.all([
        listChangeRequests(),
        getCases()
      ]);
      setRequests(Array.isArray(requestData) ? requestData : []);
      setCases(Array.isArray(caseData) ? caseData : []);
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadJudgePanel();
  }, []);

  const handleAvailabilitySave = async () => {
    setMessage("");
    setErrorMessage("");

    try {
      await updateJudgeAvailability(availabilityStatus);
      setMessage("Availability updated successfully.");
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  const handleApprove = async (requestId) => {
    setMessage("");
    setErrorMessage("");

    try {
      await reviewChangeRequest(requestId, "approve", "");
      setMessage("Request approved. The live admin change remains in place.");
      await loadJudgePanel();
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
      setMessage("Request rejected. The admin change has been rolled back.");
      setRejectingRequest(null);
      setRejectionReason("");
      await loadJudgePanel();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    }
  };

  return (
    <ProtectedPageShell
      heading="Judge Approval Panel"
      note="Judges approve or reject admin changes, provide rejection reasons, review hearings, and control availability."
      user={user}
      onLogout={onLogout}
    >
      <section className="admin-stats-row">
        <div className="admin-stat-card tone-gold">
          <span className="admin-stat-icon">&#10003;</span>
          <div className="admin-stat-info">
            <strong>{pendingJudgeRequests.length}</strong>
            <span>Pending Reviews</span>
          </div>
        </div>
        <div className="admin-stat-card tone-blue">
          <span className="admin-stat-icon">&#128197;</span>
          <div className="admin-stat-info">
            <strong>{judgeHearings.length}</strong>
            <span>Upcoming Hearings</span>
          </div>
        </div>
        <div className="admin-stat-card tone-green">
          <span className="admin-stat-icon">&#128100;</span>
          <div className="admin-stat-info">
            <strong>{availabilityStatus}</strong>
            <span>Current Availability</span>
          </div>
        </div>
      </section>

      {message ? <div className="admin-success-banner">{message}</div> : null}
      {errorMessage ? <div className="admin-error-banner">{errorMessage}</div> : null}

      <div className="admin-two-col">
        <section className="admin-content-card">
          <div className="admin-section-header">
            <div>
              <p className="eyebrow">Judge Availability</p>
              <h3>Update courtroom status</h3>
              <p className="admin-section-desc">Admins can only assign hearings while your status is Available.</p>
            </div>
          </div>

          <div className="availability-chip-row">
            {availabilityOptions.map((option) => (
              <button
                key={option}
                type="button"
                className={`filter-chip ${availabilityStatus === option ? "active" : ""}`}
                onClick={() => setAvailabilityStatus(option)}
              >
                {option}
              </button>
            ))}
          </div>

          <button type="button" className="primary-button" onClick={handleAvailabilitySave}>
            Save Availability
          </button>
        </section>

        <section className="admin-content-card">
          <div className="admin-section-header">
            <div>
              <p className="eyebrow">Hearing View</p>
              <h3>Your scheduled hearings</h3>
            </div>
          </div>

          <div className="stack-list">
            {judgeHearings.map((caseItem) => (
              <article key={caseItem.caseId} className="highlight-card">
                <strong>{caseItem.caseName}</strong>
                <span>{caseItem.status}</span>
                <p>Client: {caseItem.clientName}</p>
                <p>Hearing: {formatDateDisplay(caseItem.nextHearingDate)}</p>
                <p>Court: {caseItem.nextCourtroom || caseItem.courtDetails || "Courtroom pending"}</p>
              </article>
            ))}
            {!judgeHearings.length && !loading ? <p className="empty-state">No hearings are scheduled for your current assignment.</p> : null}
          </div>
        </section>
      </div>

      <section className="admin-content-card">
        <div className="admin-section-header">
          <div>
            <p className="eyebrow">Judge Queue</p>
            <h3>Pending admin approval requests</h3>
          </div>
          <div className="admin-badge">{pendingJudgeRequests.length} pending</div>
        </div>

        {loading ? <div className="admin-loading-bar"><div className="admin-loading-bar-fill"></div></div> : null}

        <div className="admin-card-stack">
          {pendingJudgeRequests.map((request) => (
            <article key={request.id} className="admin-approval-card">
              <div className="admin-approval-head">
                <div>
                  <strong className="admin-approval-title">#{request.id} - {request.requestTitle}</strong>
                  <p className="admin-approval-meta">
                    {request.targetEntityType} {request.actionType} submitted by {request.requestedByName}
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
          {!loading && !pendingJudgeRequests.length ? (
            <div className="admin-empty-state">
              <span className="admin-empty-icon">&#10003;</span>
              <p>No admin changes are waiting for judge review right now.</p>
            </div>
          ) : null}
        </div>
      </section>

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
              placeholder="Explain why this admin change should be rolled back."
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

export default JudgeApprovals;
