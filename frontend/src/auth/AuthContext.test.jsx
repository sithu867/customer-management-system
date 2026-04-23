import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useAuth, AuthProvider } from "./AuthContext";

function Harness() {
  const { user, login, logout } = useAuth();

  return (
    <div>
      <span data-testid="user-email">{user?.email || "anonymous"}</span>
      <button onClick={() => login("admin@cms.com", "admin123")} type="button">
        Login
      </button>
      <button onClick={logout} type="button">
        Logout
      </button>
    </div>
  );
}

describe("AuthProvider", () => {
  it("logs in with the demo credentials and persists the user", async () => {
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <Harness />
      </AuthProvider>
    );

    await user.click(screen.getByRole("button", { name: "Login" }));

    expect(screen.getByTestId("user-email")).toHaveTextContent("admin@cms.com");
    expect(JSON.parse(window.localStorage.getItem("cms-user"))).toMatchObject({
      email: "admin@cms.com",
      role: "ADMIN",
    });
  });

  it("clears the stored user on logout", async () => {
    window.localStorage.setItem(
      "cms-user",
      JSON.stringify({ name: "Operations Admin", email: "admin@cms.com", role: "ADMIN" })
    );
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <Harness />
      </AuthProvider>
    );

    await user.click(screen.getByRole("button", { name: "Logout" }));

    expect(screen.getByTestId("user-email")).toHaveTextContent("anonymous");
    expect(window.localStorage.getItem("cms-user")).toBeNull();
  });
});
