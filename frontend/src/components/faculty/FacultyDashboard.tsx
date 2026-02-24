import React, { useEffect, useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  MenuItem,
  Button,
  Collapse,
  CircularProgress,
  Alert,
} from "@mui/material";
import logo from "../../assets/logo.png";
import { useNavigate } from "react-router-dom";
import grievanceService from "../../services/grievanceService";
import type { Comment } from "../../services/grievanceService";
import authService from "../../services/authService";

interface Grievance {
  id: number;
  title: string;
  description: string;
  category: string;
  subcategory: string;
  registrationNumber: string;
  status: "SUBMITTED" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";
}

const FacultyDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [grievances, setGrievances] = useState<Grievance[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [statusUpdates, setStatusUpdates] = useState<Record<number, Grievance["status"]>>({});
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [comments, setComments] = useState<Record<number, Comment[]>>({});
  const [commentInputs, setCommentInputs] = useState<Record<number, string>>({});
  const [commentLoading, setCommentLoading] = useState<Record<number, boolean>>({});
  const [commentError, setCommentError] = useState<Record<number, string>>({});

  useEffect(() => {
    const fetchGrievances = async () => {
      try {
        const user = await authService.getCurrentUser();
        console.log("Logged-in faculty:", user);

        const res = await grievanceService.getAll();
        // if paginated, data.content else full array
        setGrievances(res.content || res);
      } catch (err) {
        console.error("Error loading grievances:", err);
        setError("Failed to load grievances. Please try again.");
      } finally {
        setLoading(false);
      }
    };

    fetchGrievances();
  }, []);

  const handleExpand = (id: number) => setExpandedId(expandedId === id ? null : id);

  const loadComments = async (id: number) => {
    if (comments[id]) return;
    setCommentLoading((prev) => ({ ...prev, [id]: true }));
    setCommentError((prev) => ({ ...prev, [id]: "" }));
    try {
      const data = await grievanceService.getComments(id);
      setComments((prev) => ({ ...prev, [id]: data }));
    } catch (err: any) {
      setCommentError((prev) => ({
        ...prev,
        [id]: err?.message || "Failed to load comments",
      }));
    } finally {
      setCommentLoading((prev) => ({ ...prev, [id]: false }));
    }
  };

  // Load comments when a grievance is expanded
  useEffect(() => {
    if (expandedId !== null) {
      loadComments(expandedId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expandedId]);

  const handleStatusChange = (id: number, newStatus: Grievance["status"]) => {
    setStatusUpdates((prev) => ({ ...prev, [id]: newStatus }));
  };

  const handleUpdateGrievance = async (id: number) => {
    const newStatus = statusUpdates[id];
    if (!newStatus) return;

    try {
      await grievanceService.updateStatus(id, newStatus);
      setGrievances((prev) =>
        prev.map((g) => (g.id === id ? { ...g, status: newStatus } : g))
      );
      setSuccessMsg(`Grievance #${id} updated to ${newStatus}`);
      setTimeout(() => setSuccessMsg(""), 2000);
    } catch (err) {
      console.error("Update failed:", err);
      setError("Failed to update grievance.");
      setTimeout(() => setError(""), 2000);
    }
  };

  const filteredGrievances = grievances.filter(
    (g) =>
      g.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (g.registrationNumber || "").toLowerCase().includes(searchTerm.toLowerCase())
  );

  const statusColors: Record<string, string> = {
    SUBMITTED: "black",
    IN_PROGRESS: "#007bff",
    RESOLVED: "green",
    REJECTED: "red",
  };

  const handleLogout = () => {
    authService.logout();
    navigate("/auth/login");
  };

  const totalCount = grievances.length;
  const submittedCount = grievances.filter((g) => g.status === "SUBMITTED").length;
  const inProgressCount = grievances.filter((g) => g.status === "IN_PROGRESS").length;
  const resolvedCount = grievances.filter((g) => g.status === "RESOLVED").length;
  const rejectedCount = grievances.filter((g) => g.status === "REJECTED").length;

  return (
    <Box
      sx={{
        width: "100vw",
        minHeight: "100vh",
        backgroundColor: "#e0f7fa",
        p: 3,
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start",
      }}
    >
      <Card sx={{ maxWidth: 1100, width: "100%", p: 3, position: "relative" }}>
        {/* Logout */}
        <Button
          variant="outlined"
          sx={{ position: "absolute", top: 16, right: 16, color: "#000", borderColor: "#ccc" }}
          onClick={handleLogout}
        >
          Logout
        </Button>

        {/* Header */}
        <Box textAlign="center" mb={3}>
          <img src={logo} alt="College Logo" style={{ width: "260px", maxWidth: "80%", marginBottom: "10px" }} />
          <Typography variant="h6" fontWeight="bold">
            Faculty Dashboard
          </Typography>
        </Box>

        {/* Notifications */}
        {successMsg && <Alert severity="success" sx={{ mb: 2 }}>{successMsg}</Alert>}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        {/* Summary */}
        <Box sx={{ display: "flex", gap: 2, mb: 3 }}>
          <SummaryCard label="Total" value={totalCount} color="#000" />
          <SummaryCard label="Submitted" value={submittedCount} color="black" />
          <SummaryCard label="In Progress" value={inProgressCount} color="#007bff" />
          <SummaryCard label="Resolved" value={resolvedCount} color="green" />
          <SummaryCard label="Rejected" value={rejectedCount} color="red" />
        </Box>

        {/* Search */}
        <TextField
          fullWidth
          placeholder="Search grievances..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          sx={{ mb: 3, backgroundColor: "#fff", borderRadius: 1 }}
        />

        {/* Content */}
        {loading ? (
          <Box textAlign="center">
            <CircularProgress />
            <Typography>Loading grievances...</Typography>
          </Box>
        ) : filteredGrievances.length === 0 ? (
          <Typography>No grievances found.</Typography>
        ) : (
          filteredGrievances.map((g) => (
            <Card
              key={g.id}
              sx={{ mb: 2, bgcolor: "#f5f5f5", cursor: "pointer" }}
              onClick={() => handleExpand(g.id)}
            >
              <CardContent>
                <Typography variant="h6">{g.title}</Typography>
                <Typography variant="body2" sx={{ color: statusColors[g.status] }}>
                  Status: {g.status.replace("_", " ")}
                </Typography>

                <Collapse in={expandedId === g.id}>
                  <Box mt={2}>
                    <Typography>
                      <strong>Registration Number:</strong> {g.registrationNumber}
                    </Typography>
                    <Typography>
                      <strong>Category:</strong> {g.category}
                    </Typography>
                    <Typography>
                      <strong>Subcategory:</strong> {g.subcategory}
                    </Typography>
                    <Typography>
                      <strong>Description:</strong> {g.description}
                    </Typography>

                    {g.status !== "RESOLVED" && g.status !== "REJECTED" && (
                      <Box sx={{ mt: 1, display: "flex", alignItems: "center", gap: 2 }}>
                        <TextField
                          select
                          label="Update Status"
                          value={statusUpdates[g.id] || g.status}
                          onChange={(e) =>
                            handleStatusChange(g.id, e.target.value as Grievance["status"])
                          }
                          sx={{ minWidth: 180 }}
                        >
                          <MenuItem value="SUBMITTED">Submitted</MenuItem>
                          <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                          <MenuItem value="RESOLVED">Resolved</MenuItem>
                          <MenuItem value="REJECTED">Rejected</MenuItem>
                        </TextField>

                        <Button
                          variant="contained"
                          color="primary"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleUpdateGrievance(g.id);
                          }}
                        >
                          Update
                        </Button>
                      </Box>
                    )}

                    <Box mt={2}>
                      <Typography variant="subtitle1">Comments</Typography>
                      {commentLoading[g.id] ? (
                        <Typography variant="body2">Loading comments...</Typography>
                      ) : commentError[g.id] ? (
                        <Typography variant="body2" color="error">
                          {commentError[g.id]}
                        </Typography>
                      ) : comments[g.id]?.length ? (
                        comments[g.id].map((c) => (
                          <Box key={c.id} sx={{ mt: 1, p: 1.5, bgcolor: "white", borderRadius: 1 }}>
                            <Typography variant="body2" fontWeight="bold">
                              {c.authorName || "User"} ({c.authorEmail || ""})
                            </Typography>
                            <Typography variant="body2" sx={{ whiteSpace: "pre-wrap" }}>
                              {c.body}
                            </Typography>
                            {c.createdAt && (
                              <Typography variant="caption" color="text.secondary">
                                {new Date(c.createdAt).toLocaleString()}
                              </Typography>
                            )}
                          </Box>
                        ))
                      ) : (
                        <Typography variant="body2">No comments yet.</Typography>
                      )}

                      <TextField
                        fullWidth
                        multiline
                        minRows={2}
                        sx={{ mt: 1 }}
                        placeholder="Add a comment for the student"
                        value={commentInputs[g.id] || ""}
                        onChange={(e) =>
                          setCommentInputs((prev) => ({ ...prev, [g.id]: e.target.value }))
                        }
                      />
                      <Button
                        variant="outlined"
                        sx={{ mt: 1 }}
                        onClick={async (e) => {
                          e.stopPropagation();
                          const body = (commentInputs[g.id] || "").trim();
                          if (!body) return;
                          setCommentLoading((prev) => ({ ...prev, [g.id]: true }));
                          setCommentError((prev) => ({ ...prev, [g.id]: "" }));
                          try {
                            const newComment = await grievanceService.addComment(g.id, body);
                            setComments((prev) => ({
                              ...prev,
                              [g.id]: [...(prev[g.id] || []), newComment],
                            }));
                            setCommentInputs((prev) => ({ ...prev, [g.id]: "" }));
                          } catch (err: any) {
                            setCommentError((prev) => ({
                              ...prev,
                              [g.id]: err?.message || "Failed to add comment",
                            }));
                          } finally {
                            setCommentLoading((prev) => ({ ...prev, [g.id]: false }));
                          }
                        }}
                      >
                        Add Comment
                      </Button>
                    </Box>
                  </Box>
                </Collapse>
              </CardContent>
            </Card>
          ))
        )}
      </Card>
    </Box>
  );
};

const SummaryCard = ({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: string;
}) => (
  <Card sx={{ flex: 1, p: 2, textAlign: "center" }}>
    <Typography variant="h6">{label}</Typography>
    <Typography variant="h4" sx={{ color }}>
      {value}
    </Typography>
  </Card>
);

export default FacultyDashboard;
