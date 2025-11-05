import React, { useState } from "react";
import { TextField, Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import logo from "../../assets/logo.png";
<<<<<<< HEAD
import authService from "../../services/authService"; // adjust path for dummy api
=======
import authService from "../../services/authService"; 
>>>>>>> master


const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setLoading(true);
    try {
      const res: any = await authService.login(email, password);

      if (!res.verified) {
        setMessage("Your account is not verified. Redirecting...");
        setTimeout(() => navigate(`/auth/verify?email=${email}`), 1000);
        return;
      }

      localStorage.setItem("token", res.token);
      navigate("/auth/student-dashboard");
    } catch (err: any) {
      setMessage(err.message || "Login failed.");
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
          <TextField fullWidth label="College Email" margin="normal" value={email} onChange={(e) => setEmail(e.target.value)} />
          <TextField fullWidth label="Password" type="password" margin="normal" value={password} onChange={(e) => setPassword(e.target.value)} />

          <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={handleLogin} disabled={loading}>
            Sign In
          </Button>

          <Button fullWidth variant="text" sx={{ mt: 1 }} onClick={() => alert("Forgot Password functionality not implemented yet.")}>
            Forgot Password?
          </Button>

          {/* Create Account */}
          <Button
            fullWidth
<<<<<<< HEAD
            variant="contained"      // same style as Sign In
=======
            variant="contained"    
>>>>>>> master
            sx={{ mt: 1, backgroundColor: "#0288d1", "&:hover": { backgroundColor: "#0277bd" } }}
            onClick={() => navigate("/auth/create-account")}
          >
            Create Account
          </Button>

          {message && (
            <Typography color="error" textAlign="center" sx={{ mt: 2 }}>
              {message}
            </Typography>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default LoginPage;
