import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./components/auth/LoginPage";
import CreateAccount from "./components/auth/CreateAccount";
import FirstPage from "./components/auth/FirstPage";
import StudentPortal from "./components/auth/StudentPortal";

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
      </Routes>
    </Router>
  );
}

export default App;
