import { useEffect, useMemo, useRef, useState } from "react";
import ProtectedPageShell from "../components/ProtectedPageShell.jsx";
import {
  apiRequest,
  createLawyerNote,
  getCases,
  getDocumentsByCaseId,
  getFriendlyErrorMessage,
  getLawyerNotes,
  listChangeRequests,
  updateLawyerNote,
  uploadCaseDocument
} from "../api";
import { demoCases, demoDocumentsByCase } from "../demoData";

const initialNoteForm = {
  noteId: null,
  noteType: "PERSONAL_HEARING_NOTE",
  hearingId: "",
  content: ""
};

function Documents({ user, onLogout }) {
  const [cases, setCases] = useState([]);
  const [selectedCaseId, setSelectedCaseId] = useState("");
  const [hearings, setHearings] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [notes, setNotes] = useState([]);
  const [requests, setRequests] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [publicDocument, setPublicDocument] = useState(true);
  const [noteForm, setNoteForm] = useState(initialNoteForm);
  const [loading, setLoading] = useState(true);
  const [usingDemo, setUsingDemo] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const fileInputRef = useRef(null);

  const isAdmin = user?.role === "Admin";
  const isLawyer = user?.role === "Lawyer";

  const approvalMap = useMemo(() => {
    const nextMap = new Map();
    requests
      .filter((request) => request.targetEntityType === "DOCUMENT" && request.targetEntityId)
      .forEach((request) => {
        if (!nextMap.has(request.targetEntityId)) {
          nextMap.set(request.targetEntityId, request);
        }
      });
    return nextMap;
  }, [requests]);

  const loadPage = async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const [caseData, requestData] = await Promise.all([
        getCases(),
        listChangeRequests().catch(() => [])
      ]);
      const nextCases = Array.isArray(caseData) ? caseData : [];
      setCases(nextCases);
      setRequests(Array.isArray(requestData) ? requestData : []);
      setSelectedCaseId((current) => current || nextCases[0]?.caseId || "");
      setUsingDemo(false);
    } catch (error) {
      setCases(demoCases);
      setRequests([]);
      setSelectedCaseId((current) => current || demoCases[0]?.caseId || "");
      setUsingDemo(true);
      setErrorMessage(getFriendlyErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const loadCaseData = async (caseId) => {
    if (!caseId) {
      setDocuments([]);
      setHearings([]);
      setNotes([]);
      return;
    }

    if (usingDemo) {
      setDocuments(demoDocumentsByCase[caseId] || []);
      setHearings([]);
      setNotes([]);
      return;
    }

    try {
      const [documentData, hearingData, noteData] = await Promise.all([
        getDocumentsByCaseId(caseId),
        apiRequest(`/api/hearings/${caseId}`, { method: "GET" }).catch(() => []),
        isLawyer ? getLawyerNotes(caseId).catch(() => []) : Promise.resolve([])
      ]);

      setDocuments(Array.isArray(documentData) ? documentData : []);
      setHearings(Array.isArray(hearingData) ? hearingData : []);
      setNotes(Array.isArray(noteData) ? noteData : []);
    } catch (error) {
      setDocuments(demoDocumentsByCase[caseId] || []);
      setHearings([]);
      setNotes([]);
      setUsingDemo(true);
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  useEffect(() => {
    loadPage();
  }, [isLawyer]);

  useEffect(() => {
    loadCaseData(selectedCaseId);
  }, [selectedCaseId, usingDemo, isLawyer]);

  const handleUpload = async (event) => {
    event.preventDefault();
    if (!selectedFile || !selectedCaseId || usingDemo) {
      return;
    }

    try {
      await uploadCaseDocument({
        caseId: Number(selectedCaseId),
        file: selectedFile,
        publicDocument: isAdmin ? publicDocument : false
      });
      setSelectedFile(null);
      setMessage(
        isAdmin
          ? "Document uploaded live and sent to the judge approval queue."
          : "Document uploaded and sent to admin approval."
      );
      await loadPage();
      await loadCaseData(Number(selectedCaseId));
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  const handleNoteSubmit = async (event) => {
    event.preventDefault();
    if (!isLawyer || usingDemo) {
      return;
    }

    try {
      const payload = {
        caseId: Number(selectedCaseId),
        hearingId: noteForm.hearingId ? Number(noteForm.hearingId) : null,
        noteType: noteForm.noteType,
        content: noteForm.content
      };

      if (noteForm.noteId) {
        await updateLawyerNote(noteForm.noteId, payload);
        setMessage("Note updated and sent to admin approval.");
      } else {
        await createLawyerNote(payload);
        setMessage("Note created and sent to admin approval.");
      }

      setNoteForm(initialNoteForm);
      await loadCaseData(Number(selectedCaseId));
      await loadPage();
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error));
    }
  };

  const selectDocumentFile = (fileList) => {
    const file = Array.isArray(fileList) ? fileList[0] : fileList?.[0];
    if (!file) {
      return;
    }
    setSelectedFile(file);
  };

  const renderDocumentStatus = (document) => {
    const request = approvalMap.get(document.documentId);
    const status = request?.status || document.approvalStatus || "APPROVED";
    return (
      <span className={`workflow-badge ${String(status).toLowerCase()}`}>
        {status}
      </span>
    );
  };

  return (
    <ProtectedPageShell
      heading="Documents"
      note={
        isAdmin
          ? "Admin uploads are live immediately and queued for judge approval. Lawyer uploads remain pending until admins review them."
          : isLawyer
            ? "Upload legal documents and maintain your personal preparation notes. Official approval stays with admins."
            : "Documents are view-only for your role."
      }
      user={user}
      onLogout={onLogout}
    >
      <section className="page-content grid-two">
        <article className="panel form-card">
          <div className="section-header">
            <div>
              <p className="eyebrow">Upload Center</p>
              <h3>{isAdmin || isLawyer ? "Submit Document" : "Read-Only Register"}</h3>
            </div>
          </div>

          {message ? <p className="success-text">{message}</p> : null}
          {errorMessage ? <p className="error-text">{errorMessage}</p> : null}
          {usingDemo ? <div className="fallback-banner compact">Using demo document data because the backend is unavailable.</div> : null}

          {isAdmin || isLawyer ? (
            <form className="stack-form" onSubmit={handleUpload}>
              <label>
                Select Case
                <select value={selectedCaseId} onChange={(event) => setSelectedCaseId(Number(event.target.value))}>
                  {cases.map((caseItem) => (
                    <option key={caseItem.caseId} value={caseItem.caseId}>
                      #{caseItem.caseId} - {caseItem.caseName}
                    </option>
                  ))}
                </select>
              </label>

              {isAdmin ? (
                <label className="checkbox-field">
                  <input
                    type="checkbox"
                    checked={publicDocument}
                    onChange={(event) => setPublicDocument(event.target.checked)}
                  />
                  <span>Mark as public document for citizen portal</span>
                </label>
              ) : null}

              <div
                className={`upload-dropzone ${dragActive ? "drag-active" : ""}`}
                onDragOver={(event) => {
                  event.preventDefault();
                  setDragActive(true);
                }}
                onDragLeave={(event) => {
                  event.preventDefault();
                  setDragActive(false);
                }}
                onDrop={(event) => {
                  event.preventDefault();
                  setDragActive(false);
                  selectDocumentFile(event.dataTransfer.files);
                }}
              >
                <div className="upload-icon-shell">&#8682;</div>
                <strong>Drag and drop PDF, DOCX, or image files here</strong>
                <p>{selectedFile ? selectedFile.name : "Choose a file from your PC or use the quick upload icon."}</p>
                <div className="upload-actions">
                  <button type="button" className="secondary-button" onClick={() => fileInputRef.current?.click()}>
                    Upload from PC
                  </button>
                  <button type="button" className="upload-icon-button" onClick={() => fileInputRef.current?.click()}>
                    &#128228;
                  </button>
                </div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,.docx,.png,.jpg,.jpeg,.gif,.webp"
                  className="sr-only-input"
                  onChange={(event) => selectDocumentFile(event.target.files)}
                />
              </div>

              <button type="submit" className="primary-button" disabled={!selectedFile || usingDemo}>
                {isAdmin ? "Upload Official Document" : "Upload Legal Document"}
              </button>
            </form>
          ) : (
            <div className="read-only-panel">
              <p>Judges, staff, and citizens can review visible document entries here but cannot upload or edit them.</p>
            </div>
          )}
        </article>

        <article className="panel table-card">
          <div className="section-header">
            <div>
              <p className="eyebrow">Document Register</p>
              <h3>Documents for Selected Case</h3>
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

          {loading ? <div className="loading-panel">Loading documents...</div> : null}

          <div className="document-list">
            {documents.map((document) => (
              <article key={document.documentId} className="document-card workflow-document-card">
                <div className="document-card-head">
                  <div>
                    <span>Document #{document.documentId}</span>
                    <strong>{document.fileName || document.fileUrl}</strong>
                  </div>
                  {renderDocumentStatus(document)}
                </div>
                <p>{document.fileUrl}</p>
                {document.rejectionReason ? <p className="rejection-copy">Reason: {document.rejectionReason}</p> : null}
              </article>
            ))}
          </div>
          {!loading && !documents.length ? <p className="empty-state">No document records are available for this case.</p> : null}
        </article>
      </section>

      {isLawyer ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Personal Notes</p>
              <h3>Hearing notes, comments, and preparation notes</h3>
            </div>
          </div>

          <div className="lawyer-notes-grid">
            <form className="stack-form" onSubmit={handleNoteSubmit}>
              <label>
                Note Type
                <select
                  value={noteForm.noteType}
                  onChange={(event) =>
                    setNoteForm((current) => ({
                      ...current,
                      noteType: event.target.value
                    }))
                  }
                >
                  <option value="PERSONAL_HEARING_NOTE">Personal Hearing Note</option>
                  <option value="ADVOCATE_COMMENT">Advocate Comment</option>
                  <option value="PREPARATION_NOTE">Preparation Note</option>
                </select>
              </label>

              <label>
                Related Hearing
                <select
                  value={noteForm.hearingId}
                  onChange={(event) =>
                    setNoteForm((current) => ({
                      ...current,
                      hearingId: event.target.value
                    }))
                  }
                >
                  <option value="">General case note</option>
                  {hearings.map((hearing) => (
                    <option key={hearing.hearingId} value={hearing.hearingId}>
                      #{hearing.hearingId} - {hearing.hearingDate}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Note Content
                <textarea
                  rows="8"
                  value={noteForm.content}
                  onChange={(event) =>
                    setNoteForm((current) => ({
                      ...current,
                      content: event.target.value
                    }))
                  }
                  placeholder="Add your private hearing notes, advocate comments, or preparation notes here."
                  required
                />
              </label>

              <div className="button-row">
                <button type="submit" className="primary-button" disabled={usingDemo}>
                  {noteForm.noteId ? "Update Note" : "Save Note"}
                </button>
                <button type="button" className="secondary-button" onClick={() => setNoteForm(initialNoteForm)}>
                  Clear
                </button>
              </div>
            </form>

            <div className="stack-list">
              {notes.map((note) => (
                <article key={note.noteId} className="notice-card">
                  <div className="document-card-head">
                    <strong>{String(note.noteType || "").replaceAll("_", " ")}</strong>
                    <span className={`workflow-badge ${String(note.approvalStatus || "").toLowerCase()}`}>
                      {note.approvalStatus}
                    </span>
                  </div>
                  <p>{note.content}</p>
                  {note.rejectionReason ? <p className="rejection-copy">Reason: {note.rejectionReason}</p> : null}
                  <div className="button-row">
                    <button
                      type="button"
                      className="table-action"
                      onClick={() =>
                        setNoteForm({
                          noteId: note.noteId,
                          noteType: note.noteType,
                          hearingId: note.hearingId || "",
                          content: note.content
                        })
                      }
                    >
                      Edit
                    </button>
                  </div>
                </article>
              ))}
              {!notes.length ? <p className="empty-state">No personal notes submitted yet.</p> : null}
            </div>
          </div>
        </section>
      ) : null}
    </ProtectedPageShell>
  );
}

export default Documents;
