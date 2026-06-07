import { useState } from "react";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  getFriendlyErrorMessage,
  getPublicCaseById
} from "../api";

function CitizenCases({ user, onLogout }) {
  const [caseId, setCaseId] = useState("");
  const [caseDetails, setCaseDetails] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSearch = async (event) => {
    event.preventDefault();
    if (!caseId.trim()) {
      return;
    }

    setLoading(true);
    setErrorMessage("");

    try {
      const response = await getPublicCaseById(caseId.trim());
      setCaseDetails(response || null);
    } catch (error) {
      setCaseDetails(null);
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setLoading(false);
    }
  };

  return (
    <ProtectedPageShell
      heading="Citizen Case Access"
      note="Search by Case ID to view public case status, hearing schedule, assigned judge, court details, and public documents."
      user={user}
      onLogout={onLogout}
    >
      <section className="content-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Case Search</p>
            <h3>Read-only public case details</h3>
          </div>
        </div>

        <form className="stack-form" onSubmit={handleSearch}>
          <label>
            Case ID
            <input
              type="text"
              value={caseId}
              onChange={(event) => setCaseId(event.target.value)}
              placeholder="Enter case ID"
              required
            />
          </label>
          <button type="submit" className="primary-button" disabled={loading}>
            {loading ? "Searching..." : "Search Case"}
          </button>
        </form>

        {errorMessage ? <p className="error-text">{errorMessage}</p> : null}

        {caseDetails ? (
          <div className="stack-list citizen-case-stack">
            <article className="highlight-card">
              <strong>
                #{caseDetails.caseId} - {caseDetails.caseName}
              </strong>
              <span>{caseDetails.status}</span>
              <p>Hearing Date: {caseDetails.hearingDate}</p>
              <p>Judge: {caseDetails.judgeName}</p>
              <p>Court Details: {caseDetails.courtDetails}</p>
            </article>

            <section className="content-card nested-content-card">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Public Documents</p>
                  <h3>Visible document list</h3>
                </div>
              </div>

              <div className="stack-list">
                {(caseDetails.documents || []).map((document) => (
                  <article key={document.documentId} className="document-card">
                    <strong>{document.fileName}</strong>
                    <p>{document.fileUrl}</p>
                  </article>
                ))}
                {!caseDetails.documents?.length ? (
                  <p className="empty-state">No public documents are published for this case.</p>
                ) : null}
              </div>
            </section>
          </div>
        ) : null}
      </section>
    </ProtectedPageShell>
  );
}

export default CitizenCases;
