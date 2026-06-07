function Footer() {
  return (
    <footer className="footer">
      <div className="footer-grid">
        <div>
          <strong>Developer:</strong>
          <span>Tarun Kumar Prajapati</span>
        </div>
        <div>
          <strong>Project:</strong>
          <span>Court Case Management System</span>
        </div>
        <div>
          <strong>Type:</strong>
          <span>College Minor Project</span>
        </div>
        <div>
          <strong>Contact:</strong>
          <span>tarun@example.com</span>
          <span>linkedin.com/in/tarun</span>
        </div>
      </div>

      <p className="footer-copy">
        Copyright © {new Date().getFullYear()} Court Case Management System. All
        rights reserved.
      </p>
    </footer>
  );
}

export default Footer;
