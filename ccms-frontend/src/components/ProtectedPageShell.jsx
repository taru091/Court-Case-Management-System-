import { useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar.jsx";
import Footer from "./Footer.jsx";

function ProtectedPageShell({ heading, eyebrow = "Court Case Management System", note, user, onLogout, children }) {
  const navigate = useNavigate();

  const handleLogout = async () => {
    await onLogout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="app-layout">
      <Sidebar user={user} />

      <div className="page-column">
        <header className="topbar">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h2>{heading}</h2>
            {note ? <p className="topbar-note">{note}</p> : null}
          </div>

          <div className="topbar-tools">
            <div className="profile-chip">
              <strong>{user?.role || "Admin"}</strong>
              <span>{user?.name || "Admin"}</span>
            </div>

            <button type="button" className="logout-button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        <main className="page-content">{children}</main>
        <Footer />
      </div>
    </div>
  );
}

export default ProtectedPageShell;
