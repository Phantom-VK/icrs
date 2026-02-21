import React, { useState, type ChangeEvent } from "react";
import { TextField, Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import logo from "../../assets/logo.png";
import authService from "../../services/authService";
import axios from "axios";

const VerifyAccount: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Get email from query params (passed from CreateAccount)
  const queryParams = new URLSearchParams(location.search);
  const email = queryParams.get("email") || "";

  const [verificationCode, setVerificationCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const handleVerify = async () => {
    if (verificationCode.length !== 6) {
      setMessage("Please enter a valid 6-digit verification code.");
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      const res = await authService.verify(email, verificationCode);
      setMessage(res.message || "Account verified successfully!");
      setTimeout(() => navigate("/auth/login"), 1500);
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        if (error.response?.data) {
          setMessage(String(error.response.data));
        } else {
          setMessage("Network error. Please try again.");
        }
      } else {
        setMessage("Unexpected error occurred during verification.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setLoading(true);
    setMessage("");

    try {
      const res = await authService.resend(email);
      setMessage(res.message || "Verification code resent successfully!");
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        if (error.response?.data) {
          setMessage(String(error.response.data));
        } else {
          setMessage("Network error while resending code.");
        }
      } else {
        setMessage("Unexpected error occurred while resending code.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCodeChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, ""); // numeric only
    if (value.length <= 6) setVerificationCode(value);
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
            Verify Your Account
          </Typography>
          <Typography variant="body2" color="textSecondary">
            A 6-digit verification code has been sent to <b>{email}</b>
          </Typography>
        </Box>
        <CardContent>
          <TextField
            fullWidth
            label="Verification Code"
            value={verificationCode}
            onChange={handleCodeChange}
            margin="normal"
            inputProps={{ maxLength: 6 }}
          />

          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={handleVerify}
            disabled={loading}
          >
            {loading ? "Verifying..." : "Verify"}
          </Button>

          <Button
            fullWidth
            variant="outlined"
            sx={{ mt: 1 }}
            onClick={handleResend}
            disabled={loading}
          >
            {loading ? "Resending..." : "Resend Code"}
          </Button>

          {message && (
            <Typography
              color={
                message.toLowerCase().includes("success")
                  ? "primary"
                  : "error"
              }
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

export default VerifyAccount;
