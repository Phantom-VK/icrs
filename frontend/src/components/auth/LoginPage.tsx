import React, { useState } from "react";
import { TextField, Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import logo from "../../assets/logo.png";

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setLoading(true);
    setMessage("");

    try {
      // 🔗 Call your Spring Boot API
      const response = await axios.post("http://localhost:8080/auth/login", {
        email,
        password,
      });

      // 🧾 Backend returns token and expiry
      const { token, expiresIn } = response.data;

      if (!token) {
        setMessage("Invalid login response from server.");
        return;
      }

      // 💾 Store JWT token locally for authenticated requests
      localStorage.setItem("token", token);
      localStorage.setItem("tokenExpiry", expiresIn);

      setMessage("Login successful!");
      setTimeout(() => navigate("/auth/student-dashboard"), 1000);
    } catch (error: any) {
      if (error.response && error.response.status === 403) {
        setMessage("Access denied. Please verify your email first.");
      } else if (error.response && error.response.status === 401) {
        setMessage("Invalid email or password.");
      } else {
        setMessage("Login failed. Please try again.");
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
      <Card sx={{ width: "100%", maxWidth: 400, p: 3 }}>
        <Box textAlign="center" mb={2}>
          <img src={logo} alt="College Logo" style={{ width: "100%", height: "auto" }} />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>
        <CardContent>
          <TextField
            fullWidth
            label="College Email"
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
            sx={{ mt: 2 }}
            onClick={handleLogin}
            disabled={loading}
          >
            {loading ? "Signing In..." : "Sign In"}
          </Button>

          <Button
            fullWidth
            variant="text"
            sx={{ mt: 1 }}
            onClick={() => alert("Forgot Password functionality not implemented yet.")}
          >
            Forgot Password?
          </Button>

          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 1, backgroundColor: "#0288d1", "&:hover": { backgroundColor: "#0277bd" } }}
            onClick={() => navigate("/auth/create-account")}
          >
            Create Account
          </Button>

          {message && (
            <Typography color={message.includes("successful") ? "primary" : "error"} textAlign="center" sx={{ mt: 2 }}>
              {message}
            </Typography>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default LoginPage;
