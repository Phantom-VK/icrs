import React, { useState } from "react";
import {
  TextField,
  Button,
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
  Link,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";
import authService from "../../services/authService";
import axios from "axios";

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  /** ✅ Handle login */
const handleLogin = async () => {
  if (!email || !password) {
    setMessage("❌ Please enter both email and password.");
    return;
  }

  setLoading(true);
  setMessage("");

  try {
    const result = await authService.login(email.trim(), password.trim());

    console.log("🔥 Raw login response:", JSON.stringify(result, null, 2));

    if (!result?.token) {
      setMessage("❌ Invalid response from server.");
      return;
    }

    setMessage("✅ Login successful!");

    // Store session data
    localStorage.setItem("isLoggedIn", "true");
    localStorage.setItem("role", result.role);
    localStorage.setItem("username", result.username);
    localStorage.setItem("email", result.email);

    // 🚀 Redirect based on role
    setTimeout(() => {
      if (result.role === "FACULTY") {
        console.log("➡ Redirecting to FACULTY dashboard");
        navigate("/faculty/dashboard");
      } else {
        console.log("➡ Redirecting to STUDENT dashboard");
        navigate("/auth/student-dashboard");
      }
    }, 500);

  } catch (error: unknown) {
    console.error("Login error:", error);

    if (axios.isAxiosError(error)) {
      const backendMessage =
        typeof error.response?.data === "string"
          ? error.response.data
          : error.response?.data?.message || "Invalid credentials.";

      setMessage(`❌ ${backendMessage}`);
    } else {
      setMessage("❌ Unexpected error occurred.");
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
          maxWidth: 420,
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
            style={{ width: "400px", marginBottom: "8px" }}
          />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>

        <CardContent>
          <TextField
            fullWidth
            label="College Email"
            type="email"
            margin="normal"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <TextField
            fullWidth
            label="Password"
            type="password"
            margin="normal"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <Button
            fullWidth
            variant="contained"
            color="primary"
            sx={{ mt: 2, py: 1.2, fontWeight: "bold" }}
            onClick={handleLogin}
            disabled={loading}
          >
            {loading ? <CircularProgress size={24} color="inherit" /> : "Sign In"}
          </Button>

          <Typography textAlign="center" sx={{ mt: 2 }}>
            <Link
              component="button"
              variant="body2"
              onClick={() => alert("Forgot Password functionality coming soon.")}
            >
              Forgot Password?
            </Link>
          </Typography>

          <Typography textAlign="center" sx={{ mt: 1 }}>
            <Link
              component="button"
              variant="body2"
              onClick={() => navigate("/auth/create-account")}
            >
              Don't have an account? Create One
            </Link>
          </Typography>

          {message && (
            <Typography
              color={message.includes("✅") ? "success.main" : "error.main"}
              textAlign="center"
              sx={{ mt: 2 }}
            >
              {message}
            </Typography>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default LoginPage;
