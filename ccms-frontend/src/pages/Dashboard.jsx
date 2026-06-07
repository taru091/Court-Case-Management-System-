import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar.jsx";
import Navbar from "../components/Navbar.jsx";
import Footer from "../components/Footer.jsx";
import {
  formatDateDisplay,
  getCases,
  getDateValue,
  getFriendlyErrorMessage,
  getJudges,
  getNotifications,
  listChangeRequests,
  markNotificationRead,
  matchesSearch
} from "../api";
import { demoCases, demoDocuments } from "../demoData";

const viewMeta = {
  dashboard: "Dashboard Overview",
  cases: "Case Registry",
  hearings: "Hearing Schedule",
  documents: "Document Register",
  notifications: "Notifications",
  profile: "Profile"
};

function Dashboard({ user, view, onLogout }) {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [cases, setCases] = useState([]);
  const [judges, setJudges] = useState([]);
  const [requests, setRequests] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [usingDemo, setUsingDemo] = useState(false);
  const [serviceMessage, setServiceMessage] = useState("");

  const loadDashboard = async () => {
    setLoading(true);

    try {
      const [caseData, requestData, notificationData, judgeData] = await Promise.all([
        getCases(),
        listChangeRequests().catch(() => []),
        getNotifications().catch(() => []),
        user?.role === "Admin" ? getJudges().catch(() => []) : Promise.resolve([])
      ]);

      setCases(Array.isArray(caseData) ? caseData : []);
      setRequests(Array.isArray(requestData) ? requestData : []);
      setNotifications(Array.isArray(notificationData) ? notificationData : []);
      setJudges(Array.isArray(judgeData) ? judgeData : []);
      setUsingDemo(false);
      setServiceMessage("");
    } catch (error) {
      setCases(demoCases);
      setRequests([]);
      setNotifications([]);
      setJudges([]);
      setUsingDemo(true);
      setServiceMessage(getFriendlyErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, [user?.role]);

  const filteredCases = useMemo(() => {
    return cases.filter((caseItem) => matchesSearch(caseItem, searchQuery));
  }, [cases, searchQuery]);

  const dashboardStats = useMemo(() => {
    const totalCases = cases.length;
    const activeCases = cases.filter((caseItem) => caseItem.status === "Active").length;
    const pendingCases = cases.filter((caseItem) => caseItem.status === "Pending").length;
    const closedCases = cases.filter((caseItem) => caseItem.status === "Closed").length;
    const upcomingHearings = cases.filter((caseItem) => {
      const nextDate = getDateValue(caseItem.nextHearingDate);
      return nextDate && nextDate >= new Date();
    }).length;

    return {
      totalCases,
      activeCases,
      pendingCases,
      closedCases,
      upcomingHearings
    };
  }, [cases]);

  const statusBreakdown = useMemo(() => {
    return [
      { label: "Active", value: dashboardStats.activeCases, accent: "green" },
      { label: "Pending", value: dashboardStats.pendingCases, accent: "orange" },
      { label: "Closed", value: dashboardStats.closedCases, accent: "red" }
    ];
  }, [dashboardStats]);

  const delayAnalytics = useMemo(() => {
    const delayedCases = cases.filter((caseItem) => {
      const nextDate = getDateValue(caseItem.nextHearingDate);
      return caseItem.status !== "Closed" && nextDate && nextDate < new Date();
    });

    const totalDelayDays = delayedCases.reduce((sum, caseItem) => {
      const nextDate = getDateValue(caseItem.nextHearingDate);
      if (!nextDate) {
        return sum;
      }

      const differenceMs = Date.now() - nextDate.getTime();
      return sum + differenceMs / (1000 * 60 * 60 * 24);
    }, 0);

    return {
      delayedCount: delayedCases.length,
      averageDelayDays:
        delayedCases.length > 0 ? totalDelayDays / delayedCases.length : 0
    };
  }, [cases]);

  const judgeWorkload = useMemo(() => {
    const judgeMap = new Map();

    cases.forEach((caseItem) => {
      const currentCount = judgeMap.get(caseItem.judgeName) || 0;
      judgeMap.set(caseItem.judgeName, currentCount + 1);
    });

    return Array.from(judgeMap.entries())
      .map(([judgeName, totalCases]) => ({ judgeName, totalCases }))
      .sort((first, second) => second.totalCases - first.totalCases);
  }, [cases]);

  const upcomingHearings = useMemo(() => {
    return filteredCases
      .filter((caseItem) => getDateValue(caseItem.nextHearingDate))
      .sort((first, second) => {
        const firstTime = getDateValue(first.nextHearingDate)?.getTime() || 0;
        const secondTime = getDateValue(second.nextHearingDate)?.getTime() || 0;
        return firstTime - secondTime;
      });
  }, [filteredCases]);

  const workflowStats = useMemo(() => {
    if (user?.role === "Admin") {
      return {
        label: "Pending Admin Approvals",
        value: requests.filter((request) => request.status === "PENDING" && request.approvalRole === "Admin").length
      };
    }

    if (user?.role === "Judge") {
      return {
        label: "Pending Judge Reviews",
        value: requests.filter((request) => request.status === "PENDING" && request.approvalRole === "Judge").length
      };
    }

    return {
      label: "Pending Submissions",
      value: requests.filter((request) => request.status === "PENDING").length
    };
  }, [requests, user?.role]);

  const notificationCount = notifications.length;

  const handleLogout = async () => {
    await onLogout();
    navigate("/login", { replace: true });
  };

  const handleMarkNotificationRead = async (notificationId) => {
    try {
      await markNotificationRead(notificationId);
      await loadDashboard();
    } catch (error) {
      setServiceMessage(getFriendlyErrorMessage(error));
    }
  };

  const renderDashboardView = () => (
    <>
      <section className="hero-banner">
        <div>
          <p className="eyebrow">Judiciary Analytics</p>
          <h3>Monitor court performance, approval queues, and hearing activity in one place.</h3>
          <p>
            Review case trends, delayed matters, live workflow requests, and judge availability without changing the current CCMS layout.
          </p>
        </div>
      </section>

      <section className="stats-grid">
        <article className="stat-card accent-gold">
          <span>Total Cases</span>
          <strong>{dashboardStats.totalCases}</strong>
        </article>
        <article className="stat-card accent-green">
          <span>Active Cases</span>
          <strong>{dashboardStats.activeCases}</strong>
        </article>
        <article className="stat-card accent-orange">
          <span>Pending Cases</span>
          <strong>{dashboardStats.pendingCases}</strong>
        </article>
        <article className="stat-card accent-red">
          <span>Closed Cases</span>
          <strong>{dashboardStats.closedCases}</strong>
        </article>
        <article className="stat-card accent-blue">
          <span>{workflowStats.label}</span>
          <strong>{workflowStats.value}</strong>
        </article>
      </section>

      <section className="analytics-grid">
        <article className="content-card">
          <div className="section-heading">
            <p className="eyebrow">Case Status Breakdown</p>
            <h3>Current status distribution</h3>
          </div>

          <div className="status-stack">
            {statusBreakdown.map((item) => (
              <div key={item.label} className="status-row">
                <span className={`status-pill ${item.accent}`}>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>
        </article>

        <article className="content-card">
          <div className="section-heading">
            <p className="eyebrow">Case Delay Analytics</p>
            <h3>Overdue hearing monitor</h3>
          </div>

          <div className="dual-metric">
            <div>
              <span>Delayed Cases</span>
              <strong>{delayAnalytics.delayedCount}</strong>
            </div>
            <div>
              <span>Average Delay Days</span>
              <strong>{delayAnalytics.averageDelayDays.toFixed(1)}</strong>
            </div>
          </div>
        </article>

        <article className="content-card">
          <div className="section-heading">
            <p className="eyebrow">Judge Workload</p>
            <h3>Cases assigned per judge</h3>
          </div>

          <div className="workload-list">
            {judgeWorkload.map((item) => (
              <div key={item.judgeName} className="workload-row">
                <div className="workload-copy">
                  <span>{item.judgeName}</span>
                  <strong>{item.totalCases} cases</strong>
                </div>
                <div className="workload-bar">
                  <span
                    style={{
                      width: `${(item.totalCases / Math.max(judgeWorkload[0]?.totalCases || 1, 1)) * 100}%`
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </article>
      </section>

      {user?.role === "Admin" ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Available Judges</p>
              <h3>Live availability widget</h3>
            </div>
            <span className="record-count">{judges.filter((judge) => judge.availabilityStatus === "Available").length} available</span>
          </div>

          <div className="available-judges-grid">
            {judges.map((judge) => (
              <article key={judge.id} className="notice-card">
                <div className="document-card-head">
                  <strong>{judge.name}</strong>
                  <span className={`workflow-badge ${String(judge.availabilityStatus || "").toLowerCase().replace(/\s+/g, "-")}`}>
                    {judge.availabilityStatus}
                  </span>
                </div>
                <p>{judge.email}</p>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      <section className="content-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Recent Case Table</p>
            <h3>Live case records</h3>
          </div>
          <button type="button" className="secondary-button" onClick={loadDashboard}>
            Refresh Data
          </button>
        </div>

        <div className="table-shell">
          <table>
            <thead>
              <tr>
                <th>Case ID</th>
                <th>Name</th>
                <th>Client</th>
                <th>Status</th>
                <th>Judge</th>
                <th>Next Hearing</th>
              </tr>
            </thead>
            <tbody>
              {filteredCases.slice(0, 8).map((caseItem) => (
                <tr key={caseItem.caseId}>
                  <td>#{caseItem.caseId}</td>
                  <td>{caseItem.caseName}</td>
                  <td>{caseItem.clientName}</td>
                  <td>
                    <span className={`status-pill ${caseItem.status.toLowerCase()}`}>
                      {caseItem.status}
                    </span>
                  </td>
                  <td>{caseItem.judgeName}</td>
                  <td>{formatDateDisplay(caseItem.nextHearingDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );

  const renderCasesView = () => (
    <section className="content-card">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Cases</p>
          <h3>Searchable case registry</h3>
        </div>
        <span className="record-count">{filteredCases.length} record(s)</span>
      </div>

      <div className="table-shell">
        <table>
          <thead>
            <tr>
              <th>Case ID</th>
              <th>Case Name</th>
              <th>Client</th>
              <th>Lawyer</th>
              <th>Judge</th>
              <th>Status</th>
              <th>Created At</th>
            </tr>
          </thead>
          <tbody>
            {filteredCases.map((caseItem) => (
              <tr key={caseItem.caseId}>
                <td>#{caseItem.caseId}</td>
                <td>{caseItem.caseName}</td>
                <td>{caseItem.clientName}</td>
                <td>{caseItem.lawyerName}</td>
                <td>{caseItem.judgeName}</td>
                <td>
                  <span className={`status-pill ${caseItem.status.toLowerCase()}`}>
                    {caseItem.status}
                  </span>
                </td>
                <td>{formatDateDisplay(caseItem.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );

  const renderHearingsView = () => (
    <section className="content-card">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Hearings</p>
          <h3>Upcoming hearing calendar</h3>
        </div>
        <span className="record-count">{upcomingHearings.length} hearing(s)</span>
      </div>

      <div className="stack-list">
        {upcomingHearings.map((caseItem) => (
          <article key={caseItem.caseId} className="highlight-card">
            <strong>{caseItem.caseName}</strong>
            <span>{caseItem.clientName}</span>
            <p>Judge: {caseItem.judgeName}</p>
            <p>Next Hearing: {formatDateDisplay(caseItem.nextHearingDate)}</p>
            <p>Court: {caseItem.nextCourtroom || caseItem.courtDetails || "Courtroom pending"}</p>
          </article>
        ))}
      </div>
    </section>
  );

  const renderDocumentsView = () => (
    <section className="content-card">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Documents</p>
          <h3>Case document register</h3>
        </div>
        <span className="record-count">{demoDocuments.length} document(s)</span>
      </div>

      <div className="stack-list">
        {demoDocuments.map((document) => (
          <article key={document.documentId} className="document-card">
            <strong>{document.fileName || `Document #${document.documentId}`}</strong>
            <span>Case #{document.caseId}</span>
            <p>{document.fileUrl}</p>
          </article>
        ))}
      </div>
    </section>
  );

  const renderNotificationsView = () => (
    <section className="content-card">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Notifications</p>
          <h3>Automatic workflow alerts</h3>
        </div>
      </div>

      <div className="stack-list">
        {notifications.map((item) => (
          <article key={item.id} className="notice-card">
            <div className="document-card-head">
              <strong>{item.title}</strong>
              <span className={`workflow-badge ${item.read ? "approved" : "pending"}`}>
                {item.read ? "READ" : "NEW"}
              </span>
            </div>
            <p>{item.message}</p>
            <div className="button-row">
              {!item.read ? (
                <button type="button" className="table-action" onClick={() => handleMarkNotificationRead(item.id)}>
                  Mark Read
                </button>
              ) : null}
              <span className="record-count">{item.createdAt}</span>
            </div>
          </article>
        ))}
        {!notifications.length ? <p className="empty-state">No role-based notifications are available right now.</p> : null}
      </div>
    </section>
  );

  const renderProfileView = () => (
    <section className="content-card profile-grid">
      <div>
        <p className="eyebrow">Profile</p>
        <h3>Session user details</h3>
        <div className="profile-block">
          <span>Name</span>
          <strong>{user?.name || "Admin"}</strong>
        </div>
        <div className="profile-block">
          <span>Role</span>
          <strong>{user?.role || "Admin"}</strong>
        </div>
        <div className="profile-block">
          <span>Availability</span>
          <strong>{user?.availabilityStatus || "Available"}</strong>
        </div>
        <div className="profile-block">
          <span>Email / Username</span>
          <strong>{user?.email || "admin"}</strong>
        </div>
      </div>

      <div>
        <p className="eyebrow">Workflow Notes</p>
        <h3>Role permissions</h3>
        <ul className="plain-list">
          <li>Admins edit live data and every change is queued for judge approval.</li>
          <li>Judges approve admin changes and manage their own availability only.</li>
          <li>Lawyers can submit legal documents and personal notes for admin approval.</li>
          <li>Citizens remain read-only through the public and citizen case views.</li>
        </ul>
      </div>
    </section>
  );

  const renderView = () => {
    if (loading) {
      return (
        <section className="content-card">
          <p>Loading case data from the backend...</p>
        </section>
      );
    }

    switch (view) {
      case "cases":
        return renderCasesView();
      case "hearings":
        return renderHearingsView();
      case "documents":
        return renderDocumentsView();
      case "notifications":
        return renderNotificationsView();
      case "profile":
        return renderProfileView();
      default:
        return renderDashboardView();
    }
  };

  return (
    <div className="app-layout">
      <Sidebar user={user} />

      <div className="page-column">
        <Navbar
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          user={user}
          notificationCount={notificationCount}
          onLogout={handleLogout}
          heading={viewMeta[view]}
          usingDemo={usingDemo}
        />

        <main className="page-content">
          {serviceMessage ? <p className="muted-text">{serviceMessage}</p> : null}
          {renderView()}
        </main>
        <Footer />
      </div>
    </div>
  );
}

export default Dashboard;
