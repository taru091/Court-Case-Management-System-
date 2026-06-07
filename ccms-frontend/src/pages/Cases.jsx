import { useEffect, useMemo, useState } from "react";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  apiRequest,
  formatDateDisplay,
  getCases,
  getFriendlyErrorMessage,
  getJudges,
  getUsers,
  listChangeRequests,
  matchesSearch
} from "../api";
import { demoCases } from "../demoData";

const initialFormState = {
  caseName: "",
  clientName: "",
  lawyerUserId: "",
  judgeUserId: "",
  status: "Active",
  courtDetails: ""
};

function Cases({ user, onLogout }) {
  const [statusFilter, setStatusFilter] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");
  const [cases, setCases] = useState([]);
  const [requests, setRequests] = useState([]);
  const [users, setUsers] = useState([]);
  const [judges, setJudges] = useState([]);
  const [formState, setFormState] = useState(initialFormState);
  const [editingCaseId, setEditingCaseId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [usingDemo, setUsingDemo] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const isAdmin = user?.role === "Admin";

  const loadData = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const [caseData, requestData, userData, judgeData] = await Promise.all([
        getCases(),
        listChangeRequests().catch(() => []),
        isAdmin ? getUsers().catch(() => []) : Promise.resolve([]),
        isAdmin ? getJudges().catch(() => []) : Promise.resolve([])
      ]);

      setCases(Array.isArray(caseData) ? caseData : []);
      setRequests(Array.isArray(requestData) ? requestData : []);
      setUsers(Array.isArray(userData) ? userData : []);
      setJudges(Array.isArray(judgeData) ? judgeData : []);
      setUsingDemo(false);
    } catch (error) {
      setCases(demoCases);
      setRequests([]);
      setUsers([]);
      setJudges([]);
      setUsingDemo(true);
      setErrorMessage(getFriendlyErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [isAdmin]);

  const lawyers = useMemo(() => users.filter((item) => item.role === "Lawyer"), [users]);

  const approvalMap = useMemo(() => {
    const nextMap = new Map();
    requests
      .filter((request) => request.targetEntityType === "CASE" && request.targetEntityId)
      .forEach((request) => {
        if (!nextMap.has(request.targetEntityId)) {
          nextMap.set(request.targetEntityId, request);
        }
      });
    return nextMap;
  }, [requests]);

  const filteredCases = useMemo(() => {
    return cases.filter((caseItem) => {
      const matchesStatus = statusFilter === "All" || caseItem.status === statusFilter;
      return matchesStatus && matchesSearch(caseItem, searchQuery);
    });
  }, [cases, searchQuery, statusFilter]);

  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setFormState((currentState) => ({
      ...currentState,
      [name]: value
    }));
  };

  const resetForm = () => {
    setFormState(initialFormState);
    setEditingCaseId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!isAdmin || usingDemo) {
      return;
    }

    const selectedLawyer = lawyers.find((item) => String(item.id) === String(formState.lawyerUserId));
    const selectedJudge = judges.find((item) => String(item.id) === String(formState.judgeUserId));

    if (!selectedLawyer || !selectedJudge) {
      setErrorMessage("Select both a lawyer and a judge before saving the case.");
      return;
    }

    try {
      const payload = {
        caseName: formState.caseName,
        clientName: formState.clientName,
        lawyerName: selectedLawyer.name,
        lawyerUserId: Number(selectedLawyer.id),
        judgeName: selectedJudge.name,
        judgeUserId: Number(selectedJudge.id),
        status: formState.status,
        courtDetails: formState.courtDetails
      };
      const method = editingCaseId ? "PUT" : "POST";
      const endpoint = editingCaseId ? `/api/cases/${editingCaseId}` : "/api/cases";

      await apiRequest(endpoint, {
        method,
        body: JSON.stringify(payload)
      });

      setMessage(
        editingCaseId
          ? "Case updated live and sent to the judge approval queue."
          : "Case created live and sent to the judge approval queue."
      );
      resetForm();
      await loadData();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  const handleEdit = (caseItem) => {
    if (!isAdmin) {
      return;
    }

    setEditingCaseId(caseItem.caseId);
    setFormState({
      caseName: caseItem.caseName,
      clientName: caseItem.clientName,
      lawyerUserId: caseItem.lawyerUserId || "",
      judgeUserId: caseItem.judgeUserId || "",
      status: caseItem.status,
      courtDetails: caseItem.courtDetails || caseItem.nextCourtroom || ""
    });
  };

  const handleDelete = async (caseId) => {
    if (!isAdmin || usingDemo) {
      return;
    }

    try {
      await apiRequest(`/api/cases/${caseId}`, { method: "DELETE" });
      setMessage("Case deleted live and sent to the judge approval queue.");
      await loadData();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  const renderApprovalBadge = (caseId) => {
    const request = approvalMap.get(caseId);
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
      heading="Case Registry"
      note={
        isAdmin
          ? "Admin case changes are saved immediately and routed to the judge approval queue."
          : "Your role has read-only access to official case records."
      }
      user={user}
      onLogout={onLogout}
    >
      <section className="page-content grid-two">
        <article className="panel form-card">
          <div className="section-header">
            <div>
              <p className="eyebrow">Case Controls</p>
              <h3>{isAdmin ? (editingCaseId ? "Update Case" : "Create Case") : "Read-Only Access"}</h3>
            </div>
          </div>

          {message ? <p className="success-text">{message}</p> : null}
          {errorMessage ? <p className="error-text">{errorMessage}</p> : null}
          {usingDemo ? <div className="fallback-banner compact">Using demo case data because the backend is unavailable.</div> : null}

          {isAdmin ? (
            <form className="stack-form" onSubmit={handleSubmit}>
              <label>
                Case Name
                <input type="text" name="caseName" value={formState.caseName} onChange={handleInputChange} required />
              </label>

              <label>
                Client Name
                <input type="text" name="clientName" value={formState.clientName} onChange={handleInputChange} required />
              </label>

              <label>
                Assigned Lawyer
                <select name="lawyerUserId" value={formState.lawyerUserId} onChange={handleInputChange} required>
                  <option value="">Select lawyer</option>
                  {lawyers.map((lawyer) => (
                    <option key={lawyer.id} value={lawyer.id}>
                      {lawyer.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Assigned Judge
                <select name="judgeUserId" value={formState.judgeUserId} onChange={handleInputChange} required>
                  <option value="">Select judge</option>
                  {judges.map((judge) => (
                    <option key={judge.id} value={judge.id}>
                      {judge.name} ({judge.availabilityStatus})
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Status
                <select name="status" value={formState.status} onChange={handleInputChange}>
                  <option value="Active">Active</option>
                  <option value="Pending">Pending</option>
                  <option value="Closed">Closed</option>
                </select>
              </label>

              <label>
                Court Details
                <input
                  type="text"
                  name="courtDetails"
                  value={formState.courtDetails}
                  onChange={handleInputChange}
                  placeholder="District Court Hall A"
                  required
                />
              </label>

              <div className="button-row">
                <button type="submit" className="primary-button" disabled={usingDemo}>
                  {editingCaseId ? "Update Case" : "Create Case"}
                </button>
                <button type="button" className="secondary-button" onClick={resetForm}>
                  Clear
                </button>
              </div>
            </form>
          ) : (
            <div className="read-only-panel">
              <p>Official case details can be viewed here, but only admins can change case assignments or status.</p>
              <p>Lawyers see assigned matters only. Judges and staff can review the live register without editing it.</p>
            </div>
          )}
        </article>

        <article className="panel table-card">
          <div className="section-header">
            <div>
              <p className="eyebrow">Case Records</p>
              <h3>View, Search, and Filter</h3>
            </div>

            <div className="toolbar">
              <label>
                Search
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Search cases, judges, clients..."
                />
              </label>
              <label>
                Status
                <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value="All">All</option>
                  <option value="Active">Active</option>
                  <option value="Pending">Pending</option>
                  <option value="Closed">Closed</option>
                </select>
              </label>
            </div>
          </div>

          {loading ? (
            <div className="loading-panel">Loading case records...</div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Case ID</th>
                    <th>Name</th>
                    <th>Client</th>
                    <th>Lawyer</th>
                    <th>Judge</th>
                    <th>Status</th>
                    <th>Court</th>
                    <th>Review</th>
                    {isAdmin ? <th>Action</th> : null}
                  </tr>
                </thead>
                <tbody>
                  {filteredCases.map((caseItem) => (
                    <tr key={caseItem.caseId}>
                      <td>#{caseItem.caseId}</td>
                      <td>
                        <strong>{caseItem.caseName}</strong>
                        <div className="table-subcopy">{formatDateDisplay(caseItem.createdAt)}</div>
                      </td>
                      <td>{caseItem.clientName}</td>
                      <td>{caseItem.lawyerName}</td>
                      <td>{caseItem.judgeName}</td>
                      <td>
                        <span className={`status-pill status-${String(caseItem.status).toLowerCase()}`}>
                          {caseItem.status}
                        </span>
                      </td>
                      <td>{caseItem.courtDetails || caseItem.nextCourtroom || "Courtroom pending"}</td>
                      <td>{renderApprovalBadge(caseItem.caseId)}</td>
                      {isAdmin ? (
                        <td>
                          <div className="table-action-group">
                            <button type="button" className="table-action" onClick={() => handleEdit(caseItem)}>
                              Edit
                            </button>
                            <button type="button" className="table-action danger" onClick={() => handleDelete(caseItem.caseId)}>
                              Delete
                            </button>
                          </div>
                        </td>
                      ) : null}
                    </tr>
                  ))}
                </tbody>
              </table>
              {!filteredCases.length ? <p className="empty-state">No cases matched the current filters.</p> : null}
            </div>
          )}
        </article>
      </section>
    </ProtectedPageShell>
  );
}

export default Cases;
