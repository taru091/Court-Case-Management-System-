import { useState } from "react";
import { apiRequest } from "../api";
import { SERVICE_UNAVAILABLE_MESSAGE, getFriendlyErrorMessage } from "../api";

const defaultRegisterState = {
  name: "",
  email: "",
  password: "",
  role: "Staff"
};

function AuthPage({ onAuthSuccess, onDemoAccess, apiNotice }) {
  const [mode, setMode] = useState("login");
  const [formState, setFormState] = useState(defaultRegisterState);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormState((currentState) => ({
      ...currentState,
      [name]: value
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");

    try {
      const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
      const payload =
        mode === "login"
          ? { email: formState.email, password: formState.password }
          : formState;

      const response = await apiRequest(endpoint, {
        method: "POST",
        body: JSON.stringify(payload)
      });

      onAuthSuccess(response.user);
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-layout">
      <section className="auth-hero">
        <span className="brand-mark large">&#9878;</span>
        <p className="eyebrow">Court Case Management System</p>
        <h1>Track cases, hearings, documents, and workload in one place.</h1>
        <p>
          A structured courtroom workflow for admins, lawyers, and staff with
          role-based access and analytics.
        </p>

        <div className="feature-strip">
          <article>
            <strong>Total oversight</strong>
            <span>Dashboard cards, case search, and hearing tracking.</span>
          </article>
          <article>
            <strong>Professional workflow</strong>
            <span>Dark blue court theme with clear status indicators.</span>
          </article>
          <article>
            <strong>Safe fallback</strong>
            <span>Demo data remains available when the API is offline.</span>
          </article>
        </div>
      </section>

      <section className="auth-card">
        <div className="auth-toggle">
          <button
            type="button"
            className={mode === "login" ? "active" : ""}
            onClick={() => setMode("login")}
          >
            Login
          </button>
          <button
            type="button"
            className={mode === "register" ? "active" : ""}
            onClick={() => setMode("register")}
          >
            Register
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          {mode === "register" ? (
            <label>
              Full Name
              <input
                type="text"
                name="name"
                placeholder="Enter full name"
                value={formState.name}
                onChange={handleChange}
                required
              />
            </label>
          ) : null}

          <label>
            Email
            <input
              type="email"
              name="email"
              placeholder="name@example.com"
              value={formState.email}
              onChange={handleChange}
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              name="password"
              placeholder="Enter password"
              value={formState.password}
              onChange={handleChange}
              required
            />
          </label>

          {mode === "register" ? (
            <label>
              Role
              <select name="role" value={formState.role} onChange={handleChange}>
                <option value="Admin">Admin</option>
                <option value="Lawyer">Lawyer</option>
                <option value="Staff">Staff</option>
              </select>
            </label>
          ) : null}

          {errorMessage ? <p className="error-text">{errorMessage}</p> : null}
          {apiNotice ? <p className="api-notice static">{apiNotice}</p> : null}

          <button type="submit" className="primary-button" disabled={submitting}>
            {submitting ? "Please wait..." : mode === "login" ? "Login" : "Create Account"}
          </button>

          <button type="button" className="secondary-button" onClick={onDemoAccess}>
            Continue with Demo Data
          </button>
        </form>
      </section>
    </div>
  );
}

export default AuthPage;
