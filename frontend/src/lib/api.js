import axios from "axios";

// Factory-style JavaScript: axios.create builds one reusable API client.
export const api = axios.create({
  baseURL: "http://localhost:8080/api",
});

export function readError(error, fallback) {
  // Optional chaining safely reads the backend error message if it exists.
  return error?.response?.data?.message || fallback;
}
