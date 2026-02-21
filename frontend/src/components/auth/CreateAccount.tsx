import React, { useState, type ChangeEvent } from "react";
import {
  TextField,
  Button,
  Card,
  CardContent,
  Typography,
  Box,
  Link,
  CircularProgress,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";
import authService from "../../services/authService";
import axios from "axios";

const CreateAccount: React.FC = () => {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: "",
    studentId: "",
    department: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleCreateAccount = async () => {
    // Basic validation
    if (!formData.username || !formData.email || !formData.password) {
      setMessage("Please fill all required fields.");
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setMessage("Passwords do not match.");
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      await authService.register(
        formData.username.trim(),
        formData.studentId.trim(),
        formData.department.trim(),
        formData.email.trim(),
        formData.password.trim() 
      );

      setMessage("Account created successfully! Check your email for verification.");
      // Redirect to verification page
      setTimeout(() => navigate(`/auth/verify?email=${formData.email}`), 2000);
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        if (error.response?.data) {
          // Backend might return a string or object
          const backendMessage =
            typeof error.response.data === "string"
              ? error.response.data
              : error.response.data.message || "Signup failed.";
          setMessage(backendMessage);
        } else {
          setMessage("Network error. Please try again.");
        }
      } else {
        setMessage("Unexpected error occurred.");
      }
    } finally {
      setLoading(false);
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
      <Card
        sx={{
          width: "100%",
          maxWidth: 480,
          p: 3,
          boxShadow: 5,
          borderRadius: 3,
          backgroundColor: "#ffffff",
        }}
      >
        <Box textAlign="center" mb={2}>
          <img
            src={logo}
            alt="College Logo"
            style={{ width: "220px", maxWidth: "80%", marginBottom: "8px" }}
          />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>

        <CardContent>
          <TextField
            fullWidth
            label="Full Name"
            name="username"
            margin="normal"
            value={formData.username}
            onChange={handleChange}
          />
          <TextField
            fullWidth
            label="Student ID"
            name="studentId"
            margin="normal"
            value={formData.studentId}
            onChange={handleChange}
          />
          <TextField
            fullWidth
            label="Department"
            name="department"
            margin="normal"
            value={formData.department}
            onChange={handleChange}
          />
          <TextField
            fullWidth
            label="College Email"
            name="email"
            type="email"
            margin="normal"
            value={formData.email}
            onChange={handleChange}
          />
          <TextField
            fullWidth
            label="Password"
            type="password"
            name="password"
            margin="normal"
            value={formData.password}
            onChange={handleChange}
          />
          <TextField
            fullWidth
            label="Confirm Password"
            type="password"
            name="confirmPassword"
            margin="normal"
            value={formData.confirmPassword}
            onChange={handleChange}
          />

          <Button
            fullWidth
            variant="contained"
            color="primary"
            sx={{ mt: 2, py: 1.2, fontWeight: "bold" }}
            onClick={handleCreateAccount}
            disabled={loading}
          >
            {loading ? <CircularProgress size={24} color="inherit" /> : "Create Account"}
          </Button>

          {message && (
            <Typography
              color={
                message.toLowerCase().includes("success")
                  ? "success.main"
                  : "error.main"
              }
              textAlign="center"
              sx={{ mt: 2 }}
            >
              {message}
            </Typography>
          )}

          <Typography textAlign="center" sx={{ mt: 2 }}>
            <Link
              component="button"
              variant="body2"
              onClick={() => navigate("/auth/login")}
            >
              Already have an account? Sign In
            </Link>
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default CreateAccount;
