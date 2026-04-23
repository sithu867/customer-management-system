import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { useCustomersData } from "../hooks/useCustomersData";
import {
  addAddress,
  addMobile,
  createEmptyForm,
  mapCustomerToForm,
  mapFormToPayload,
  removeAddress,
  removeMobile,
  toggleFamilyMember,
  updateAddress,
  updateForm,
  updateMobile,
} from "../lib/customerForm";
import { api, readError } from "../lib/api";

export function CustomerFormPage({ mode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const customerId = location.pathname.split("/")[2];
  const isEdit = mode === "edit";
  const { customers, countries, cities, error, refresh } = useCustomersData();
  const [form, setForm] = useState(createEmptyForm());
  const [feedback, setFeedback] = useState("");

  useEffect(() => {
    if (!isEdit || !customers.length) {
      return;
    }
    const customer = customers.find((item) => String(item.id) === customerId);
    if (customer) {
      setForm(mapCustomerToForm(customer));
    }
  }, [customerId, customers, isEdit]);

  const selectableFamilyMembers = customers.filter((item) => String(item.id) !== customerId);

  async function handleSubmit(event) {
    event.preventDefault();
    setFeedback("");

    try {
      const payload = mapFormToPayload(form);
      if (isEdit) {
        await api.put(`/customers/${customerId}`, payload);
        setFeedback("Customer updated successfully.");
      } else {
        await api.post("/customers", payload);
        setFeedback("Customer created successfully.");
      }
      await refresh();
      setTimeout(() => navigate("/customers"), 800);
    } catch (requestError) {
      setFeedback(readError(requestError, "Failed to save customer."));
    }
  }

  return (
    <AppShell
      actions={
        <button className="ghost-button" onClick={() => navigate("/customers")} type="button">
          Back to List
        </button>
      }
      subtitle={
        isEdit
          ? "Update this customer through a dedicated edit workflow."
          : "Create a new customer through a focused creation flow."
      }
      title={isEdit ? "Edit Customer" : "Create Customer"}
    >
      {feedback ? <div className="banner success">{feedback}</div> : null}
      {error ? <div className="banner error">{error}</div> : null}

      <section className="panel">
        <form className="stack" onSubmit={handleSubmit}>
          <div className="card-grid">
            <div className="section-card">
              <p className="eyebrow">Identity</p>
              <h3>Mandatory Details</h3>
              <div className="field-grid">
                <label>
                  <span>Name</span>
                  <input
                    required
                    type="text"
                    value={form.name}
                    onChange={(event) => updateForm(setForm, "name", event.target.value)}
                  />
                </label>
                <label>
                  <span>Date of Birth</span>
                  <input
                    required
                    type="date"
                    value={form.dateOfBirth}
                    onChange={(event) => updateForm(setForm, "dateOfBirth", event.target.value)}
                  />
                </label>
                <label>
                  <span>NIC Number</span>
                  <input
                    required
                    type="text"
                    value={form.nic}
                    onChange={(event) => updateForm(setForm, "nic", event.target.value)}
                  />
                </label>
              </div>
            </div>

            <div className="section-card">
              <div className="subsection-header">
                <div>
                  <p className="eyebrow">Contact</p>
                  <h3>Mobile Numbers</h3>
                </div>
                <button type="button" onClick={() => addMobile(setForm)}>
                  Add Mobile
                </button>
              </div>
              {form.mobileNumbers.map((mobile, index) => (
                <div className="inline-row" key={`mobile-${index}`}>
                  <input
                    placeholder="07XXXXXXXX"
                    type="text"
                    value={mobile}
                    onChange={(event) => updateMobile(setForm, index, event.target.value)}
                  />
                  <button type="button" onClick={() => removeMobile(setForm, index)}>
                    Remove
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="section-card">
            <div className="subsection-header">
              <div>
                <p className="eyebrow">Residence</p>
                <h3>Addresses</h3>
              </div>
              <button type="button" onClick={() => addAddress(setForm)}>
                Add Address
              </button>
            </div>

            {form.addresses.map((address, index) => {
              const filteredCities = address.countryId
                ? cities.filter((city) => String(city.countryId) === address.countryId)
                : cities;

              return (
                <div className="address-card" key={`address-${index}`}>
                  <div className="field-grid">
                    <label>
                      <span>Address Line 1</span>
                      <input
                        type="text"
                        value={address.addressLine1}
                        onChange={(event) => updateAddress(setForm, index, "addressLine1", event.target.value)}
                      />
                    </label>
                    <label>
                      <span>Address Line 2</span>
                      <input
                        type="text"
                        value={address.addressLine2}
                        onChange={(event) => updateAddress(setForm, index, "addressLine2", event.target.value)}
                      />
                    </label>
                    <label>
                      <span>Country</span>
                      <select
                        value={address.countryId}
                        onChange={(event) => updateAddress(setForm, index, "countryId", event.target.value)}
                      >
                        <option value="">Select country</option>
                        {countries.map((country) => (
                          <option key={country.id} value={country.id}>
                            {country.countryName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      <span>City</span>
                      <select
                        value={address.cityId}
                        onChange={(event) => updateAddress(setForm, index, "cityId", event.target.value)}
                      >
                        <option value="">Select city</option>
                        {filteredCities.map((city) => (
                          <option key={city.id} value={city.id}>
                            {city.cityName}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                  <button type="button" onClick={() => removeAddress(setForm, index)}>
                    Remove Address
                  </button>
                </div>
              );
            })}
          </div>

          <div className="section-card">
            <p className="eyebrow">Relationships</p>
            <h3>Family Members</h3>
            <div className="family-grid">
              {selectableFamilyMembers.map((customer) => (
                <label className="family-option" key={customer.id}>
                  <input
                    checked={form.familyMemberIds.includes(customer.id)}
                    type="checkbox"
                    onChange={() => toggleFamilyMember(setForm, customer.id)}
                  />
                  <span>
                    {customer.name}
                    <small>{customer.nic}</small>
                  </span>
                </label>
              ))}
              {!selectableFamilyMembers.length ? (
                <p className="helper-copy">No other customers available yet.</p>
              ) : null}
            </div>
          </div>

          <div className="form-footer">
            <button className="ghost-button" onClick={() => navigate("/customers")} type="button">
              Cancel
            </button>
            <button className="primary-button" type="submit">
              {isEdit ? "Save Changes" : "Create Customer"}
            </button>
          </div>
        </form>
      </section>
    </AppShell>
  );
}
