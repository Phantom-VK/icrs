import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./components/auth/LoginPage";
import CreateAccount from "./components/auth/CreateAccount";
import FirstPage from "./components/auth/FirstPage";
import StudentPortal from "./components/auth/StudentPortal";
import FacultyGrievances from "./components/faculty/FacultyGrievances";

function App() {
  return (
    <Router>
      <Routes>
        {/* Default: go to login */}
        <Route path="/" element={<Navigate to="/auth/login" replace />} />
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/create-account" element={<CreateAccount />} />
        <Route path="/auth/first-page" element={<FirstPage />} />
        <Route path="/auth/student-portal" element={<StudentPortal />} />
        <Route path="/faculty/grievances" element={<FacultyGrievances />} />
      </Routes>
    </Router>
  );
}

export default App;
