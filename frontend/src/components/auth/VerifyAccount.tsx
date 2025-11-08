import React, { useState } from "react";
import { TextField, Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import logo from "../../assets/logo.png";
import authService from "../../services/authService"; // adjust path for dummy api


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
            setMessage("Please enter a 6-digit code.");
            return;
        }

        setLoading(true);
        try {
            const res: any = await authService.verify(email, verificationCode);
            setMessage(res.message);
            setTimeout(() => navigate("/auth/login"), 1500);
        } catch (err: any) {
            setMessage(err.message || "Verification failed.");
        } finally {
            setLoading(false);
        }
    };


    const handleResend = async () => {
        setLoading(true);
        try {
            const res: any = await authService.resend(email);
            setMessage(res.message || "Verification code resent.");
        } catch (err: any) {
            setMessage(err.message || "Resend failed.");
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
                        onChange={(e) => setVerificationCode(e.target.value)}
                        margin="normal"
                    />
                    <Button
                        fullWidth
                        variant="contained"
                        sx={{ mt: 2 }}
                        onClick={handleVerify}
                        disabled={loading}
                    >
                        Verify
                    </Button>

                    <Button
                        fullWidth
                        variant="outlined"
                        sx={{ mt: 1 }}
                        onClick={handleResend}
                        disabled={loading}
                    >
                        Resend Code
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

export default VerifyAccount;
