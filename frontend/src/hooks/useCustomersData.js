import { useEffect, useState } from "react";
import { api, readError } from "../lib/api";

// Custom hook: keeps shared customer/country/city loading logic in one place.
export function useCustomersData() {
  const [customers, setCustomers] = useState([]);
  const [countries, setCountries] = useState([]);
  const [cities, setCities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function refresh() {
    setLoading(true);
    setError("");
    try {
      // Promise.all runs the three API requests in parallel for faster loading.
      const [customerResponse, countryResponse, cityResponse] = await Promise.all([
        api.get("/customers"),
        api.get("/countries"),
        api.get("/cities"),
      ]);
      setCustomers(customerResponse.data);
      setCountries(countryResponse.data);
      setCities(cityResponse.data);
    } catch (requestError) {
      setError(readError(requestError, "Failed to load customer data."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    // Load data once when the component using this hook first appears.
    refresh();
  }, []);

  return {
    customers,
    countries,
    cities,
    loading,
    error,
    refresh,
  };
}
