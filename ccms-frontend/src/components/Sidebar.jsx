import { NavLink } from "react-router-dom";

function getMenuItems(role) {
  if (role === "Citizen") {
    return [
      { path: "/citizen-cases", label: "Case Access", icon: "\u25EB" },
      { path: "/ai-assistant", label: "AI Assistant", icon: "\uD83E\uDDE0" },
      { path: "/profile", label: "Profile", icon: "\u25C9" }
    ];
  }

  if (role === "Judge") {
    return [
      { path: "/dashboard", label: "Dashboard", icon: "\u25A6" },
      { path: "/hearings", label: "Hearings", icon: "\u25F7" },
      { path: "/judge-approvals", label: "Approvals", icon: "\u25A3" },
      { path: "/ai-assistant", label: "AI Assistant", icon: "\uD83E\uDDE0" },
      { path: "/notifications", label: "Notifications", icon: "\uD83D\uDD14" },
      { path: "/profile", label: "Profile", icon: "\u25C9" }
    ];
  }

  if (role === "Lawyer") {
    return [
      { path: "/dashboard", label: "Dashboard", icon: "\u25A6" },
      { path: "/cases", label: "Cases", icon: "\u25EB" },
      { path: "/hearings", label: "Hearings", icon: "\u25F7" },
      { path: "/documents", label: "Documents", icon: "\u25A4" },
      { path: "/ai-assistant", label: "AI Assistant", icon: "\uD83E\uDDE0" },
      { path: "/notifications", label: "Notifications", icon: "\uD83D\uDD14" },
      { path: "/profile", label: "Profile", icon: "\u25C9" }
    ];
  }

  if (role === "Admin") {
    return [
      { path: "/dashboard", label: "Dashboard", icon: "\u25A6" },
      { path: "/cases", label: "Cases", icon: "\u25EB" },
      { path: "/hearings", label: "Hearings", icon: "\u25F7" },
      { path: "/documents", label: "Documents", icon: "\u25A4" },
      { path: "/ai-assistant", label: "AI Assistant", icon: "\uD83E\uDDE0" },
      { path: "/notifications", label: "Notifications", icon: "\uD83D\uDD14" },
      { path: "/admin-panel", label: "Admin Panel", icon: "\u2699" },
      { path: "/profile", label: "Profile", icon: "\u25C9" }
    ];
  }

  return [
    { path: "/dashboard", label: "Dashboard", icon: "\u25A6" },
    { path: "/cases", label: "Cases", icon: "\u25EB" },
    { path: "/hearings", label: "Hearings", icon: "\u25F7" },
    { path: "/documents", label: "Documents", icon: "\u25A4" },
    { path: "/ai-assistant", label: "AI Assistant", icon: "\uD83E\uDDE0" },
    { path: "/notifications", label: "Notifications", icon: "\uD83D\uDD14" },
    { path: "/profile", label: "Profile", icon: "\u25C9" }
  ];
}

function Sidebar({ user }) {
  const menuItems = getMenuItems(user?.role);

  return (
    <aside className="sidebar">
      <div>
        <div className="sidebar-brand">
          <span className="brand-mark">&#9878;</span>
          <div>
            <h1>CCMS</h1>
            <p>Court Case Management System</p>
          </div>
        </div>

        <nav className="sidebar-menu">
          {menuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? "active" : ""}`
              }
            >
              <span className="sidebar-link-copy">
                <span className="sidebar-icon" aria-hidden="true">
                  {item.icon}
                </span>
                <span>{item.label}</span>
              </span>
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="sidebar-note">
        <span>{user?.role || "Judiciary"}</span>
        <p>
          {user?.role === "Citizen"
            ? "Protected case access for registered citizens."
            : "Secure session-based workflow for court operations."}
        </p>
      </div>
    </aside>
  );
}

export default Sidebar;
