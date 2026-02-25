import React, { useEffect, useMemo, useState } from "react";
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
import type { Category, Subcategory } from "../../types/category";

const SubmitGrievance: React.FC = () => {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [subCategoryId, setSubCategoryId] = useState<number | "">("");
  const [description, setDescription] = useState("");
  const [registrationNumber, setRegistrationNumber] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [userLoading, setUserLoading] = useState(false);

  useEffect(() => {
    const loadCategories = async () => {
      setCategoryLoading(true);
      try {
        const data = await grievanceService.getCategories();
        setCategories(data || []);
      } catch (err: any) {
        const message = err?.message || "Failed to load categories.";
        setError(message);
        enqueueSnackbar(message, { variant: "error" });
      } finally {
        setCategoryLoading(false);
      }
    };
    loadCategories();
  }, [enqueueSnackbar]);

  useEffect(() => {
    const loadUser = async () => {
      setUserLoading(true);
      try {
        const user = await authService.getCurrentUser();
        if (user?.studentId) {
          setRegistrationNumber(user.studentId);
        }
      } catch (err: any) {
        console.warn("Could not fetch user for registration number", err);
      } finally {
        setUserLoading(false);
      }
    };
    loadUser();
  }, []);

  const currentSubcategories: Subcategory[] = useMemo(() => {
    const cat = categories.find((c) => c.id === categoryId);
    return cat?.subcategories || [];
  }, [categories, categoryId]);

  const handleSubmit = async () => {
    setError(null);
    setSubmitted(false);

    if (!categoryId || !description) {
      setError("Please fill in all required fields.");
      return;
    }

    if (!registrationNumber) {
      setError("Could not fetch your registration number. Please re-login and try again.");
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

      const selectedCategory = categories.find((c) => c.id === categoryId);
      const selectedSub = currentSubcategories.find((s) => s.id === subCategoryId);
      const title = `${selectedCategory?.name || "Grievance"}${selectedSub ? ` - ${selectedSub.name}` : ""}`;
      const payload = {
        title,
        categoryId,
        subcategoryId: subCategoryId || undefined,
        description,
        registrationNumber,
      };

      console.log("Submitting grievance payload:", payload);

      const response = await grievanceService.submit(payload);
      console.log("Backend response:", response);

      setSubmitted(true);
      enqueueSnackbar("Grievance submitted successfully", { variant: "success" });
      setCategoryId("");
      setSubCategoryId("");
      setDescription("");

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
            value={categoryId}
            onChange={(e) => {
              setCategoryId(Number(e.target.value));
              setSubCategoryId("");
            }}
            disabled={categoryLoading}
            helperText={
              categoryLoading
                ? "Loading categories..."
                : categories.length === 0
                  ? "No categories available"
                  : ""
            }
          >
            {categories.map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {c.name}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            select
            fullWidth
            label="Sub Category"
            margin="normal"
            value={subCategoryId}
            onChange={(e) => setSubCategoryId(Number(e.target.value))}
            disabled={!categoryId || currentSubcategories.length === 0}
            helperText={
              !categoryId
                ? "Select a category first"
                : currentSubcategories.length === 0
                  ? "No subcategories for this category"
                  : ""
            }
          >
            {currentSubcategories.map((s) => (
              <MenuItem key={s.id} value={s.id}>
                {s.name}
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
            InputProps={{ readOnly: true }}
            helperText={
              userLoading
                ? "Fetching your registration number..."
                : registrationNumber
                ? "Fetched from your profile"
                : "We could not fetch your registration number; please re-login."
            }
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
