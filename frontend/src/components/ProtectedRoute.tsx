import React from "react";
import { Navigate } from "react-router-dom";

const ProtectedRoute = ({ children }: { children: React.ReactElement }) => {
  const token = localStorage.getItem("token");
  const expiry = Number(localStorage.getItem("tokenExpiry") || 0);
  const expired = expiry && Date.now() > expiry;

  if (!token || expired) {
    localStorage.removeItem("token");
    localStorage.removeItem("tokenExpiry");
    return <Navigate to="/auth/login" replace />;
  }
  return children;
};

export default ProtectedRoute;
