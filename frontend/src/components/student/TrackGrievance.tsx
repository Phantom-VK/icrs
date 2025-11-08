import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

interface Grievance {
  id: number;
  title: string;
  description: string;
  category: string;
  status: "Pending" | "Resolved" | "Rejected";
}

const dummyGrievances: Grievance[] = [
  { id: 1, title: "Library Issue", description: "Noise in library", category: "Facilities", status: "Pending" },
  { id: 2, title: "Hostel Issue", description: "Leaking tap", category: "Facilities", status: "Resolved" },
  { id: 3, title: "Exam Issue", description: "Incorrect marks", category: "Academic", status: "Rejected" },
];

const TrackGrievance: React.FC = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [filterStatus, setFilterStatus] = useState<"All" | "Pending" | "Resolved" | "Rejected">("All");

  const filteredGrievances = dummyGrievances.filter((g) => {
    const matchesSearch =
      g.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.category.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === "All" || g.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const statusColors: Record<string, string> = {
    Pending: "black",
    Resolved: "green",
    Rejected: "red",
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
        <h2 style={{ textAlign: "center", marginBottom: "20px" }}>Track Your Grievances</h2>

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
        <div style={{ display: "flex", justifyContent: "center", gap: "15px", marginBottom: "20px" }}>
          {["All", "Pending", "Resolved", "Rejected"].map((status) => (
            <button
              key={status}
              onClick={() => setFilterStatus(status as any)}
              style={{
                padding: "10px 20px",
                borderRadius: "6px",
                border: "none",
                cursor: "pointer",
                fontWeight: filterStatus === status ? "bold" : "normal",
                backgroundColor: filterStatus === status ? "#81d4fa" : "#f5f5f5",
                color: "#000",
              }}
            >
              {status}
            </button>
          ))}
        </div>

        {/* Grievance List */}
        <div style={{ display: "flex", flexDirection: "column", gap: "15px" }}>
          {filteredGrievances.length === 0 ? (
            <p style={{ textAlign: "center" }}>No grievances found.</p>
          ) : (
            filteredGrievances.map((g) => (
              <div
                key={g.id}
                onClick={() => setExpandedId(expandedId === g.id ? null : g.id)}
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
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <strong>{g.title}</strong>
                  <span style={{ color: statusColors[g.status] }}>{g.status}</span>
                </div>
                {expandedId === g.id && (
                  <div style={{ marginTop: "10px", fontSize: "0.9em", color: "#000", lineHeight: "1.4em" }}>
                    <p><strong>Category:</strong> {g.category}</p>
                    <p><strong>Description:</strong> {g.description}</p>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default TrackGrievance;
