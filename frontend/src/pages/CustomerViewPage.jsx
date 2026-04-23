import { useLocation, useNavigate } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { useCustomersData } from "../hooks/useCustomersData";

export function CustomerViewPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const customerId = location.pathname.split("/")[2];
  const { customers, error } = useCustomersData();
  const customer = customers.find((item) => String(item.id) === customerId);

  return (
    <AppShell
      actions={
        <>
          <button className="ghost-button" onClick={() => navigate("/customers")} type="button">
            Back
          </button>
          {customer ? (
            <button className="primary-button" onClick={() => navigate(`/customers/${customer.id}/edit`)} type="button">
              Edit Customer
            </button>
          ) : null}
        </>
      }
      subtitle="View a customer in a dedicated profile screen."
      title="Customer Profile"
    >
      {error ? <div className="banner error">{error}</div> : null}
      {!customer ? (
        <section className="panel">
          <p className="empty-state">Customer not found.</p>
        </section>
      ) : (
        <section className="details-grid">
          <article className="panel accent-panel">
            <p className="eyebrow">Profile Snapshot</p>
            <h3>{customer.name}</h3>
            <dl className="detail-list">
              <div>
                <dt>NIC</dt>
                <dd>{customer.nic}</dd>
              </div>
              <div>
                <dt>Date of Birth</dt>
                <dd>{customer.dateOfBirth}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{customer.createdAt || "-"}</dd>
              </div>
              <div>
                <dt>Updated</dt>
                <dd>{customer.updatedAt || "-"}</dd>
              </div>
            </dl>
          </article>

          <article className="panel">
            <h3>Mobile Numbers</h3>
            <ul className="pill-list">
              {customer.mobileNumbers.length ? (
                customer.mobileNumbers.map((mobile) => <li key={mobile}>{mobile}</li>)
              ) : (
                <li>No mobile numbers added.</li>
              )}
            </ul>
          </article>

          <article className="panel">
            <h3>Addresses</h3>
            <div className="mini-stack">
              {customer.addresses.length ? (
                customer.addresses.map((address) => (
                  <div className="mini-card" key={address.id}>
                    <strong>{address.addressLine1 || "Address"}</strong>
                    <span>{address.addressLine2 || "-"}</span>
                    <span>
                      {address.cityName || "-"}, {address.countryName || "-"}
                    </span>
                  </div>
                ))
              ) : (
                <p className="helper-copy">No addresses on file.</p>
              )}
            </div>
          </article>

          <article className="panel">
            <h3>Family Members</h3>
            <div className="mini-stack">
              {customer.familyMembers.length ? (
                customer.familyMembers.map((member) => (
                  <div className="mini-card" key={member.id}>
                    <strong>{member.name}</strong>
                    <span>{member.nic}</span>
                  </div>
                ))
              ) : (
                <p className="helper-copy">No family members linked.</p>
              )}
            </div>
          </article>
        </section>
      )}
    </AppShell>
  );
}
