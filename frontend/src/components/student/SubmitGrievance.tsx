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
import grievanceService from "../../services/grievanceService";
import authService from "../../services/authService";
import LoadingOverlay from "../common/LoadingOverlay";
import { useSnackbar } from "notistack";

const categories = ["Academic", "Administrative", "Facilities", "Other"];
const subCategories = ["Issue 1", "Issue 2", "Issue 3", "Other"];

const SubmitGrievance: React.FC = () => {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [category, setCategory] = useState("");
  const [subCategory, setSubCategory] = useState("");
  const [description, setDescription] = useState("");
  const [registrationNumber, setRegistrationNumber] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError(null);
    setSubmitted(false);

    if (!category || !description || !registrationNumber) {
      setError("Please fill in all required fields.");
      return;
    }

    setLoading(true);
    try {
      let user = null;
      try {
        user = await authService.getCurrentUser();
        console.log("Logged-in user:", user?.email || "Unknown user");
      } catch {
        console.warn("Could not fetch user info, proceeding anyway.");
      }

      const title = `${category}${subCategory ? ` - ${subCategory}` : ""}`;
      const payload = {
        title,
        category,
        subcategory: subCategory,
        description,
        registrationNumber,
      };

      console.log("Submitting grievance payload:", payload);

      const response = await grievanceService.submit(payload);
      console.log("Backend response:", response);

      setSubmitted(true);
      enqueueSnackbar("Grievance submitted successfully", { variant: "success" });
      setCategory("");
      setSubCategory("");
      setDescription("");
      setRegistrationNumber("");

      setTimeout(() => navigate("/auth/student-dashboard"), 2000);
    } catch (err: any) {
      const message =
        err?.response?.data?.message ||
        err?.response?.data ||
        "Failed to submit grievance. Please try again.";
      console.error("Submission failed:", message);
      setError(message);
      enqueueSnackbar(message, { variant: "error" });
    } finally {
      setLoading(false);
    }
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
      <LoadingOverlay show={loading} message="Submitting grievance..." />
      <Card sx={{ maxWidth: 700, width: "100%", p: 3 }}>
        {/* Back Button */}
        <Button
          variant="outlined"
          sx={{ mb: 2 }}
          onClick={() => navigate("/auth/student-dashboard")}
        >
          &larr; Back to Portal
        </Button>

        {/* Header */}
        <Box textAlign="center" mb={2}>
          <img
            src={logo}
            alt="College Logo"
            style={{ width: "220px", maxWidth: "80%", height: "auto" }}
          />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>

        {/* Alerts */}
        {submitted && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Grievance submitted successfully! Redirecting...
          </Alert>
        )}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {typeof error === "string" ? error : "An unknown error occurred."}
          </Alert>
        )}

        {/* Form */}
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
            disabled={loading}
          >
            {loading ? "Submitting..." : "Submit Grievance"}
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SubmitGrievance;
