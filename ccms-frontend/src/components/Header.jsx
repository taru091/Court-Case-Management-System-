function Header({ title, searchValue, onSearchChange, user, onLogout, apiNotice }) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">Court Case Dashboard</p>
        <h2>{title}</h2>
        {apiNotice ? <p className="api-notice">{apiNotice}</p> : null}
      </div>

      <div className="header-actions">
        <label className="search-box">
          <span>Search</span>
          <input
            type="text"
            placeholder="Search cases, clients, judges..."
            value={searchValue}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </label>

        <button type="button" className="notification-button" title="Notifications">
          <span>&#128276;</span>
          <strong>3</strong>
        </button>

        <div className="user-pill">
          <div>
            <strong>{user?.name}</strong>
            <span>{user?.role}</span>
          </div>
        </div>

        <button type="button" className="logout-button" onClick={onLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default Header;
