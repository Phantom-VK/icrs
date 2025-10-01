import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./components/auth/LoginPage";
import CreateAccount from "./components/auth/CreateAccount";
import StudentDashboard from "./components/student/StudentDashboard";
import SubmitGrievance from "./components/student/SubmitGrievance";
import TrackGrievance from "./components/student/TrackGrievance";
import FacultyDashboard from "./components/faculty/FacultyDashboard";
import VerifyAccount from "./components/auth/VerifyAccount";

function App() {
  return (
    <Router>
      <Routes>
        {/* Default redirect to login */}
        <Route path="/" element={<Navigate to="/auth/login" replace />} />

        {/* Authentication */}
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/create-account" element={<CreateAccount />} />
        <Route path="/auth/verify" element={<VerifyAccount />} />
 

        {/* Student Pages */}
        <Route path="/auth/student-dashboard" element={<StudentDashboard />} />
        <Route path="/student/submit-grievance" element={<SubmitGrievance />} />
        <Route path="/student/track-grievance" element={<TrackGrievance />} />

        {/* Faculty Pages */}
        <Route path="/faculty/dashboard" element={<FacultyDashboard />} />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/auth/login" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
