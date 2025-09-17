import React, { useState } from "react";
import {
  TextField,
  Button,
  Card,
  CardContent,
  Typography,
  Box,
  MenuItem,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";
const StudentPortal: React.FC = () => {
  const navigate = useNavigate();
  const [role, setRole] = useState("student");
  const [subCategory, setSubCategory] = useState("");

  const studentSubCategories = [
    "Academic Issues",
    "Administrative Issues",
    "Infrastructure & Facilities",
    "Discipline & Behavior",
    "Career & Support",
    "Other",
  ];

  const adminSubCategories = [
    "Governance",
    "Academics",
    "Student Welfare",
    "Infrastructure",
    "Finance & Administration",
    "Placements & Alumni",
  ];

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
        <Box textAlign="center" mb={2}>
          <img
            src={logo}
            alt="College Logo"
            style={{ width: "100%", height: "auto" }}
          />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>
        <CardContent>
          <Typography variant="h5" textAlign="center" gutterBottom>
            Submit Your Grievance
          </Typography>

          {/* Category */}
          <TextField
            select
            fullWidth
            label="Select Category"
            margin="normal"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            <MenuItem value="student">Student</MenuItem>
            <MenuItem value="admin">Admin</MenuItem>
          </TextField>

          {/* Sub Category */}
          <TextField
            select
            fullWidth
            label="Sub Category"
            margin="normal"
            value={subCategory}
            onChange={(e) => setSubCategory(e.target.value)}
          >
            {(role === "student" ? studentSubCategories : adminSubCategories).map(
              (option, index) => (
                <MenuItem key={index} value={option}>
                  {option}
                </MenuItem>
              )
            )}
          </TextField>

          {/* Description */}
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Description"
            margin="normal"
          />

          {/* Registration Number */}
          <TextField fullWidth label="Registration Number" margin="normal" />

          {/* Submit */}
          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={() => navigate("/auth/first-page")}
          >
            Submit
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default StudentPortal;
