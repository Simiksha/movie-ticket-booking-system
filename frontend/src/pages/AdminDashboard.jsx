import { Link } from "react-router-dom";

export default function AdminDashboard() {
  return (
    <div className="container">
      <div className="h1">Admin Dashboard</div>
      <div className="sub">Manage movies, theaters, screens, and shows.</div>

      <div className="grid grid3" style={{ marginTop: 14 }}>
        <div className="card">
          <div style={{ fontWeight: 900, fontSize: 16 }}>Movies</div>
          <div style={{ color: "var(--muted)", marginTop: 6,marginBottom: 20 }}>
            Create, update, and delete movies.
          </div>
          <div style={{ marginTop: 12 }}>
            <Link className="btn btnPrimary" to="/admin/movies">Manage Movies</Link>
          </div>
        </div>

        <div className="card">
          <div style={{ fontWeight: 900, fontSize: 16 }}>Theaters</div>
          <div style={{ color: "var(--muted)", marginTop: 6, marginBottom: 20 }}>
            Create theaters and add screens.
          </div>
          <div style={{ marginTop: 12 }}>
            <Link className="btn btnPrimary" to="/admin/theaters">Manage Theaters</Link>
          </div>
        </div>

        <div className="card">
          <div style={{ fontWeight: 900, fontSize: 16 }}>Shows</div>
          <div style={{ color: "var(--muted)", marginTop: 6, marginBottom: 20 }}>
            Add shows. 
          </div>
          <div style={{ marginTop: 12 }}>
            <Link className="btn btnPrimary" to="/admin/shows">Manage Shows</Link>
          </div>
        </div>
      </div>
    </div>
  );
}