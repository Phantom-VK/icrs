import React, { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  MenuItem,
  Button,
  Collapse,
} from "@mui/material";
import logo from "../../assets/logo.png";
import { useNavigate } from "react-router-dom";

interface Grievance {
  id: number;
  title: string;
  studentId: string;
  category: string;
  description: string;
  status: "Pending" | "Resolved" | "Rejected";
}

const dummyGrievances: Grievance[] = [
  {
    id: 1,
    title: "Library Noise",
    studentId: "S12345",
    category: "Facilities",
    description: "Noise in library",
    status: "Pending",
  },
  {
    id: 2,
    title: "Exam Marks",
    studentId: "S23456",
    category: "Academic",
    description: "Incorrect marks entered",
    status: "Resolved",
  },
  {
    id: 3,
    title: "Hostel Leak",
    studentId: "S34567",
    category: "Facilities",
    description: "Water leakage in hostel",
    status: "Pending",
  },
  {
    id: 4,
    title: "Cafeteria Issue",
    studentId: "S45678",
    category: "Facilities",
    description: "Food quality problem",
    status: "Rejected",
  },
];

const FacultyDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [grievances, setGrievances] = useState(dummyGrievances);
  const [statusUpdates, setStatusUpdates] = useState<Record<number, Grievance["status"]>>({});

  const handleExpand = (id: number) => setExpandedId(expandedId === id ? null : id);

  const handleStatusChange = (id: number, newStatus: Grievance["status"]) =>
    setStatusUpdates((prev) => ({ ...prev, [id]: newStatus }));

  const handleUpdateGrievance = (id: number) => {
    const updatedStatus = statusUpdates[id] || grievances.find((g) => g.id === id)?.status;
    if (!updatedStatus) return;

    setGrievances((prev) =>
      prev.map((g) =>
        g.id === id
          ? { ...g, status: updatedStatus, description: updatedStatus === "Resolved" ? g.description + " (Resolved)" : g.description }
          : g
      )
    );

    if (updatedStatus === "Resolved") {
      setStatusUpdates((prev) => {
        const copy = { ...prev };
        delete copy[id];
        return copy;
      });
    }
  };

  const filteredGrievances = grievances.filter(
    (g) => g.title.toLowerCase().includes(searchTerm.toLowerCase()) || g.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const statusColors: Record<string, string> = {
    Pending: "black",
    Resolved: "green",
    Rejected: "red",
  };

  // Summary counts
  const totalCount = grievances.length;
  const pendingCount = grievances.filter((g) => g.status === "Pending").length;
  const resolvedCount = grievances.filter((g) => g.status === "Resolved").length;
  const rejectedCount = grievances.filter((g) => g.status === "Rejected").length;

  return (
    <Box sx={{ width: "100vw", minHeight: "100vh", backgroundColor: "#e0f7fa", p: 3, display: "flex", justifyContent: "center", alignItems: "flex-start" }}>
      <Card sx={{ maxWidth: 1050, width: "100%", p: 3, position: "relative" }}>
        {/* Logout */}
        <Button
          variant="outlined"
          sx={{ position: "absolute", top: 16, right: 16, color: "#000", borderColor: "#ccc" }}
          onClick={() => navigate("/auth/login")}
        >
          Logout
        </Button>

        {/* Header */}
        <Box textAlign="center" mb={3}>
          <img src={logo} alt="College Logo" style={{ width: "700px", marginBottom: "10px" }} />
          <Typography variant="h6" fontWeight="bold">Faculty Dashboard</Typography>
        </Box>

        {/* Summary Boxes */}
        <Box sx={{ display: "flex", gap: 2, mb: 3 }}>
          <Card sx={{ flex: 1, p: 2, textAlign: "center" }}>
            <Typography variant="h6">Total</Typography>
            <Typography variant="h4">{totalCount}</Typography>
          </Card>
          <Card sx={{ flex: 1, p: 2, textAlign: "center" }}>
            <Typography variant="h6">Pending</Typography>
            <Typography variant="h4" sx={{ color: "black" }}>{pendingCount}</Typography>
          </Card>
          <Card sx={{ flex: 1, p: 2, textAlign: "center" }}>
            <Typography variant="h6">Resolved</Typography>
            <Typography variant="h4" sx={{ color: "green" }}>{resolvedCount}</Typography>
          </Card>
          <Card sx={{ flex: 1, p: 2, textAlign: "center" }}>
            <Typography variant="h6">Rejected</Typography>
            <Typography variant="h4" sx={{ color: "red" }}>{rejectedCount}</Typography>
          </Card>
        </Box>

        {/* Search */}
        <TextField
          fullWidth
          placeholder="Search grievances..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          sx={{ mb: 3, backgroundColor: "#fff", borderRadius: 1 }}
        />

        {/* Grievance List */}
        {filteredGrievances.map((g) => (
          <Card key={g.id} sx={{ mb: 2, bgcolor: "#f5f5f5", cursor: "pointer" }} onClick={() => handleExpand(g.id)}>
            <CardContent>
              <Typography variant="h6">{g.title}</Typography>
              <Typography variant="body2" sx={{ color: statusColors[g.status] }}>Status: {g.status}</Typography>

              <Collapse in={expandedId === g.id}>
                <Box mt={2}>
                  <Typography>Student ID: {g.studentId}</Typography>
                  <Typography>Category: {g.category}</Typography>
                  <Typography>Description: {g.description}</Typography>

                  {/* Update controls */}
                  {g.status !== "Resolved" && (
                    <Box sx={{ mt: 1, display: "flex", alignItems: "center", gap: 2 }}>
                      <TextField
                        select
                        label="Update Status"
                        value={statusUpdates[g.id] || g.status}
                        onChange={(e) => handleStatusChange(g.id, e.target.value as Grievance["status"])}
                        sx={{ minWidth: 150 }}
                      >
                        <MenuItem value="Pending">Pending</MenuItem>
                        <MenuItem value="Resolved">Resolved</MenuItem>
                        <MenuItem value="Rejected">Rejected</MenuItem>
                      </TextField>

                      <Button
                        variant="contained"
                        onClick={(e) => { e.stopPropagation(); handleUpdateGrievance(g.id); }}
                      >
                        Update Grievance
                      </Button>
                    </Box>
                  )}
                </Box>
              </Collapse>
            </CardContent>
          </Card>
        ))}
      </Card>
    </Box>
  );
};

export default FacultyDashboard;
