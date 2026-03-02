import { Routes, Route, Navigate, Link } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import { useAuth } from "./auth/AuthContext";
import Movies from "./pages/Movies";
import Shows from "./pages/Shows";
import ShowSeatsPage from "./pages/ShowSeatsPage";
import Payment from "./pages/Payment";
import MyBookings from "./pages/MyBookings";
import Navbar from "./components/Navbar";
import AdminRoute from "./auth/AdminRoute";
import AdminDashboard from "./pages/AdminDashboard";
import AdminMovies from "./pages/admin/AdminMovies";
import AdminTheaters from "./pages/admin/AdminTheaters";
import AdminShows from "./pages/admin/AdminShows";
import RequireAuth from "./auth/RequiredAuth";

export default function App() {
  const { isAuthed, role, email, logout } = useAuth();

  return (
    <>
      <Navbar />

      <Routes>
        <Route path="/" element={<Navigate to="/movies" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route path="/movies" element={<Movies />} />
        <Route path="/movies/:movieId/shows" element={<Shows />} />

        <Route path="/shows" element={<Shows />} />
        <Route path="/shows/:showId/seats" element={<RequireAuth><ShowSeatsPage /></RequireAuth>} />

        <Route path="/bookings/:bookingId/pay" element={<RequireAuth><Payment /></RequireAuth>} />
        <Route path="/my-bookings" element={<MyBookings />} />
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminDashboard />
            </AdminRoute>
          }
        />

        <Route
          path="/admin/movies"
          element={
            <AdminRoute>
              <AdminMovies />
            </AdminRoute>
          }
        />

        <Route
          path="/admin/theaters"
          element={
            <AdminRoute>
              <AdminTheaters />
            </AdminRoute>
          }
        />

        <Route
          path="/admin/shows"
          element={
            <AdminRoute>
              <AdminShows />
            </AdminRoute>
          }
        />
      </Routes>
    </>
  );
}