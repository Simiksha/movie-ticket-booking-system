import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function RequireAdmin({ children }) {
  const { isAuthed, role } = useAuth();
  const isAdmin = isAuthed && String(role || "").toUpperCase() === "ADMIN";
  if (!isAdmin) return <Navigate to="/movies" replace />;
  return children;
}