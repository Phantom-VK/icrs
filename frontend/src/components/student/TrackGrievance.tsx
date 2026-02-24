import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import grievanceService from "../../services/grievanceService";
import type { Comment } from "../../services/grievanceService";

interface Grievance {
  id: number;
  title: string;
  description: string;
  category: string;
  subcategory: string;
  registrationNumber: string;
  status: "SUBMITTED" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";
}

const TrackGrievance: React.FC = () => {
  const navigate = useNavigate();
  const [grievances, setGrievances] = useState<Grievance[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [filterStatus, setFilterStatus] = useState<
    "All" | "SUBMITTED" | "IN_PROGRESS" | "RESOLVED" | "REJECTED"
  >("All");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [commentMap, setCommentMap] = useState<Record<number, Comment[]>>({});
  const [commentLoadingMap, setCommentLoadingMap] = useState<Record<number, boolean>>({});
  const [commentErrorMap, setCommentErrorMap] = useState<Record<number, string>>({});

  useEffect(() => {
    const fetchGrievances = async () => {
      try {
        console.log("Fetching grievances for logged-in student...");
        const data = await grievanceService.getMyGrievances();
        setGrievances(data || []);
        console.log("Fetched grievances:", data);
      } catch (err) {
        console.error("Failed to load grievances:", err);
        setError("Failed to load grievances. Please try again.");
      } finally {
        setLoading(false);
      }
    };
    fetchGrievances();
  }, []);

  const filteredGrievances = grievances.filter((g) => {
    const matchesSearch =
      g.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.registrationNumber.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === "All" || g.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const statusColors: Record<string, string> = {
    SUBMITTED: "black",
    IN_PROGRESS: "#007bff",
    RESOLVED: "green",
    REJECTED: "red",
  };

  return (
    <div
      style={{
        width: "100vw",
        minHeight: "100vh",
        backgroundColor: "#e0f7fa",
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start",
        paddingTop: "50px",
        fontFamily: "Arial, sans-serif",
        color: "#000",
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: "1050px",
          backgroundColor: "#fff",
          borderRadius: "10px",
          padding: "30px",
          boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
        }}
      >
        {/* Back to Portal */}
        <button
          onClick={() => navigate("/auth/student-dashboard")}
          style={{
            padding: "6px 14px",
            cursor: "pointer",
            color: "#000",
            backgroundColor: "#f5f5f5",
            border: "none",
            borderRadius: "6px",
            marginBottom: "20px",
          }}
        >
          &larr; Back to Portal
        </button>

        {/* Header */}
        <h2 style={{ textAlign: "center", marginBottom: "20px" }}>
          Track Your Grievances
        </h2>

        {/* Search Bar */}
        <div style={{ marginBottom: "20px", textAlign: "center" }}>
          <input
            type="text"
            placeholder="Search by title, description, or category..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              width: "80%",
              maxWidth: "500px",
              padding: "10px",
              borderRadius: "6px",
              border: "1px solid #ccc",
              backgroundColor: "#fff",
            }}
          />
        </div>

        {/* Status Filter Tabs */}
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            gap: "15px",
            marginBottom: "20px",
          }}
        >
          {["All", "SUBMITTED", "IN_PROGRESS", "RESOLVED", "REJECTED"].map(
            (status) => (
              <button
                key={status}
                onClick={() => setFilterStatus(status as any)}
                style={{
                  padding: "10px 20px",
                  borderRadius: "6px",
                  border: "none",
                  cursor: "pointer",
                  fontWeight: filterStatus === status ? "bold" : "normal",
                  backgroundColor:
                    filterStatus === status ? "#81d4fa" : "#f5f5f5",
                  color: "#000",
                }}
              >
                {status.replace("_", " ")}
              </button>
            )
          )}
        </div>

        {/* Content */}
        {loading ? (
          <p style={{ textAlign: "center" }}>Loading grievances...</p>
        ) : error ? (
          <p style={{ textAlign: "center", color: "red" }}>{error}</p>
        ) : filteredGrievances.length === 0 ? (
          <p style={{ textAlign: "center" }}>No grievances found.</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "15px" }}>
            {filteredGrievances.map((g) => (
              <div
                key={g.id}
                onClick={() => {
                  const next = expandedId === g.id ? null : g.id;
                  setExpandedId(next);
                  if (next !== null) loadComments(g.id);
                }}
                style={{
                  padding: "20px",
                  borderRadius: "10px",
                  backgroundColor: "#f5f5f5",
                  cursor: "pointer",
                  display: "flex",
                  flexDirection: "column",
                  gap: "5px",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <strong>{g.title}</strong>
                  <span style={{ color: statusColors[g.status] }}>
                    {g.status.replace("_", " ")}
                  </span>
                </div>

                {expandedId === g.id && (
                  <div
                    style={{
                      marginTop: "10px",
                      fontSize: "0.9em",
                      color: "#000",
                      lineHeight: "1.4em",
                    }}
                  >
                    <p>
                      <strong>Registration Number:</strong>{" "}
                      {g.registrationNumber}
                    </p>
                    <p>
                      <strong>Category:</strong> {g.category}
                    </p>
                    <p>
                      <strong>Subcategory:</strong> {g.subcategory}
                    </p>
                    <p>
                      <strong>Description:</strong> {g.description}
                    </p>
                    <div style={{ marginTop: "10px" }}>
                      <strong>Comments:</strong>
                      {commentLoadingMap[g.id] ? (
                        <p>Loading comments...</p>
                      ) : commentErrorMap[g.id] ? (
                        <p style={{ color: "red" }}>{commentErrorMap[g.id]}</p>
                      ) : commentMap[g.id]?.length ? (
                        commentMap[g.id].map((c) => (
                          <div
                            key={c.id}
                            style={{
                              marginTop: "6px",
                              padding: "8px",
                              background: "#fff",
                              borderRadius: "6px",
                            }}
                          >
                            <div style={{ fontWeight: 600 }}>
                              {c.authorName || "User"} ({c.authorEmail || ""})
                            </div>
                            <div>{c.body}</div>
                            {c.createdAt && (
                              <div style={{ fontSize: "12px", color: "#666" }}>
                                {new Date(c.createdAt).toLocaleString()}
                              </div>
                            )}
                          </div>
                        ))
                      ) : (
                        <p>No comments yet.</p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TrackGrievance;
  const loadComments = async (id: number) => {
    if (commentMap[id]) return;
    setCommentLoadingMap((prev) => ({ ...prev, [id]: true }));
    setCommentErrorMap((prev) => ({ ...prev, [id]: "" }));
    try {
      const data = await grievanceService.getComments(id);
      setCommentMap((prev) => ({ ...prev, [id]: data }));
    } catch (err: any) {
      setCommentErrorMap((prev) => ({
        ...prev,
        [id]: err?.message || "Failed to load comments",
      }));
    } finally {
      setCommentLoadingMap((prev) => ({ ...prev, [id]: false }));
    }
  };
