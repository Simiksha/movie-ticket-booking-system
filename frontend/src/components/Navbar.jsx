import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function Navbar() {
  const { isAuthed, role, email, logout } = useAuth();
  const nav = useNavigate();
  const isAdmin = String(role || "").toUpperCase() === "ADMIN";

  function onLogout() {
    logout();
    nav("/movies"); 
  }

  return (
    <div className="nav">
      <div className="navInner">
        <div className="brand">
          <span className="brandDot" />
          Movie Booking
        </div>

        <div className="navLinks">
          <Link className="navLink" to="/movies">Movies</Link>

          {/* USER links */}
          {!isAdmin && (
            <Link className="navLink" to="/my-bookings">My Bookings</Link>
          )}

          {/* ADMIN links */}
          {isAdmin && (
            <Link className="navLink" to="/admin">Admin</Link>
          )}

          {!isAuthed ? (
            <Link className="navLink" to="/login">Login</Link>
          ) : (
            <>
              <span className="navLink" style={{ cursor: "default" }}>
                {email} {isAdmin ? "(ADMIN)" : ""}
              </span>
              <button className="btn btnPrimary" onClick={onLogout}>
                Logout
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}