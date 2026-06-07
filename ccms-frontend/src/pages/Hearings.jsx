import { useEffect, useMemo, useState } from "react";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  apiRequest,
  formatDateDisplay,
  getCases,
  getFriendlyErrorMessage,
  getJudges,
  listChangeRequests
} from "../api";
import { demoCases, demoHearingsByCase } from "../demoData";

const initialFormState = {
  caseId: "",
  hearingDate: "",
  courtroom: "",
  judgeUserId: ""
};

function Hearings({ user, onLogout }) {
  const [cases, setCases] = useState([]);
  const [judges, setJudges] = useState([]);
  const [requests, setRequests] = useState([]);
  const [selectedCaseId, setSelectedCaseId] = useState("");
  const [hearings, setHearings] = useState([]);
  const [formState, setFormState] = useState(initialFormState);
  const [editingHearingId, setEditingHearingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [usingDemo, setUsingDemo] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const isAdmin = user?.role === "Admin";

  const loadCases = async () => {
    try {
      const [caseData, judgeData, requestData] = await Promise.all([
        getCases(),
        isAdmin ? getJudges({ availableOnly: true }).catch(() => []) : Promise.resolve([]),
        listChangeRequests().catch(() => [])
      ]);

      const nextCases = Array.isArray(caseData) ? caseData : [];
      setCases(nextCases);
      setJudges(Array.isArray(judgeData) ? judgeData : []);
      setRequests(Array.isArray(requestData) ? requestData : []);
      const defaultCaseId = nextCases[0]?.caseId || "";
      setSelectedCaseId((current) => current || defaultCaseId);
      setFormState((current) => ({
        ...current,
        caseId: current.caseId || defaultCaseId
      }));
      setUsingDemo(false);
    } catch (error) {
      setCases(demoCases);
      setJudges([]);
      setRequests([]);
      const defaultCaseId = demoCases[0]?.caseId || "";
      setSelectedCaseId((current) => current || defaultCaseId);
      setFormState((current) => ({
        ...current,
        caseId: current.caseId || defaultCaseId
      }));
      setUsingDemo(true);
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  const loadHearings = async (caseId) => {
    if (!caseId) {
      setHearings([]);
      return;
    }

    if (usingDemo) {
      setHearings(demoHearingsByCase[caseId] || []);
      return;
    }

    try {
      const response = await apiRequest(`/api/hearings/${caseId}`, {
        method: "GET"
      });
      setHearings(Array.isArray(response) ? response : []);
    } catch (error) {
      setHearings(demoHearingsByCase[caseId] || []);
      setUsingDemo(true);
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  useEffect(() => {
    const loadPage = async () => {
      setLoading(true);
      setErrorMessage("");
      await loadCases();
      setLoading(false);
    };

    loadPage();
  }, [isAdmin]);

  useEffect(() => {
    loadHearings(selectedCaseId);
  }, [selectedCaseId, usingDemo]);

  const approvalMap = useMemo(() => {
    const nextMap = new Map();
    requests
      .filter((request) => request.targetEntityType === "HEARING" && request.targetEntityId)
      .forEach((request) => {
        if (!nextMap.has(request.targetEntityId)) {
          nextMap.set(request.targetEntityId, request);
        }
      });
    return nextMap;
  }, [requests]);

  const nextHearing = useMemo(() => {
    return [...hearings].find((hearing) => {
      const date = new Date(String(hearing.hearingDate || "").replace(" ", "T"));
      return !Number.isNaN(date.getTime()) && date >= new Date();
    });
  }, [hearings]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!isAdmin || usingDemo) {
      return;
    }

    try {
      const payload = {
        caseId: Number(formState.caseId),
        hearingDate: formState.hearingDate,
        courtroom: formState.courtroom,
        judgeUserId: Number(formState.judgeUserId)
      };
      const method = editingHearingId ? "PUT" : "POST";
      const endpoint = editingHearingId ? `/api/hearings/${editingHearingId}` : "/api/hearings";

      await apiRequest(endpoint, {
        method,
        body: JSON.stringify(payload)
      });

      setMessage(
        editingHearingId
          ? "Hearing updated live and sent to the judge approval queue."
          : "Hearing scheduled live and sent to the judge approval queue."
      );
      setEditingHearingId(null);
      setFormState((current) => ({
        ...initialFormState,
        caseId: current.caseId
      }));
      await loadCases();
      await loadHearings(Number(formState.caseId));
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  const handleEdit = (hearing) => {
    if (!isAdmin) {
      return;
    }

    setEditingHearingId(hearing.hearingId);
    setFormState({
      caseId: hearing.caseId,
      hearingDate: String(hearing.hearingDate || "").replace(" ", "T"),
      courtroom: hearing.courtroom,
      judgeUserId: hearing.judgeUserId || ""
    });
  };

  const renderApprovalBadge = (hearingId) => {
    const request = approvalMap.get(hearingId);
    if (!request) {
      return <span className="workflow-badge approved">No pending review</span>;
    }

    return (
      <span className={`workflow-badge ${String(request.status || "").toLowerCase()}`}>
        {request.status}
      </span>
    );
  };

  return (
    <ProtectedPageShell
      heading="Hearings"
      note={
        isAdmin
          ? "Only available judges can be assigned to a new hearing, and every admin hearing change goes to judge review."
          : "Hearings are view-only for your role."
      }
      user={user}
      onLogout={onLogout}
    >
      <section className="page-content grid-two">
        <article className="panel form-card">
          <div className="section-header">
            <div>
              <p className="eyebrow">Hearing Controls</p>
              <h3>{isAdmin ? (editingHearingId ? "Update Hearing" : "Schedule Hearing") : "Assigned Hearings"}</h3>
            </div>
          </div>

          {message ? <p className="success-text">{message}</p> : null}
          {errorMessage ? <p className="error-text">{errorMessage}</p> : null}
          {usingDemo ? <div className="fallback-banner compact">Using demo hearing data because the backend is unavailable.</div> : null}

          {isAdmin ? (
            <form className="stack-form" onSubmit={handleSubmit}>
              <label>
                Select Case
                <select
                  value={formState.caseId}
                  onChange={(event) => {
                    const value = Number(event.target.value);
                    setSelectedCaseId(value);
                    setFormState((current) => ({ ...current, caseId: value }));
                  }}
                  required
                >
                  <option value="">Select case</option>
                  {cases.map((caseItem) => (
                    <option key={caseItem.caseId} value={caseItem.caseId}>
                      #{caseItem.caseId} - {caseItem.caseName}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Available Judge
                <select
                  value={formState.judgeUserId}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      judgeUserId: event.target.value
                    }))
                  }
                  required
                >
                  <option value="">Select available judge</option>
                  {judges.map((judge) => (
                    <option key={judge.id} value={judge.id}>
                      {judge.name} ({judge.availabilityStatus})
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Hearing Date
                <input
                  type="datetime-local"
                  value={formState.hearingDate}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      hearingDate: event.target.value
                    }))
                  }
                  required
                />
              </label>

              <label>
                Courtroom
                <input
                  type="text"
                  value={formState.courtroom}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      courtroom: event.target.value
                    }))
                  }
                  required
                />
              </label>

              <div className="button-row">
                <button type="submit" className="primary-button" disabled={usingDemo}>
                  {editingHearingId ? "Update Hearing" : "Schedule Hearing"}
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setEditingHearingId(null);
                    setFormState((current) => ({
                      ...initialFormState,
                      caseId: current.caseId || selectedCaseId
                    }));
                  }}
                >
                  Clear
                </button>
              </div>
            </form>
          ) : (
            <div className="read-only-panel">
              <p>Judges can review hearing schedules and update availability in the judge panel.</p>
              <p>Lawyers and staff can monitor the live schedule here without changing official hearing data.</p>
            </div>
          )}
        </article>

        <article className="panel table-card">
          <div className="section-header">
            <div>
              <p className="eyebrow">Hearing Timeline</p>
              <h3>Hearings by Case</h3>
            </div>

            <label className="toolbar">
              <span>Case</span>
              <select value={selectedCaseId} onChange={(event) => setSelectedCaseId(Number(event.target.value))}>
                {cases.map((caseItem) => (
                  <option key={caseItem.caseId} value={caseItem.caseId}>
                    #{caseItem.caseId} - {caseItem.caseName}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {loading ? <div className="loading-panel">Loading hearings...</div> : null}
          {!loading && nextHearing ? (
            <div className="highlight-card">
              <span>Next Hearing</span>
              <strong>{formatDateDisplay(nextHearing.hearingDate)}</strong>
              <p>{nextHearing.courtroom}</p>
              {nextHearing.judgeName ? <p>Judge: {nextHearing.judgeName}</p> : null}
            </div>
          ) : null}

          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Hearing ID</th>
                  <th>Date</th>
                  <th>Courtroom</th>
                  <th>Judge</th>
                  <th>Review</th>
                  {isAdmin ? <th>Action</th> : null}
                </tr>
              </thead>
              <tbody>
                {hearings.map((hearing) => (
                  <tr key={hearing.hearingId}>
                    <td>#{hearing.hearingId}</td>
                    <td>{formatDateDisplay(hearing.hearingDate)}</td>
                    <td>{hearing.courtroom}</td>
                    <td>{hearing.judgeName || "Judge pending"}</td>
                    <td>{renderApprovalBadge(hearing.hearingId)}</td>
                    {isAdmin ? (
                      <td>
                        <button type="button" className="table-action" onClick={() => handleEdit(hearing)}>
                          Edit
                        </button>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
            {!hearings.length ? <p className="empty-state">No hearings available for this case.</p> : null}
          </div>
        </article>
      </section>
    </ProtectedPageShell>
  );
}

export default Hearings;
