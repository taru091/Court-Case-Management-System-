import { Link } from "react-router-dom";

function PublicPortal() {
  return (
    <div className="screen-shell login-shell public-shell">
      <section className="login-hero public-hero">
        <span className="brand-mark large">&#9878;</span>
        <p className="eyebrow">Court Case Management System</p>
        <h1>Secure access to the judiciary workflow platform.</h1>
        <p>
          Citizens, lawyers, judges, and admins can sign in to access case
          records, hearing schedules, document management, and approval workflows.
        </p>
        <div className="hero-points">
          <article>
            <strong>Citizen Access</strong>
            <span>Search and view your case details, hearing schedules, and legal documents.</span>
          </article>
          <article>
            <strong>Lawyer Workflow</strong>
            <span>Manage assigned cases, submit change requests, and track hearings.</span>
          </article>
          <article>
            <strong>Judge &amp; Admin Control</strong>
            <span>Review approvals, configure settings, and manage the full court system.</span>
          </article>
        </div>
      </section>

      <section className="login-panel">
        <div className="panel-card">
          <div className="login-header">
            <h2>Welcome Back</h2>
            <p>Sign in to access your dashboard and case management tools.</p>
          </div>

          <Link to="/login" className="primary-button" style={{ display: "block", textAlign: "center", textDecoration: "none" }}>
            Sign In
          </Link>

          <div style={{ marginTop: "1.5rem", paddingTop: "1.5rem", borderTop: "1px solid var(--line)" }}>
            <div className="login-header">
              <h3>New Here?</h3>
              <p>Create an account to get started with case access and workflow tools.</p>
            </div>
            <Link to="/signup" className="secondary-button" style={{ display: "block", textAlign: "center", textDecoration: "none" }}>
              Create Account
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

export default PublicPortal;
