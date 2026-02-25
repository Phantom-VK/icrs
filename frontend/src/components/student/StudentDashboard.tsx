import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import logo from "../../assets/logo.png";
import authService from "../../services/authService";
import grievanceService from "../../services/grievanceService";
import { getActiveSortedGrievances } from "../../utils/grievanceFilters";

interface Grievance {
  id: number;
  title: string;
  description: string;
  category: string;
  status: "SUBMITTED" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";
  createdAt?: string;
}

const StudentDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [grievances, setGrievances] = useState<Grievance[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [user, setUser] = useState<{ email: string } | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      const token = localStorage.getItem("token");
      if (!token) {
        console.warn("No token found — redirecting to login");
        navigate("/auth/login");
        return;
      }

      try {
        const currentUser = await authService.getCurrentUser();
        if (!currentUser?.email) {
          console.error("Invalid user session:", currentUser);
          throw new Error("Invalid or expired user session.");
        }

        console.log("Logged in as:", currentUser.email);
        setUser(currentUser);

        const grievancesData = await grievanceService.getMyGrievances();
        console.log(`Loaded ${grievancesData.length} grievances.`);
        setGrievances(grievancesData || []);
      } catch (err: any) {
        console.error("Error loading dashboard data:", err);
        setError("Failed to load grievances. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    // Delay to ensure Axios interceptor applies token properly
    setTimeout(fetchData, 250);
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
    navigate("/auth/login");
  };

  const toggleExpand = (id: number) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const pendingCount = grievances.filter((g) => g.status === "SUBMITTED").length;
  const resolvedCount = grievances.filter((g) => g.status === "RESOLVED").length;
  const rejectedCount = grievances.filter((g) => g.status === "REJECTED").length;

  const activeGrievances = getActiveSortedGrievances(grievances);

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
      {loading && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.2)",
            display: "flex",
            alignItems: "flex-end",
            justifyContent: "center",
            paddingBottom: "20px",
            zIndex: 1200,
            fontWeight: 600,
            color: "#000",
          }}
        >
          Loading dashboard...
        </div>
      )}
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
          onClick={handleLogout}
          style={{
            position: "absolute",
            top: "20px",
            right: "20px",
            padding: "6px 14px",
            cursor: "pointer",
            backgroundColor: "#0288d1",
            color: "#fff",
            border: "none",
            borderRadius: "4px",
          }}
        >
          Logout
        </button>

        {/* Header */}
        <div style={{ textAlign: "center", marginBottom: "30px" }}>
          <img src={logo} alt="College Logo" style={{ width: "260px", maxWidth: "80%", marginBottom: "10px" }} />
          <h2 style={{ color: "#000" }}>SGGS COLLEGE REDRESSAL SYSTEM</h2>
          {user && <p>Welcome, {user.email}</p>}
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
                Report an issue of concern that needs attention.
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
                View the status and progress of your submitted grievances.
              </p>
            </div>
          </div>
        </div>

        {/* Overview Cards */}
        <div style={{ display: "flex", gap: "10px", marginBottom: "30px" }}>
          <OverviewCard label="Pending" count={pendingCount} color="black" />
          <OverviewCard label="Resolved" count={resolvedCount} color="green" />
          <OverviewCard label="Rejected" count={rejectedCount} color="red" />
        </div>

        {/* Recent Grievances */}
        <div>
          <h4 style={{ color: "#000" }}>Recent Grievances</h4>
          {loading ? (
            <p>Loading grievances...</p>
          ) : error ? (
            <p style={{ color: "red" }}>{error}</p>
          ) : activeGrievances.length === 0 ? (
            <p>No active grievances. Submit a new one to get started.</p>
          ) : (
            <ul style={{ listStyle: "none", paddingLeft: 0 }}>
          {activeGrievances.map((g) => (
            <li
              key={g.id}
              onClick={() => toggleExpand(g.id)}
              style={{
                    padding: "15px 0",
                    borderBottom: "1px solid #ddd",
                    cursor: "pointer",
                    color: "#000",
                  }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span>
                    <strong>{g.title}</strong> - Status: {g.status}
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/student/track-grievance?id=${g.id}`);
                    }}
                    style={{
                      padding: "6px 10px",
                      borderRadius: "4px",
                      border: "1px solid #0288d1",
                      background: "#fff",
                      color: "#0288d1",
                      cursor: "pointer",
                    }}
                  >
                    More info
                  </button>
                </div>
                {expandedId === g.id && (
                  <div
                    style={{
                      marginTop: "5px",
                      fontSize: "0.9em",
                        paddingLeft: "10px",
                      }}
                    >
                      <p>
                        <strong>Category:</strong> {g.category}
                      </p>
                      <p>
                        <strong>Description:</strong> {g.description}
                      </p>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};

const OverviewCard = ({
  label,
  count,
  color,
}: {
  label: string;
  count: number;
  color: string;
}) => (
  <div
    style={{
      flex: 1,
      backgroundColor: "#f5f5f5",
      padding: "15px",
      borderRadius: "6px",
      textAlign: "center",
    }}
  >
    <h4 style={{ color: "#000" }}>{label}</h4>
    <p
      style={{
        fontSize: "20px",
        fontWeight: "bold",
        color,
        margin: 0,
      }}
    >
      {count}
    </p>
  </div>
);

export default StudentDashboard;
