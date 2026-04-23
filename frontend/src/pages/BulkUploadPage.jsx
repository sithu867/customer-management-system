import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { api, readError } from "../lib/api";

export function BulkUploadPage() {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [mode, setMode] = useState("UPSERT");
  const [result, setResult] = useState(null);
  const [job, setJob] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!job || (job.status !== "QUEUED" && job.status !== "PROCESSING")) {
      return undefined;
    }

    const timer = window.setInterval(async () => {
      try {
        const response = await api.get(`/customers/bulk-jobs/${job.jobId}`);
        const nextJob = response.data;
        setJob(nextJob);
        if (nextJob.status === "COMPLETED") {
          setResult(nextJob.result);
          setSubmitting(false);
          window.clearInterval(timer);
        } else if (nextJob.status === "FAILED") {
          setError(nextJob.message || "Bulk upload failed.");
          setSubmitting(false);
          window.clearInterval(timer);
        }
      } catch (requestError) {
        setError(readError(requestError, "Failed to refresh bulk upload status."));
        setSubmitting(false);
        window.clearInterval(timer);
      }
    }, 1500);

    return () => window.clearInterval(timer);
  }, [job]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setResult(null);
    setJob(null);

    if (!file) {
      setError("Choose a .xlsx file before uploading.");
      return;
    }

    try {
      setSubmitting(true);
      const formData = new FormData();
      formData.append("file", file);
      formData.append("mode", mode);
      const response = await api.post("/customers/bulk-jobs", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setJob(response.data);
    } catch (requestError) {
      setError(readError(requestError, "Bulk upload failed."));
      setSubmitting(false);
    }
  }

  return (
    <AppShell
      actions={
        <button className="ghost-button" onClick={() => navigate("/customers")} type="button">
          Back to List
        </button>
      }
      subtitle="Upload customer batches from Excel without crowding the day-to-day customer list screen."
      title="Bulk Upload"
    >
      {error ? <div className="banner error">{error}</div> : null}
      {job ? (
        <div className={`banner ${job.status === "FAILED" ? "error" : "success"}`}>
          {job.message}
        </div>
      ) : null}

      <section className="panel upload-panel">
        <p className="eyebrow">Excel Intake</p>
        <h3>Process a customer batch through a dedicated upload flow.</h3>
        <p className="page-subtitle">Expected columns: Name, Date of Birth, NIC Number.</p>
        <p className="helper-copy">
          Large files are queued and processed in the background so the browser is not stuck waiting
          on one long request.
        </p>

        <form className="stack" onSubmit={handleSubmit}>
          <div className="field-grid">
            <label>
              <span>Import Mode</span>
              <select value={mode} onChange={(event) => setMode(event.target.value)}>
                <option value="UPSERT">Upsert existing customers</option>
                <option value="CREATE_ONLY">Create only</option>
              </select>
            </label>

            <label>
              <span>Excel File</span>
              <input accept=".xlsx" type="file" onChange={(event) => setFile(event.target.files?.[0] || null)} />
            </label>
          </div>

          <div className="form-footer">
            <button className="primary-button" disabled={submitting} type="submit">
              {submitting ? "Queueing Upload..." : "Upload Batch"}
            </button>
          </div>
        </form>
      </section>

      {result ? (
        <section className="panel">
          <div className="panel-header spread">
            <div>
              <h3>Import Summary</h3>
              <p>Job ID: {job?.jobId}</p>
            </div>
            <span className="page-tag">{job?.status}</span>
          </div>
          <div className="stats-grid">
            <article><strong>{result.totalRows}</strong><span>Total Rows</span></article>
            <article><strong>{result.createdCount}</strong><span>Created</span></article>
            <article><strong>{result.updatedCount}</strong><span>Updated</span></article>
            <article><strong>{result.skippedCount}</strong><span>Skipped</span></article>
            <article><strong>{result.invalidCount}</strong><span>Invalid</span></article>
          </div>

          {result.sampleErrors?.length ? (
            <div className="error-list">
              <h3>Sample Errors</h3>
              <ul>
                {result.sampleErrors.map((item, index) => (
                  <li key={`bulk-error-${index}`}>
                    Row {item.rowNumber}: {item.message}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </section>
      ) : null}
    </AppShell>
  );
}
