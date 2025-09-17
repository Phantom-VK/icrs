import React from "react";
import { TextField, Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";
const LoginPage: React.FC = () => {
  const navigate = useNavigate();

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
          <TextField fullWidth label="College Email" margin="normal" />
          <TextField fullWidth label="Password" type="password" margin="normal" />

          {/* Sign In */}
          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={() => navigate("/auth/first-page")}
          >
            Sign In
          </Button>

          {/* Forgot Password (UI only for now) */}
          <Button
            fullWidth
            variant="text"
            sx={{ mt: 1 }}
            onClick={() => alert("Forgot Password functionality not implemented yet.")}
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
