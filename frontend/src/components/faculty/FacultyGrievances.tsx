// components/faculty/FacultyGrievances.tsx
import React, { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  Typography,
  Button,
  Collapse,
  Box,
} from "@mui/material";
import axios from "axios";
import logo from "../../assets/logo.png"; // same logo as auth pages

interface Grievance {
  id: number;
  title: string;
  description: string;
  status: string; // SUBMITTED, IN_PROGRESS, RESOLVED
  created_at: string;
}

const FacultyGrievances: React.FC = () => {
  const [grievances, setGrievances] = useState<Grievance[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    axios
      .get("/api/grievances/department") // backend API for faculty dept grievances
      .then((res) => setGrievances(res.data))
      .catch((err) => console.error(err));
  }, []);

  const handleExpand = (id: number) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const handleResolve = async (id: number) => {
    try {
      await axios.put(`/api/grievances/${id}/resolve`);
      setGrievances((prev) =>
        prev.map((g) => (g.id === id ? { ...g, status: "RESOLVED" } : g))
      );
    } catch (error) {
      console.error("Error resolving grievance", error);
    }
  };

  return (
    <Box
      sx={{
        width: "100vw",
        height: "100vh",
        backgroundColor: "#e0f7fa",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Card sx={{ width: "100%", maxWidth: 500, p: 3 }}>
        {/* Header with logo and title */}
        <Box textAlign="center" mb={2}>
          <img
            src={logo}
            alt="College Logo"
            style={{ width: "100%", height: "auto", marginBottom: "10px" }}
          />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
          <Typography variant="h5" sx={{ mt: 1 }}>
            Faculty Grievances
          </Typography>
        </Box>

        {/* Grievances list */}
        <CardContent sx={{ maxHeight: "60vh", overflowY: "auto" }}>
          {grievances.length === 0 ? (
            <Typography align="center" color="text.secondary">
              No grievances found.
            </Typography>
          ) : (
            grievances.map((g) => (
              <Card
                key={g.id}
                sx={{
                  mb: 2,
                  bgcolor: g.status === "RESOLVED" ? "lightgreen" : "lightcoral",
                  cursor: "pointer",
                }}
                onClick={() => handleExpand(g.id)}
              >
                <CardContent>
                  <Typography variant="h6">{g.title}</Typography>
                  <Typography variant="body2">
                    Submitted: {new Date(g.created_at).toLocaleString()}
                  </Typography>

                  <Collapse in={expandedId === g.id}>
                    <Box mt={2}>
                      <Typography>{g.description}</Typography>
                      {g.status !== "RESOLVED" && (
                        <Button
                          variant="contained"
                          color="success"
                          sx={{ mt: 1 }}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleResolve(g.id);
                          }}
                        >
                          Resolve
                        </Button>
                      )}
                    </Box>
                  </Collapse>
                </CardContent>
              </Card>
            ))
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default FacultyGrievances;
