import { useEffect, useMemo, useState } from "react";
import api from "../../api/axios";

function todayISO() {
    return new Date().toISOString().slice(0, 10);
}

export default function AdminShows() {
    const [err, setErr] = useState("");
    const [ok, setOk] = useState("");

    // dropdown data
    const [movies, setMovies] = useState([]);
    const [theaters, setTheaters] = useState([]);
    const [screens, setScreens] = useState([]);

    // selections 
    const [movieId, setMovieId] = useState("");
    const [theaterId, setTheaterId] = useState("");
    const [screenId, setScreenId] = useState("");

    // bulk params 
    const [startDate, setStartDate] = useState("");
    const [days, setDays] = useState("");
    const [times, setTimes] = useState("");
    const [price, setPrice] = useState("");

    // shows list
    const [shows, setShows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const parsedTimes = useMemo(
        () => times.split(",").map((s) => s.trim()).filter(Boolean),
        [times]
    );

    async function loadShows() {
        const res = await api.get("/admin/shows");
        setShows(Array.isArray(res.data) ? res.data : []);
    }

    async function loadMovies() {
        const res = await api.get("/movies");
        const data = Array.isArray(res.data)
            ? res.data
            : Array.isArray(res.data?.content)
                ? res.data.content
                : [];
        setMovies(data);
    }

    async function loadTheaters() {
        const res = await api.get("/admin/theaters");
        const data = Array.isArray(res.data) ? res.data : [];
        setTheaters(data);
    }

    async function loadScreens(tId) {
        if (!tId) {
            setScreens([]);
            setScreenId("");
            return;
        }
        const res = await api.get(`/admin/theaters/${tId}/screens`);
        const data = Array.isArray(res.data) ? res.data : [];
        setScreens(data);
    }

    useEffect(() => {
        (async () => {
            try {
                setErr("");
                setLoading(true);
                await Promise.all([loadMovies(), loadTheaters()]);
                await loadShows();
            } catch (e) {
                setErr(e?.response?.data?.message || "Failed to load admin data");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    // load screens when theater changes
    useEffect(() => {
        (async () => {
            try {
                setErr("");
                await loadScreens(theaterId);
            } catch (e) {
                setErr(e?.response?.data?.message || "Failed to load screens");
            }
        })();
    }, [theaterId]);

    async function bulkCreate(e) {
        e.preventDefault();
        setErr("");
        setOk("");

        try {
            setSaving(true);

            if (!movieId) throw new Error("Select a movie");
            if (!theaterId) throw new Error("Select a theater");
            if (!screenId) throw new Error("Select a screen");
            if (!startDate) throw new Error("Select start date");
            if (!days || Number(days) < 1) throw new Error("Days must be >= 1");
            if (!price || Number(price) < 1) throw new Error("Price must be >= 1");
            if (!times.trim()) throw new Error("Enter at least one time");

            const payload = {
                movieId: Number(movieId),
                screenId: Number(screenId),
                startDate,
                days: Number(days),
                times: parsedTimes,
                price: Number(price),
            };

            const res = await api.post("/admin/shows/bulk", payload);

            setOk(`Shows Added for ${res.data} days✅ )`);
            await loadShows();

            // reset form
            setMovieId("");
            setTheaterId("");
            setScreenId("");
            setStartDate("");
            setDays("");
            setTimes("");
            setPrice("");
            setScreens([]);
        } catch (e2) {
            setErr(e2?.response?.data?.message || e2.message || "Bulk create failed");
        } finally {
            setSaving(false);
        }
    }

    async function remove(showId) {
        if (!window.confirm("Delete this show?")) return;

        try {
            setErr("");
            setOk("");
            await api.delete(`/admin/shows/${showId}`);
            setOk("Show deleted ✅");
            await loadShows();
        } catch (e) {
            setErr(e?.response?.data?.message || "Delete failed");
        }
    }

    if (loading) return <div className="container">Loading...</div>;

    return (
        <div className="container">
            <div className="h1">Admin • Shows</div>
            <div className="sub">Add shows.</div>

            {err && <div className="err">{err}</div>}
            {ok && <div className="card" style={{ borderColor: "var(--ok)" }}>{ok}</div>}

            <div className="grid" style={{ marginTop: 14 }}>
                <div className="card">
                    <div style={{ fontWeight: 900, marginBottom: 10 }}>Bulk Create Shows</div>

                    <form onSubmit={bulkCreate} className="grid" style={{ gap: 12 }}>
                        <div>
                            <div className="label">Movie</div>
                            <select className="input" value={movieId} onChange={(e) => setMovieId(e.target.value)}>
                                <option value="">Select movie</option>
                                {movies.map((m) => (
                                    <option key={m.id} value={m.id}>
                                        {m.title}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="row" style={{ gap: 12 }}>
                            <div style={{ flex: 1 }}>
                                <div className="label">Theater</div>
                                <select className="input" value={theaterId} onChange={(e) => setTheaterId(e.target.value)}>
                                    <option value="">Select theater</option>
                                    {theaters.map((t) => (
                                        <option key={t.id} value={t.id}>
                                            {t.name} • {t.city}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div style={{ flex: 1 }}>
                                <div className="label">Screen</div>
                                <select
                                    className="input"
                                    value={screenId}
                                    onChange={(e) => setScreenId(e.target.value)}
                                    disabled={!theaterId}
                                >
                                    <option value="">
                                        {theaterId ? "Select screen" : "Select theater first"}
                                    </option>
                                    {screens.map((s) => (
                                        <option key={s.id} value={s.id}>
                                            {s.name} • {s.totalSeats} seats
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="row" style={{ gap: 12 }}>
                            <div style={{ flex: 1 }}>
                                <div className="label">Start Date</div>
                                <input className="input" type="date" min={todayISO()} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                            </div>
                            <div style={{ width: 160 }}>
                                <div className="label">Days</div>
                                <input className="input" type="number" min={1} value={days} onChange={(e) => setDays(e.target.value)} />
                            </div>
                            <div style={{ width: 160 }}>
                                <div className="label">Price</div>
                                <input className="input" type="number" min={1} value={price} onChange={(e) => setPrice(e.target.value)} />
                            </div>
                        </div>

                        <div>
                            <div className="label">Show Times (comma separated)</div>
                            <input className="input" placeholder="10:00,14:00,18:00" value={times} onChange={(e) => setTimes(e.target.value)} />
                        </div>

                        <button
                            className="btn btnPrimary"
                            disabled={saving || !movieId || !screenId || parsedTimes.length === 0}
                        >
                            {saving ? "Creating..." : "Create Shows"}
                        </button>

                        {saving && <div className="sub" style={{ marginTop: 8 }}>Please wait…</div>}
                    </form>
                </div>

                <div className="card">
                    <div style={{ fontWeight: 900, marginBottom: 10 }}>All Shows</div>

                    <div style={{ display: "grid", gap: 10 }}>
                        {shows.map((s) => (
                            <div
                                key={s.id}
                                style={{
                                    border: "1px solid var(--line)",
                                    borderRadius: 14,
                                    padding: 12,
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "center",
                                    gap: 10,
                                    flexWrap: "wrap",
                                }}
                            >
                                <div style={{ display: "grid", gap: 4 }}>
                                    <div style={{ fontWeight: 900 }}>{s.movieTitle}</div>
                                    <div style={{ color: "var(--muted)", fontSize: 13 }}>
                                        #{s.id} • {s.theaterName} • {s.screenName} • {String(s.showTime).replace("T", " ")} • ₹{s.price}
                                    </div>
                                </div>
                            </div>
                        ))}

                        {shows.length === 0 && <div style={{ color: "var(--muted)" }}>No shows.</div>}
                    </div>
                </div>
            </div>
        </div>
    );
}