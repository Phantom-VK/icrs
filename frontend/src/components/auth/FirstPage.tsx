import React from "react";
import { Button, Card, CardContent, Typography, Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";
const FirstPage: React.FC = () => {
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
      <Card sx={{ width: "100%", maxWidth: 400, p: 3, textAlign: "center" }}>
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
          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={() => navigate("/auth/student-portal")}
          >
            Submit New Grievance
          </Button>
          <Typography sx={{ mt: 2, mb: 2 }}>OR</Typography>
          <Button fullWidth variant="outlined" sx={{ mt: 2 }}>
            Your Grievances
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default FirstPage;
