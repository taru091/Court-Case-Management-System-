import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  getDefaultRouteForUser,
  getFriendlyErrorMessage,
  loginUser
} from "../api";

const demoAccounts = [
  { role: "Admin", username: "admin", password: "admin" },
  { role: "Lawyer", username: "lawyer@ccms.com", password: "lawyer123" },
  { role: "Staff", username: "staff@ccms.com", password: "staff123" },
  { role: "Judge", username: "judge@ccms.com", password: "judge123" },
  { role: "Citizen", username: "citizen@ccms.com", password: "citizen123" }
];

function Login({ onLoginSuccess, sessionError }) {
  const navigate = useNavigate();
  const [formState, setFormState] = useState({ username: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormState((current) => ({ ...current, [name]: value }));
  };

  const fillDemoAccount = (account) => {
    setFormState({ username: account.username, password: account.password });
    setErrorMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setErrorMessage("");
    try {
      const response = await loginUser(formState.username, formState.password);
      const loggedInUser = response?.user || null;
      if (!loggedInUser) {
        throw new Error("Login response did not include a user session.");
      }
      onLoginSuccess(loggedInUser);
      navigate(getDefaultRouteForUser(loggedInUser), { replace: true });
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="screen-shell login-shell">
      <section className="login-hero">
        <span className="brand-mark large">&#9878;</span>
        <p className="eyebrow">Secure Access</p>
        <h1>Sign in to the court workflow system.</h1>
        <p>
          Judges, lawyers, staff, and admins can sign in to access protected workflow
          tools, case management, and approval queues. Citizens can sign in with
          their registered phone number or email to search detailed case records.
        </p>
        <div className="hero-points">
          <article>
            <strong>Professional login</strong>
            <span>Judges, lawyers, staff, and admins receive secure session-based access.</span>
          </article>
          <article>
            <strong>Citizen access</strong>
            <span>Registered citizens can sign in using phone or email to search cases in detail.</span>
          </article>
          <article>
            <strong>Safe error handling</strong>
            <span>Backend errors are never exposed. Friendly messages are shown instead.</span>
          </article>
        </div>
      </section>
      <section className="login-panel">
        <div className="panel-card">
          <div className="login-header">
            <h2>Login</h2>
            <p>Use your registered email, phone number, or username with your password.</p>
          </div>
          <div className="demo-login-grid" aria-label="Demo login accounts">
            {demoAccounts.map((account) => (
              <button
                type="button"
                className="demo-login-button"
                key={account.role}
                onClick={() => fillDemoAccount(account)}
              >
                <span>{account.role}</span>
                <small>{account.username}</small>
              </button>
            ))}
          </div>
          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              Email / Phone / Username
              <input type="text" name="username" value={formState.username} onChange={handleChange} placeholder="Enter email, phone, or username" required />
            </label>
            <label>
              Password
              <input type="password" name="password" value={formState.password} onChange={handleChange} placeholder="Enter password" required />
            </label>
            {errorMessage ? <p className="error-text">{errorMessage}</p> : null}
            {sessionError ? <p className="muted-text">{sessionError}</p> : null}
            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? "Signing In..." : "Sign In"}
            </button>
            <div className="auth-link-row">
              <Link to="/forgot-password" className="inline-link">
                Forgot Password?
              </Link>
              <Link to="/signup" className="inline-link">
                Need an account? Register
              </Link>
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}

export default Login;
