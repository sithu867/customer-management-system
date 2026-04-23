import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { useCustomersData } from "../hooks/useCustomersData";
import { api, readError } from "../lib/api";

export function CustomersPage() {
  const navigate = useNavigate();
  const { customers, loading, error, refresh } = useCustomersData();
  const [search, setSearch] = useState("");
  const [feedback, setFeedback] = useState("");

  const filteredCustomers = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) {
      return customers;
    }
    return customers.filter((customer) =>
      [customer.name, customer.nic, customer.mobileNumbers.join(" ")]
        .join(" ")
        .toLowerCase()
        .includes(keyword)
    );
  }, [customers, search]);

  async function handleDelete(customer) {
    const confirmed = window.confirm(`Delete ${customer.name}?`);
    if (!confirmed) {
      return;
    }

    try {
      await api.delete(`/customers/${customer.id}`);
      setFeedback("Customer deleted successfully.");
      await refresh();
    } catch (requestError) {
      setFeedback(readError(requestError, "Failed to delete customer."));
    }
  }

  return (
    <AppShell
      actions={
        <>
          <button className="ghost-button" onClick={() => navigate("/customers/bulk-upload")} type="button">
            Bulk Upload
          </button>
          <button className="primary-button" onClick={() => navigate("/customers/new")} type="button">
            New Customer
          </button>
        </>
      }
      subtitle="See every customer in a clean table view, then branch into focused actions from one place."
      title="Customer List"
    >
      <div className="hero-strip">
        <div>
          <p className="eyebrow">Daily Overview</p>
          <h3>{customers.length} customer records active in the workspace</h3>
          <p>Use the action column for focused view, edit, and delete flows.</p>
        </div>
      </div>

      {feedback ? <div className="banner success">{feedback}</div> : null}
      {error ? <div className="banner error">{error}</div> : null}

      <section className="panel">
        <div className="panel-header spread">
          <div>
            <h3>All Customers</h3>
            <p>Search by customer name, NIC number, or mobile number.</p>
          </div>
          <label className="search-box">
            <span>Search</span>
            <input
              placeholder="Type to filter customers"
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Date of Birth</th>
                <th>NIC</th>
                <th>Mobiles</th>
                <th>Family</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredCustomers.map((customer) => (
                <tr key={customer.id}>
                  <td><strong>{customer.name}</strong></td>
                  <td>{customer.dateOfBirth}</td>
                  <td>{customer.nic}</td>
                  <td>{customer.mobileNumbers.join(", ") || "-"}</td>
                  <td>{customer.familyMembers.length}</td>
                  <td>
                    <div className="table-actions">
                      <button onClick={() => navigate(`/customers/${customer.id}`)} type="button">View</button>
                      <button onClick={() => navigate(`/customers/${customer.id}/edit`)} type="button">Edit</button>
                      <button className="danger-text" onClick={() => handleDelete(customer)} type="button">
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {!filteredCustomers.length ? (
                <tr>
                  <td className="empty-state" colSpan="6">
                    {loading ? "Loading customers..." : "No customers match your search."}
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
    </AppShell>
  );
}
