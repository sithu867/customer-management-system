import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("admin@cms.com");
  const [password, setPassword] = useState("admin123");
  const [error, setError] = useState("");

  useEffect(() => {
    if (user) {
      navigate("/customers", { replace: true });
    }
  }, [navigate, user]);

  function handleSubmit(event) {
    event.preventDefault();
    const success = login(email.trim(), password);
    if (success) {
      navigate(location.state?.from || "/customers", { replace: true });
      return;
    }
    setError("Use admin@cms.com / admin123 to access this demo workspace.");
  }

  return (
    <div className="login-page">
      <div className="login-panel">
        <section className="login-copy">
          <p className="eyebrow">Customer Management System</p>
          <h1>Design-led customer operations for teams who need clarity, not clutter.</h1>
          <p>
            Sign in to a more realistic internal workspace with separate customer list, create,
            view, edit, delete, and bulk upload flows.
          </p>
          <div className="login-badges">
            <span>CRM Desk</span>
            <span>Bulk Excel Import</span>
            <span>Family Linking</span>
          </div>
        </section>

        <form className="login-card" onSubmit={handleSubmit}>
          <p className="eyebrow">Staff Login</p>
          <h2>Welcome back</h2>
          <p className="page-subtitle">Use the demo admin account to enter the workspace.</p>

          <label>
            <span>Email</span>
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
          </label>

          <label>
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>

          {error ? <div className="banner error">{error}</div> : null}

          <button className="primary-button full-width" type="submit">
            Enter Workspace
          </button>

          <div className="credentials-card">
            <strong>Demo Credentials</strong>
            <span>admin@cms.com</span>
            <span>admin123</span>
          </div>
        </form>
      </div>
    </div>
  );
}
