import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AppShell({ title, subtitle, actions, children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { label: "Customer List", path: "/customers" },
    { label: "New Customer", path: "/customers/new" },
    { label: "Bulk Upload", path: "/customers/bulk-upload" },
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-card">
          <p className="eyebrow">Orbit Desk</p>
          <h1>Customer Hub</h1>
          <span>Internal operations workspace for records, edits, and bulk intake.</span>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <button
              key={item.path}
              className={`nav-link ${location.pathname === item.path ? "active" : ""}`}
              onClick={() => navigate(item.path)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-user">
          <div>
            <strong>{user?.name}</strong>
            <span>{user?.email}</span>
          </div>
          <button className="ghost-button" onClick={logout} type="button">
            Log Out
          </button>
        </div>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <p className="page-tag">Operations Console</p>
            <h2>{title}</h2>
            <p className="page-subtitle">{subtitle}</p>
          </div>
          <div className="topbar-actions">{actions}</div>
        </header>
        {children}
      </main>
    </div>
  );
}
