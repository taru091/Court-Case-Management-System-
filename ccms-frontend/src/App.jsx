import { useEffect, useState } from "react";
import { BrowserRouter } from "react-router-dom";
import AppRoutes from "./routes.jsx";
import {
  SERVICE_UNAVAILABLE_MESSAGE,
  getFriendlyErrorMessage,
  getSessionUser,
  logoutUser
} from "./api";

function App() {
  const [user, setUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [sessionError, setSessionError] = useState("");

  useEffect(() => {
    let cancelled = false;

    const restoreSession = async () => {
      setAuthLoading(true);

      try {
        const response = await getSessionUser();
        if (!cancelled) {
          setUser(response?.authenticated ? response.user || null : null);
          setSessionError("");
        }
      } catch (error) {
        if (!cancelled) {
          setUser(null);
          setSessionError(getFriendlyErrorMessage(error, SERVICE_UNAVAILABLE_MESSAGE));
        }
      } finally {
        if (!cancelled) {
          setAuthLoading(false);
        }
      }
    };

    restoreSession();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleLoginSuccess = (loggedInUser) => {
    setUser(loggedInUser || null);
    setSessionError("");
    setAuthLoading(false);
  };

  const handleLogout = async () => {
    try {
      await logoutUser();
    } finally {
      setUser(null);
    }
  };

  return (
    <BrowserRouter>
      <AppRoutes
        user={user}
        authLoading={authLoading}
        sessionError={sessionError}
        onLoginSuccess={handleLoginSuccess}
        onLogout={handleLogout}
      />
    </BrowserRouter>
  );
}

export default App;
