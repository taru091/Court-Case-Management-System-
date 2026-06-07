import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  getDefaultRouteForUser,
  getFriendlyErrorMessage,
  registerUser
} from "../api";

const initialFormState = {
  name: "",
  occupation: "Citizen",
  mobile: "",
  email: "",
  password: "",
  confirmPassword: "",
  barCouncilNumber: "",
  courtId: "",
  aadhaarNumber: "",
  profilePhoto: null
};

const roleOptions = [
  { value: "Citizen", icon: "\uD83D\uDC64", label: "Citizen", desc: "Search cases & access public portal" },
  { value: "Lawyer", icon: "\u2696", label: "Lawyer", desc: "Manage cases & submit change requests" },
  { value: "Judge", icon: "\uD83D\uDCDC", label: "Judge", desc: "Review requests & manage hearings" },
  { value: "Staff", icon: "\uD83D\uDCC1", label: "Staff", desc: "Manage hearings, documents, and dashboard workflows" },
  { value: "Admin", icon: "\u2699", label: "Admin", desc: "Full system control & configuration" }
];

function Signup({ onLoginSuccess }) {
  const navigate = useNavigate();
  const [formState, setFormState] = useState(initialFormState);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const isLawyer = formState.occupation === "Lawyer";
  const isJudge = formState.occupation === "Judge";

  const handleChange = (event) => {
    const { name, value, files, type } = event.target;

    setFormState((currentState) => ({
      ...currentState,
      [name]: type === "file" ? files?.[0] || null : value
    }));
  };

  const handleRoleSelect = (role) => {
    setFormState((current) => ({
      ...current,
      occupation: role,
      barCouncilNumber: role === "Lawyer" ? current.barCouncilNumber : "",
      courtId: role === "Judge" ? current.courtId : ""
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (formState.password !== formState.confirmPassword) {
      setErrorMessage("Password and confirm password must match.");
      return;
    }

    setLoading(true);
    setErrorMessage("");

    try {
      const payload = new FormData();
      payload.append("name", formState.name);
      payload.append("occupation", formState.occupation);
      payload.append("mobile", formState.mobile);
      payload.append("email", formState.email);
      payload.append("password", formState.password);
      payload.append("aadhaarNumber", formState.aadhaarNumber);

      if (isLawyer) {
        payload.append("barCouncilNumber", formState.barCouncilNumber);
      }

      if (isJudge) {
        payload.append("courtId", formState.courtId);
      }

      if (formState.profilePhoto) {
        payload.append("profilePhoto", formState.profilePhoto);
      }

      const response = await registerUser(payload);
      const nextUser = response?.user || null;
      onLoginSuccess(nextUser);
      navigate(getDefaultRouteForUser(nextUser), { replace: true });
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
        <p className="eyebrow">Registration</p>
        <h1>Create your account for controlled court case access.</h1>
        <p>
          Register with verified identity details so the system can support
          controlled case access, protected workflow tools, and approval-ready
          website updates.
        </p>
        <div className="hero-points">
          <article>
            <strong>Role-aware onboarding</strong>
            <span>Citizen, lawyer, judge, and admin-specific fields appear only when needed.</span>
          </article>
          <article>
            <strong>Identity-ready records</strong>
            <span>Mobile, Aadhaar, court ID, and bar council details are stored in the backend.</span>
          </article>
          <article>
            <strong>Photo upload support</strong>
            <span>Optional profile images can be attached during signup.</span>
          </article>
        </div>
      </section>

      <section className="login-panel">
        <div className="panel-card wide">
          <div className="login-header">
            <h2>Sign Up</h2>
            <p>Create an account, then continue into citizen access or protected court workflow tools.</p>
          </div>

          <form className="login-form detail-form" onSubmit={handleSubmit}>
            <div className="detail-grid">
              <label className="span-full">
                Full Name
                <input
                  type="text"
                  name="name"
                  value={formState.name}
                  onChange={handleChange}
                  placeholder="Enter full name"
                  required
                />
              </label>

              <label className="span-full">
                Select Your Role
                <div className="signup-role-selector">
                  {roleOptions.map((role) => (
                    <label
                      key={role.value}
                      className={`signup-role-option ${formState.occupation === role.value ? "selected" : ""}`}
                    >
                      <input
                        type="radio"
                        name="occupation"
                        value={role.value}
                        checked={formState.occupation === role.value}
                        onChange={() => handleRoleSelect(role.value)}
                      />
                      <span className={`signup-role-icon ${role.value.toLowerCase()}`}>{role.icon}</span>
                      <span className="signup-role-name">{role.label}</span>
                      <span className="signup-role-desc">{role.desc}</span>
                    </label>
                  ))}
                </div>
              </label>

              <label>
                Mobile Number
                <input
                  type="tel"
                  name="mobile"
                  value={formState.mobile}
                  onChange={handleChange}
                  placeholder="Enter mobile number"
                  maxLength="15"
                  required
                />
              </label>

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

              <label>
                Password
                <input
                  type="password"
                  name="password"
                  value={formState.password}
                  onChange={handleChange}
                  placeholder="Create password"
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
                  placeholder="Confirm password"
                  required
                />
              </label>

              {isLawyer || isJudge ? (
                <div className="span-full signup-conditional-fields">
                  {isLawyer ? (
                    <label>
                      Bar Council Number
                      <input
                        type="text"
                        name="barCouncilNumber"
                        value={formState.barCouncilNumber}
                        onChange={handleChange}
                        placeholder="Enter bar council number"
                        required={isLawyer}
                      />
                    </label>
                  ) : null}
                  {isJudge ? (
                    <label>
                      Court ID
                      <input
                        type="text"
                        name="courtId"
                        value={formState.courtId}
                        onChange={handleChange}
                        placeholder="Enter court ID"
                        required={isJudge}
                      />
                    </label>
                  ) : null}
                </div>
              ) : null}

              <label className="span-full">
                Aadhaar / National ID Number
                <input
                  type="text"
                  name="aadhaarNumber"
                  value={formState.aadhaarNumber}
                  onChange={handleChange}
                  placeholder="Enter Aadhaar or national ID"
                  maxLength="20"
                  required
                />
              </label>

              <label className="span-full">
                Profile Photo (Optional)
                <input
                  type="file"
                  name="profilePhoto"
                  accept="image/*"
                  onChange={handleChange}
                />
              </label>
            </div>

            {errorMessage ? <p className="error-text">{errorMessage}</p> : null}

            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? "Creating Account..." : "Create Account"}
            </button>

            <div className="auth-link-row">
              <Link to="/login" className="inline-link">
                Already have an account? Login
              </Link>
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}

export default Signup;
