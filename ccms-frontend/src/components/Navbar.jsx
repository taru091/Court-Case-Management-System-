function Navbar({
  searchQuery,
  onSearchChange,
  user,
  notificationCount,
  onLogout,
  heading,
  usingDemo
}) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Court Case Management System</p>
        <h2>{heading}</h2>
        {usingDemo ? (
          <p className="topbar-note">
            Service is temporarily unavailable, so the dashboard is showing demo fallback data.
          </p>
        ) : null}
      </div>

      <div className="topbar-tools">
        <label className="search-field">
          <span>Search</span>
          <input
            type="text"
            placeholder="Search cases, clients, judges..."
            value={searchQuery}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </label>

        <button type="button" className="alert-button" title="Notifications">
          <span>&#128276;</span>
          <strong>{notificationCount}</strong>
        </button>

        <div className="profile-chip">
          <strong>{user?.role || "Admin"}</strong>
          <span>{user?.name || "Admin"}</span>
        </div>

        <button type="button" className="logout-button" onClick={onLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default Navbar;
