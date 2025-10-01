import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import logo from "../../assets/logo.png";

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

const StudentDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const pendingCount = dummyGrievances.filter((g) => g.status === "Pending").length;
  const resolvedCount = dummyGrievances.filter((g) => g.status === "Resolved").length;
  const rejectedCount = dummyGrievances.filter((g) => g.status === "Rejected").length;

  const toggleExpand = (id: number) => {
    setExpandedId(expandedId === id ? null : id);
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
          backgroundColor: "#ffffff",
          borderRadius: "10px",
          padding: "30px",
          position: "relative",
          boxShadow: "0 2px 8px rgba(255, 255, 255, 0.1)",
        }}
      >
        {/* Logout Button */}
        <button
          onClick={() => navigate("/auth/login")}
          style={{
            position: "absolute",
            top: "20px",
            right: "20px",
            padding: "6px 14px",
            cursor: "pointer",
            color: "#ffffffff",
          }}
        >
          Logout
        </button>

        {/* Header */}
        <div style={{ textAlign: "center", marginBottom: "30px" }}>
          <img src={logo} alt="College Logo" style={{ width: "700px", marginBottom: "10px" }} />
          <h2 style={{ color: "#000" }}>SGGS COLLEGE REDRESSAL SYSTEM</h2>
        </div>

        {/* Submit / Track Buttons */}
        <div style={{ display: "flex", gap: "20px", marginBottom: "30px" }}>
          <div
            onClick={() => navigate("/student/submit-grievance")}
            style={{
              flex: 1,
              backgroundColor: "#b2ebf2",
              borderRadius: "10px",
              padding: "20px",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: "15px",
            }}
          >
            <AddIcon style={{ fontSize: 40, color: "#03a9f4" }} />
            <div style={{ textAlign: "left" }}>
              <h3 style={{ margin: "0 0 5px 0" }}>Submit Grievance</h3>
              <p style={{ fontSize: "0.8em", margin: 0 }}>
                Report an Issue of concern that needs attention.
              </p>
            </div>
          </div>

          <div
            onClick={() => navigate("/student/track-grievance")}
            style={{
              flex: 1,
              backgroundColor: "#c8e6c9",
              borderRadius: "10px",
              padding: "20px",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: "15px",
            }}
          >
            <SearchIcon style={{ fontSize: 40, color: "green" }} />
            <div style={{ textAlign: "left" }}>
              <h3 style={{ margin: "0 0 5px 0" }}>Track Grievance</h3>
              <p style={{ fontSize: "0.8em", margin: 0 }}>
                View the status and Progress of your submitted grievances.
              </p>
            </div>
          </div>
        </div>

        {/* Grievance Overview */}
        <div style={{ display: "flex", gap: "10px", marginBottom: "30px" }}>
          <div
            style={{
              flex: 1,
              backgroundColor: "#f5f5f5",
              padding: "15px",
              borderRadius: "6px",
              textAlign: "center",
            }}
          >
            <h4 style={{ color: "#000" }}>Pending</h4>
            <p style={{ fontSize: "20px", fontWeight: "bold", color: "black", margin: 0 }}>{pendingCount}</p>
          </div>
          <div
            style={{
              flex: 1,
              backgroundColor: "#f5f5f5",
              padding: "15px",
              borderRadius: "6px",
              textAlign: "center",
            }}
          >
            <h4 style={{ color: "#000" }}>Resolved</h4>
            <p style={{ fontSize: "20px", fontWeight: "bold", color: "green", margin: 0 }}>{resolvedCount}</p>
          </div>
          <div
            style={{
              flex: 1,
              backgroundColor: "#f5f5f5",
              padding: "15px",
              borderRadius: "6px",
              textAlign: "center",
            }}
          >
            <h4 style={{ color: "#000" }}>Rejected</h4>
            <p style={{ fontSize: "20px", fontWeight: "bold", color: "red", margin: 0 }}>{rejectedCount}</p>
          </div>
        </div>

        {/* Recent Grievances */}
        <div>
          <h4 style={{ color: "#000" }}>Recent Grievances</h4>
          <ul style={{ listStyle: "none", paddingLeft: 0 }}>
            {dummyGrievances.map((g) => (
              <li
                key={g.id}
                onClick={() => setExpandedId(expandedId === g.id ? null : g.id)}
                style={{
                  padding: "15px 0",
                  borderBottom: "1px solid #ddd",
                  cursor: "pointer",
                  color: "#000",
                }}
              >
                <strong>{g.title}</strong> - Status: {g.status}
                {expandedId === g.id && (
                  <div style={{ marginTop: "5px", fontSize: "0.9em", color: "#000", paddingLeft: "10px" }}>
                    <p><strong>Category:</strong> {g.category}</p>
                    <p><strong>Description:</strong> {g.description}</p>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
};

export default StudentDashboard;
