import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./components/auth/LoginPage";
import CreateAccount from "./components/auth/CreateAccount";
import VerifyAccount from "./components/auth/VerifyAccount";

import StudentDashboard from "./components/student/StudentDashboard";
import SubmitGrievance from "./components/student/SubmitGrievance";
import TrackGrievance from "./components/student/TrackGrievance";

import FacultyDashboard from "./components/faculty/FacultyDashboard";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <Router>
      <Routes>
        {/* Default redirect to login */}
        <Route path="/" element={<Navigate to="/auth/login" replace />} />

        {/* Public routes */}
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/create-account" element={<CreateAccount />} />
        <Route path="/auth/verify" element={<VerifyAccount />} />

        {/* Protected Student Routes */}
        <Route
          path="/auth/student-dashboard"
          element={
            <ProtectedRoute>
              <StudentDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/submit-grievance"
          element={
            <ProtectedRoute>
              <SubmitGrievance />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/track-grievance"
          element={
            <ProtectedRoute>
              <TrackGrievance />
            </ProtectedRoute>
          }
        />

        {/* Protected Faculty Route */}
        <Route
          path="/faculty/dashboard"
          element={
            <ProtectedRoute>
              <FacultyDashboard />
            </ProtectedRoute>
          }
        />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/auth/login" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
