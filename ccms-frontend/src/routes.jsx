import { Navigate, Route, Routes } from "react-router-dom";
import { getDefaultRouteForUser } from "./api";
import PublicPortal from "./pages/PublicPortal.jsx";
import Login from "./pages/Login.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import Signup from "./pages/Signup.jsx";
import ForgotPassword from "./pages/ForgotPassword.jsx";
import AiAssistant from "./pages/AiAssistant.jsx";
import Cases from "./pages/Cases.jsx";
import Hearings from "./pages/Hearings.jsx";
import Documents from "./pages/Documents.jsx";
import AdminPanel from "./pages/AdminPanel.jsx";
import JudgeApprovals from "./pages/JudgeApprovals.jsx";
import CitizenCases from "./pages/CitizenCases.jsx";

function LoadingScreen() {
  return (
    <div className="screen-shell">
      <div className="loading-card">
        <span className="brand-mark large">&#9878;</span>
        <h2>Loading CCMS</h2>
        <p>Checking your court session...</p>
      </div>
    </div>
  );
}

function ProtectedRoute({ user, authLoading, allowedRoles, children }) {
  if (authLoading) {
    return <LoadingScreen />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (Array.isArray(allowedRoles) && allowedRoles.length && !allowedRoles.includes(user.role)) {
    return <Navigate to={getDefaultRouteForUser(user)} replace />;
  }

  return children;
}

function PublicRoute({ user, authLoading, children }) {
  if (authLoading) {
    return <LoadingScreen />;
  }

  if (user) {
    return <Navigate to={getDefaultRouteForUser(user)} replace />;
  }

  return children;
}

function AppRoutes({ user, authLoading, sessionError, onLoginSuccess, onLogout }) {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <PublicRoute user={user} authLoading={authLoading}>
            <PublicPortal />
          </PublicRoute>
        }
      />
      <Route
        path="/login"
        element={
          <PublicRoute user={user} authLoading={authLoading}>
            <Login
              onLoginSuccess={onLoginSuccess}
              sessionError={sessionError}
            />
          </PublicRoute>
        }
      />
      <Route
        path="/signup"
        element={
          <PublicRoute user={user} authLoading={authLoading}>
            <Signup onLoginSuccess={onLoginSuccess} />
          </PublicRoute>
        }
      />
      <Route
        path="/forgot-password"
        element={
          <PublicRoute user={user} authLoading={authLoading}>
            <ForgotPassword />
          </PublicRoute>
        }
      />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff"]}>
            <Dashboard user={user} view="dashboard" onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/cases"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff"]}>
            <Cases user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/hearings"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff"]}>
            <Hearings user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/documents"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff"]}>
            <Documents user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/notifications"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff"]}>
            <Dashboard user={user} view="notifications" onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff", "Citizen"]}>
            <Dashboard user={user} view="profile" onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/ai-assistant"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin", "Lawyer", "Judge", "Staff", "Citizen"]}>
            <AiAssistant user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin-panel"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Admin"]}>
            <AdminPanel user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/judge-approvals"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Judge"]}>
            <JudgeApprovals user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/citizen-cases"
        element={
          <ProtectedRoute user={user} authLoading={authLoading} allowedRoles={["Citizen"]}>
            <CitizenCases user={user} onLogout={onLogout} />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default AppRoutes;
