import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function AdminRoute({ children }) {
  const { isAuthed, role } = useAuth();

  if (!isAuthed) return <Navigate to="/login" replace />;
  if (String(role).toUpperCase() !== "ADMIN") return <Navigate to="/movies" replace />;

  return children;
}