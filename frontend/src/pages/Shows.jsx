import { useEffect, useMemo, useState } from "react";
import api from "../api/axios";
import { Link, useParams, useSearchParams } from "react-router-dom";

function formatDateTime(iso) {
  if (!iso) return "—";

  const d = new Date(iso);
  if (isNaN(d.getTime())) return String(iso);

  return d.toLocaleString("en-GB", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });
}

function groupByTheater(shows) {
  const map = new Map();
  for (const s of shows) {
    const key = s.theaterName || "Unknown Theater";
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(s);
  }
  for (const [k, arr] of map) {
    arr.sort((a, b) => String(a.showTime).localeCompare(String(b.showTime)));
    map.set(k, arr);
  }
  return Array.from(map.entries());
}

export default function Shows() {
  const { movieId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const [shows, setShows] = useState([]);
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(true);

  const today = new Date().toISOString().slice(0, 10);

  const [date, setDate] = useState(
    () => searchParams.get("date") || new Date().toISOString().slice(0, 10)
  );

  const city = searchParams.get("city") || "";


  useEffect(() => {
    if (!movieId) return;

    (async () => {
      try {
        setErr("");
        setLoading(true);

        const safeDate = date < today ? today : date;

        if (safeDate !== date) {
          setDate(safeDate);
          const next = {};
          next.date = safeDate;
          if (city) next.city = city;
          setSearchParams(next);
          return;
        }

        const params = new URLSearchParams();
        params.set("movieId", movieId);
        params.set("date", safeDate);
        params.set("page", "0");
        params.set("size", "50");
        if (city) params.set("city", city);

        const res = await api.get(`/shows?${params.toString()}`);

        const data = Array.isArray(res.data)
          ? res.data
          : Array.isArray(res.data?.content)
            ? res.data.content
            : [];

        setShows(data);
      } catch (e) {
        setErr(e?.response?.data?.message || "Failed to load shows");
      } finally {
        setLoading(false);
      }
    })();
  }, [movieId, date, city, today, setSearchParams]);

  const movieTitle = shows?.[0]?.movieTitle || "Shows";
  const theaters = useMemo(() => groupByTheater(shows), [shows]);

  if (loading) return <div className="container">Loading shows...</div>;

  return (
    <div className="container">
      <div
        className="row"
        style={{ justifyContent: "space-between", alignItems: "flex-end" }}
      >
        <div>
          <div className="h1">{movieTitle}</div>
          <div className="sub">Select a theater and showtime for {date}.</div>
        </div>

        <div className="row" style={{ gap: 10, flexWrap: "wrap" }}>
          <input
            className="input"
            type="date"
            value={date}
            min={today}
            onChange={(e) => {
              const v = e.target.value;
              const safe = v < today ? today : v;
              setDate(safe);

              const next = {};
              next.date = safe;
              if (city) next.city = city;
              setSearchParams(next);
            }}
            style={{ width: 180 }}
          />

          <Link className="btn" to="/movies">
            ← Back to Movies
          </Link>
        </div>
      </div>

      {err && <div className="err">{err}</div>}

      {!err && shows.length === 0 && (
        <div className="card">
          <b>No shows found for {date}{city ? ` in ${city}` : ""}.</b>
          <div style={{ color: "var(--muted)", marginTop: 6 }}>
            Try another date{city ? " or city" : ""}.
          </div>
        </div>
      )}

      <div className="grid" style={{ marginTop: 14 }}>
        {theaters.map(([theaterName, list]) => {
          const theaterCity = list?.[0]?.city;

          return (
            <div key={theaterName} className="card">
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: 12,
                  flexWrap: "wrap",
                }}
              >
                <div>
                  <div style={{ fontWeight: 850, fontSize: 16 }}>
                    {theaterName}
                  </div>
                  <div
                    style={{
                      color: "var(--muted)",
                      fontSize: 13,
                      marginTop: 4,
                      display: "flex",
                      gap: 8,
                      flexWrap: "wrap",
                      alignItems: "center",
                    }}
                  >
                    {theaterCity ? <span className="pill">{theaterCity}</span> : null}
                    <span>
                      {list.length} show{list.length === 1 ? "" : "s"} available
                    </span>
                  </div>
                </div>

                <span className="pill">
                  From ₹{Math.min(...list.map((s) => Number(s.price || 0)))}
                </span>
              </div>

              <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
                {list.map((s) => {
                  const started = new Date(s.showTime).getTime() <= Date.now();

                  return (
                    <div
                      key={s.id}
                      style={{
                        border: "1px solid var(--line)",
                        borderRadius: 14,
                        padding: 12,
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        gap: 12,
                        flexWrap: "wrap",
                      }}
                    >
                      <div style={{ display: "grid", gap: 4 }}>
                        <div style={{ fontWeight: 800 }}>
                          {formatDateTime(s.showTime)}
                        </div>
                        <div style={{ color: "var(--muted)", fontSize: 13 }}>
                          {s.screenName} • ₹{s.price}
                        </div>
                      </div>

                      {started ? (
                        <button
                          className="btn"
                          disabled
                          style={{ opacity: 0.6, cursor: "not-allowed" }}
                        >
                          Booking Closed
                        </button>
                      ) : (
                        <Link className="btn btnPrimary" to={`/shows/${s.id}/seats`}>
                          Select Seats
                        </Link>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}