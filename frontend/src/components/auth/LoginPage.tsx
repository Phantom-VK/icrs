// components/auth/LoginPage.tsx
import React, { useState } from "react";
import { TextField, Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = () => {
    // Hardcoded faculty credentials (temporary until backend)
    const facultyEmail = "itfaculty01@sggs.ac.in";
    const facultyPassword = "itdepartment01";

    if (email === facultyEmail && password === facultyPassword) {
      // Faculty → Faculty grievance page
      navigate("/faculty/grievances");
    } else {
      // Students (any email/password for now) → Student flow
      navigate("/auth/first-page");
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
          {/* Email */}
          <TextField
            fullWidth
            label="College Email"
            margin="normal"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          {/* Password */}
          <TextField
            fullWidth
            label="Password"
            type="password"
            margin="normal"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {/* Sign In */}
          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={handleLogin}
          >
            Sign In
          </Button>

          {/* Forgot Password (UI only) */}
          <Button
            fullWidth
            variant="text"
            sx={{ mt: 1 }}
            onClick={() =>
              alert("Forgot Password functionality not implemented yet.")
            }
          >
            Forgot Password?
          </Button>

          {/* Create Account */}
          <Button
            fullWidth
            variant="outlined"
            sx={{ mt: 1 }}
            onClick={() => navigate("/auth/create-account")}
          >
            Create Account
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default LoginPage;
