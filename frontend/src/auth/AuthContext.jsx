import { createContext, useContext, useMemo, useState } from "react";

// Provider pattern: this context holds auth data that many components can share.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem("cms-user");
    return stored ? JSON.parse(stored) : null;
  });

  const value = useMemo(
    () => ({
      user,
      login(email, password) {
        if (email === "admin@cms.com" && password === "admin123") {
          const nextUser = {
            name: "Operations Admin",
            email: "admin@cms.com",
            role: "ADMIN",
          };
          setUser(nextUser);
          localStorage.setItem("cms-user", JSON.stringify(nextUser));
          return true;
        }
        return false;
      },
      logout() {
        localStorage.removeItem("cms-user");
        setUser(null);
      },
    }),
    [user]
  );

  // AuthContext.Provider makes user, login, and logout available to child components.
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  // Custom hook: components call useAuth() instead of using AuthContext directly.
  return useContext(AuthContext);
}
