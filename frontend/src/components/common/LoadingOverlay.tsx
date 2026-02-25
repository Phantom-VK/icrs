import React from "react";

type Props = {
  show: boolean;
  message?: string;
};

const LoadingOverlay: React.FC<Props> = ({ show, message = "Loading..." }) => {
  if (!show) return null;
  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.35)",
        zIndex: 2000,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        color: "#fff",
        fontSize: "18px",
        fontWeight: 600,
        backdropFilter: "blur(2px)",
      }}
    >
      <div
        style={{
          background: "rgba(0,0,0,0.6)",
          padding: "16px 24px",
          borderRadius: "10px",
          boxShadow: "0 8px 24px rgba(0,0,0,0.3)",
        }}
      >
        {message}
      </div>
    </div>
  );
};

export default LoadingOverlay;
