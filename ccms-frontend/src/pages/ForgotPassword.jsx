import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  getFriendlyErrorMessage,
  requestPasswordOtp,
  resetPasswordWithOtp
} from "../api";

function ForgotPassword() {
  const navigate = useNavigate();
  const [formState, setFormState] = useState({
    email: "",
    otp: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [otpSending, setOtpSending] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormState((currentState) => ({
      ...currentState,
      [name]: value
    }));
  };

  const handleSendOtp = async () => {
    if (!formState.email.trim()) {
      setErrorMessage("Enter your email address first.");
      return;
    }

    setOtpSending(true);
    setErrorMessage("");
    setMessage("");

    try {
      const response = await requestPasswordOtp(formState.email);
      setOtpSent(true);
      setMessage(response?.message || "OTP sent successfully.");
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setOtpSending(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (formState.newPassword !== formState.confirmPassword) {
      setErrorMessage("New password and confirm password must match.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");
    setMessage("");

    try {
      const response = await resetPasswordWithOtp(
        formState.email,
        formState.otp,
        formState.newPassword
      );
      setMessage(response?.message || "Password updated successfully.");
      setTimeout(() => {
        navigate("/login", { replace: true });
      }, 1200);
    } catch (error) {
      setErrorMessage(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="screen-shell login-shell">
      <section className="login-hero">
        <span className="brand-mark large">&#9878;</span>
        <p className="eyebrow">Court Case Management System</p>
        <h1>Reset your password securely with a one-time verification code.</h1>
        <p>
          Request a six-digit OTP on your registered email address, confirm it within
          ten minutes, and set a fresh password for your CCMS account.
        </p>
        <div className="hero-points">
          <article>
            <strong>Email-based verification</strong>
            <span>OTPs are delivered through the configured Gmail SMTP account.</span>
          </article>
          <article>
            <strong>10-minute expiry</strong>
            <span>Each code is temporary and invalid after the expiry window.</span>
          </article>
          <article>
            <strong>Secure password hashing</strong>
            <span>Updated passwords are stored with the backend SHA-256 hashing flow.</span>
          </article>
        </div>
      </section>

      <section className="login-panel">
        <div className="panel-card">
          <div className="login-header">
            <h2>Forgot Password</h2>
            <p>Request an OTP, then enter the code and your new password.</p>
          </div>

          <form className="login-form detail-form" onSubmit={handleSubmit}>
            <label>
              Email Address
              <input
                type="email"
                name="email"
                value={formState.email}
                onChange={handleChange}
                placeholder="name@example.com"
                required
              />
            </label>

            <button
              type="button"
              className="secondary-button"
              onClick={handleSendOtp}
              disabled={otpSending}
            >
              {otpSending ? "Sending OTP..." : "Send OTP"}
            </button>

            <label>
              OTP
              <input
                type="text"
                name="otp"
                value={formState.otp}
                onChange={handleChange}
                placeholder="Enter 6-digit OTP"
                maxLength="6"
                required
              />
            </label>

            <label>
              New Password
              <input
                type="password"
                name="newPassword"
                value={formState.newPassword}
                onChange={handleChange}
                placeholder="Enter new password"
                required
              />
            </label>

            <label>
              Confirm Password
              <input
                type="password"
                name="confirmPassword"
                value={formState.confirmPassword}
                onChange={handleChange}
                placeholder="Confirm new password"
                required
              />
            </label>

            {message ? <p className="success-text">{message}</p> : null}
            {errorMessage ? <p className="error-text">{errorMessage}</p> : null}

            <button type="submit" className="primary-button" disabled={submitting || !otpSent}>
              {submitting ? "Updating Password..." : "Submit"}
            </button>

            <div className="auth-link-row">
              <Link to="/login" className="inline-link">
                Back to Login
              </Link>
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}

export default ForgotPassword;
