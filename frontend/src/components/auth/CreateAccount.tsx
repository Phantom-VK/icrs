import React, { useState } from "react";
import { TextField, Button, Card, CardContent, Typography, Box, Link } from "@mui/material";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import logo from "../../assets/logo.png";
import authService from "../../services/authService"; // adjust path for dummy api

const CreateAccount: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullname: "",
    studentId: "",
    department: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleCreateAccount = async () => {
    if (formData.password !== formData.confirmPassword) {
      setMessage("Passwords do not match.");
      return;
    }
    setLoading(true);
    try {
      const res: any = await authService.register(
        formData.fullname,
        formData.studentId,
        formData.department,
        formData.email,
        formData.password
      );
      setMessage(res.message);
      setTimeout(() => navigate(`/auth/verify?email=${formData.email}`), 1000);
    } catch (err: any) {
      setMessage(err.message || "Signup failed.");
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
      <Card sx={{ width: "100%", maxWidth: 500, p: 3 }}>
        <Box textAlign="center" mb={2}>
          <img src={logo} alt="College Logo" style={{ width: "100%", height: "auto" }} />
          <Typography variant="h6" fontWeight="bold">
            SGGS COLLEGE REDRESSAL SYSTEM
          </Typography>
        </Box>
        <CardContent>
          <TextField fullWidth label="Full Name" name="fullname" margin="normal" value={formData.fullname} onChange={handleChange} />
          <TextField fullWidth label="Student ID" name="studentId" margin="normal" value={formData.studentId} onChange={handleChange} />
          <TextField fullWidth label="Department" name="department" margin="normal" value={formData.department} onChange={handleChange} />
          <TextField fullWidth label="College Email" name="email" margin="normal" value={formData.email} onChange={handleChange} />
          <TextField fullWidth label="Password" type="password" name="password" margin="normal" value={formData.password} onChange={handleChange} />
          <TextField fullWidth label="Confirm Password" type="password" name="confirmPassword" margin="normal" value={formData.confirmPassword} onChange={handleChange} />

          <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={handleCreateAccount} disabled={loading}>
            Create Account
          </Button>

          {message && (
            <Typography color="error" textAlign="center" sx={{ mt: 2 }}>
              {message}
            </Typography>
          )}

          <Typography textAlign="center" sx={{ mt: 2 }}>
            <Link component="button" variant="body2" onClick={() => navigate("/auth/login")}>
              Already have an account? Sign In
            </Link>
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default CreateAccount;
