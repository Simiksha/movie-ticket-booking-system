import { useEffect, useMemo, useState } from "react";
import api from "../api/axios";
import { Link } from "react-router-dom";

function safeText(v, fallback = "—") {
  if (v === null || v === undefined) return fallback;
  const s = String(v).trim();
  return s.length ? s : fallback;
}

function normalizeGenres(movie) {
  if (Array.isArray(movie?.genres)) return Array.from(movie.genres).join(", ");
  if (typeof movie?.genre === "string") return movie.genre;
  return "";
}

export default function Movies() {
  const [movies, setMovies] = useState([]);
  const [cities, setCities] = useState([]);
  const [city, setCity] = useState("");

  const [q, setQ] = useState("");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const today = new Date().toISOString().slice(0, 10);
  const safeDate = date < today ? today : date;

  useEffect(() => {
    (async () => {
      try {
        const res = await api.get("/theaters/cities");
        setCities(Array.isArray(res.data) ? res.data : []);
      } catch {
        setCities([]);
      }
    })();
  }, []);

  // Load movies based on date + city
  useEffect(() => {
    (async () => {
      try {
        setErr("");
        setLoading(true);

        const params = new URLSearchParams();
        params.set("page", "0");
        params.set("size", "60"); // adjust if needed
        params.set("date", safeDate);
        if (city) params.set("city", city);

        const res = await api.get(`/movies?${params.toString()}`);

        const data =
          Array.isArray(res.data) ? res.data :
          Array.isArray(res.data?.data) ? res.data.data :
          Array.isArray(res.data?.content) ? res.data.content :
          Array.isArray(res.data?.movies) ? res.data.movies :
          [];

        setMovies(data);
      } catch (e) {
        setErr(e?.response?.data?.message || "Failed to load movies");
      } finally {
        setLoading(false);
      }
    })();
  }, [safeDate, city]);

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return movies;

    return movies.filter((m) => {
      const hay = [
        m?.title,
        m?.language,
        normalizeGenres(m),
        m?.rating,
        m?.description,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return hay.includes(term);
    });
  }, [movies, q]);

  if (loading) return <div className="container">Loading movies...</div>;

  return (
    <div className="container">
      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-end" }}>
        <div>
          <div className="h1">Movies</div>
          <div className="sub">
            Choose a city, pick a date, and book your seats.
          </div>
        </div>

        <div className="row" style={{ gap: 10, flexWrap: "wrap" }}>
          <input
            className="input"
            type="date"
            value={date}
            min={today}
            onChange={(e) => setDate(e.target.value)}
            style={{ width: 180 }}
          />

          <select
            className="input"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            style={{ width: 180 }}
          >
            <option value="">All cities</option>
            {cities.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>

          <input
            className="input"
            placeholder="Search by title, genre, language..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
            style={{ width: 320, maxWidth: "100%" }}
          />
        </div>
      </div>

      {err && <div className="err">{err}</div>}

      {!err && filtered.length === 0 && (
        <div className="card">
          <b>No movies found{city ? ` in ${city}` : ""} for {safeDate}.</b>
          <div style={{ color: "var(--muted)", marginTop: 6 }}>
            Try another date{city ? " or city" : ""}, or change the search term.
          </div>
        </div>
      )}

      <div className="grid grid3" style={{ marginTop: 16 }}>
        {filtered.map((movie) => {
          const poster = movie?.posterUrl || movie?.poster || "";
          const genres = normalizeGenres(movie);

          const cityParam = city ? `&city=${encodeURIComponent(city)}` : "";

          return (
            <div key={movie.id} className="card" style={{ overflow: "hidden", padding: 0 }}>
              <div
                style={{
                  width: "100%",
                  maxWidth: 180,
                  margin: "0 auto",
                  aspectRatio: "2 / 3",
                  background: "#f3f4f6",
                  overflow: "hidden",
                  borderRadius: 12,
                }}
              >
                {poster ? (
                  <img
                    src={poster}
                    alt={safeText(movie.title, "Movie poster")}
                    style={{
                      width: "100%",
                      height: "100%",
                      objectFit: "cover",
                      objectPosition: "center",
                      display: "block",
                    }}
                  />
                ) : (
                  <div style={{ height: "100%", display: "grid", placeItems: "center", color: "var(--muted)" }}>
                    No Poster
                  </div>
                )}
              </div>

              <div style={{ padding: 16 }}>
                <div style={{ display: "flex", gap: 10, justifyContent: "space-between", alignItems: "flex-start" }}>
                  <div style={{ fontWeight: 800, fontSize: 16, lineHeight: 1.2 }}>
                    {safeText(movie.title)}
                  </div>
                  {movie?.rating ? <span className="pill">{movie.rating}</span> : null}
                </div>

                <div style={{ color: "var(--muted)", marginTop: 8, fontSize: 13, display: "grid", gap: 6 }}>
                  {genres ? <div><b>Genres:</b> {genres}</div> : null}
                  {movie?.language ? <div><b>Language:</b> {movie.language}</div> : null}
                  {movie?.duration ? <div><b>Duration:</b> {movie.duration} mins</div> : null}
                  {movie?.releaseDate ? <div><b>Release:</b> {movie.releaseDate}</div> : null}
                </div>

                <div className="row" style={{ marginTop: 12, justifyContent: "space-between" }}>
                  <Link className="btn btnPrimary" to={`/movies/${movie.id}/shows?date=${safeDate}${cityParam}`}>
                    View Shows
                  </Link>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}