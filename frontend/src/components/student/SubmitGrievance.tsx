import React, { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  MenuItem,
  Button,
  Alert,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";

const categories = ["Academic", "Administrative", "Facilities", "Other"];
const subCategories = ["Issue 1", "Issue 2", "Issue 3", "Other"];

const SubmitGrievance: React.FC = () => {
  const navigate = useNavigate(); // Added for navigation
  const [category, setCategory] = useState("");
  const [subCategory, setSubCategory] = useState("");
  const [description, setDescription] = useState("");
  const [registrationNumber, setRegistrationNumber] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = () => {
    // simulate submission
    setSubmitted(true);
  };

  return (
    <Box
      sx={{
        width: "100vw",
        minHeight: "100vh",
        backgroundColor: "#e0f7fa",
        p: 3,
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Card sx={{ maxWidth: 700, width: "100%", p: 3, position: "relative" }}>
        {/* Back to Portal button */}
        <Button
          variant="outlined"
          sx={{ mb: 2 }}
          onClick={() => navigate("/auth/student-dashboard")}
        >
          &larr; Back to Portal
        </Button>

        <Box textAlign="center" mb={2}>
          <img src={logo} alt="College Logo" style={{ width: "600px", height: "auto" }} />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>

        {submitted && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Your grievance has been submitted successfully!
          </Alert>
        )}

        <CardContent>
          <TextField
            select
            fullWidth
            label="Category"
            margin="normal"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          >
            {categories.map((c) => (
              <MenuItem key={c} value={c}>
                {c}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            select
            fullWidth
            label="Sub Category"
            margin="normal"
            value={subCategory}
            onChange={(e) => setSubCategory(e.target.value)}
          >
            {subCategories.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            fullWidth
            multiline
            rows={4}
            label="Description"
            margin="normal"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <TextField
            fullWidth
            label="Registration Number"
            margin="normal"
            value={registrationNumber}
            onChange={(e) => setRegistrationNumber(e.target.value)}
          />

          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={handleSubmit}
          >
            Submit Grievance
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SubmitGrievance;
