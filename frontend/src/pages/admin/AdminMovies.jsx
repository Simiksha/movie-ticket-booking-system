import { useEffect, useState } from "react";
import api from "../../api/axios";

const GENRES = [
    "ACTION",
    "THRILLER",
    "COMEDY",
    "DRAMA",
    "ROMANCE",
    "HORROR",
    "SCI_FI",
    "FANTASY",
    "ADVENTURE",
    "ANIMATION",
    "CRIME",
    "MYSTERY",
];

const LANGUAGES = [
    "English",
    "Tamil",
    "Telugu",
    "Hindi",
    "Malayalam",
    "Kannada",
];

const RATINGS = [
    "U",
    "UA",
    "A",
    "PG",
    "PG-13",
    "R"
];

const emptyForm = {
    title: "",
    description: "",
    genres: [],
    duration: "",
    language: "",
    rating: "",
    releaseDate: "",
    posterUrl: "",
};

export default function AdminMovies() {
    const [movies, setMovies] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [editId, setEditId] = useState(null);

    const [err, setErr] = useState("");
    const [ok, setOk] = useState("");
    const [loading, setLoading] = useState(true);

    async function load() {
        const res = await api.get("/movies"); // public GET
        const data = Array.isArray(res.data)
            ? res.data
            : Array.isArray(res.data?.content)
                ? res.data.content
                : [];
        setMovies(data);
    }

    useEffect(() => {
        (async () => {
            try {
                setErr("");
                setLoading(true);
                await load();
            } catch (e) {
                setErr(e?.response?.data?.message || "Failed to load movies");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    function setField(k, v) {
        setForm((p) => ({ ...p, [k]: v }));
    }

    function toggleGenre(g) {
        setForm((p) => {
            const set = new Set(p.genres || []);
            if (set.has(g)) set.delete(g);
            else set.add(g);
            return { ...p, genres: Array.from(set) };
        });
    }

    function startCreate() {
        setEditId(null);
        setForm(emptyForm);
        setOk("");
        setErr("");
    }

    function startEdit(m) {
        setEditId(m.id);
        setForm({
            title: m.title || "",
            description: m.description || "",
            genres: Array.isArray(m.genres) ? m.genres : [],
            duration: m.duration ? String(m.duration) : "",
            language: m.language || "",
            rating: m.rating || "",
            releaseDate: (m.releaseDate || "").slice(0, 10),
            posterUrl: m.posterUrl || "",
        });
        setOk("");
        setErr("");
    }

    async function submit(e) {
        e.preventDefault();
        setErr("");
        setOk("");

        try {
            const payload = {
                ...form,
                duration: form.duration ? Number(form.duration) : null,
            };

            if (!payload.title?.trim()) throw new Error("Title is required");
            if (!payload.language) throw new Error("Language is required");
            if (!payload.duration || payload.duration < 1) throw new Error("Duration must be >= 1");
            if (!Array.isArray(payload.genres) || payload.genres.length === 0) throw new Error("Select at least one genre");

            if (editId) {
                await api.put(`/admin/movies/${editId}`, payload);
                setOk("Movie updated ✅");
            } else {
                await api.post("/admin/movies", payload);
                setOk("Movie created ✅");
            }

            await load();
            startCreate();
        } catch (e2) {
            setErr(e2?.response?.data?.message || e2.message || "Save failed");
        }
    }

    async function remove(id) {
        if (!window.confirm("Delete this movie?")) return;

        try {
            setErr("");
            setOk("");
            await api.delete(`/admin/movies/${id}`);
            await load();
            if (editId === id) startCreate();
            setOk("Movie deleted ✅");
        } catch (e) {
            setErr(e?.response?.data?.message || "Delete failed");
        }
    }

    if (loading) return <div className="container">Loading...</div>;

    return (
        <div className="container">
            <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-end" }}>
                <div>
                    <div className="h1">Admin • Movies</div>
                    <div className="sub">Add movies.</div>
                </div>
                <button className="btn" onClick={startCreate}>+ New Movie</button>
            </div>

            {err && <div className="err">{err}</div>}
            {ok && <div className="card" style={{ borderColor: "var(--ok)" }}>{ok}</div>}

            <div className="grid" style={{ marginTop: 14 }}>
                {/* Form */}
                <div className="card">
                    <div style={{ fontWeight: 900, marginBottom: 10 }}>
                        {editId ? `Edit Movie #${editId}` : "Create Movie"}
                    </div>

                    <form onSubmit={submit} className="grid" style={{ gap: 12 }}>
                        <div>
                            <div className="label">Title</div>
                            <input className="input" placeholder="Movie title" value={form.title} onChange={(e) => setField("title", e.target.value)} />
                        </div>

                        <div>
                            <div className="label">Description</div>
                            <input className="input" placeholder="Short description" value={form.description} onChange={(e) => setField("description", e.target.value)} />
                        </div>

                        <div>
                            <div className="label">Genres</div>
                            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                                {GENRES.map((g) => {
                                    const checked = (form.genres || []).includes(g);
                                    return (
                                        <label
                                            key={g}
                                            style={{
                                                display: "flex",
                                                gap: 8,
                                                alignItems: "center",
                                                border: "1px solid var(--line)",
                                                borderRadius: 999,
                                                padding: "6px 10px",
                                                cursor: "pointer",
                                                background: checked ? "rgba(59,130,246,0.10)" : "white",
                                            }}
                                        >
                                            <input
                                                type="checkbox"
                                                checked={checked}
                                                onChange={() => toggleGenre(g)}
                                            />
                                            <span style={{ fontSize: 13, fontWeight: 700 }}>{g}</span>
                                        </label>
                                    );
                                })}
                            </div>
                        </div>

                        <div className="row" style={{ gap: 12 }}>
                            <div style={{ flex: 1 }}>
                                <div className="label">Duration (minutes)</div>
                                <input
                                    className="input"
                                    type="number"
                                    min={1}
                                    placeholder="e.g. 152"
                                    value={form.duration}
                                    onChange={(e) => setField("duration", e.target.value)}
                                />
                            </div>

                            <div style={{ flex: 1 }}>
                                <div className="label">Language</div>
                                <select
                                    className="input"
                                    value={form.language}
                                    onChange={(e) => setField("language", e.target.value)}
                                >
                                    <option value="">Select language</option>
                                    {LANGUAGES.map((l) => (
                                        <option key={l} value={l}>{l}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="row" style={{ gap: 12 }}>
                            <div style={{ flex: 1 }}>
                                <div style={{ flex: 1 }}>
                                    <div className="label">Rating</div>
                                    <select
                                        className="input"
                                        value={form.rating}
                                        onChange={(e) => setField("rating", e.target.value)}
                                    >
                                        <option value="">Select rating</option>
                                        {RATINGS.map((r) => (
                                            <option key={r} value={r}>{r}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div style={{ flex: 1 }}>
                                <div className="label">Release Date</div>
                                <input
                                    className="input"
                                    type="date"
                                    value={form.releaseDate}
                                    onChange={(e) => setField("releaseDate", e.target.value)}
                                />
                            </div>
                        </div>

                        <div style={{ flex: 1 }}>
                            <div className="label">Poster URL</div>
                            <input
                                className="input"
                                placeholder="https://..."
                                value={form.posterUrl || ""}
                                onChange={(e) => setField("posterUrl", e.target.value)}
                            />

                            {form.posterUrl ? (
                                <div style={{ marginTop: 10 }}>
                                    <img
                                        src={form.posterUrl}
                                        alt="Poster preview"
                                        style={{ width: 160, height: 220, objectFit: "cover", borderRadius: 12, border: "1px solid var(--line)" }}
                                        onError={(e) => (e.currentTarget.style.display = "none")}
                                    />
                                </div>
                            ) : null}
                        </div>

                        <div className="row" style={{ justifyContent: "flex-end", gap: 10 }}>
                            {editId && (
                                <button type="button" className="btn" onClick={startCreate}>
                                    Cancel Edit
                                </button>
                            )}
                            <button className="btn btnPrimary" type="submit">
                                {editId ? "Update" : "Create"}
                            </button>
                        </div>
                    </form>
                </div>

                {/* List */}
                <div className="card">
                    <div style={{ fontWeight: 900, marginBottom: 10 }}>All Movies</div>

                    <div style={{ display: "grid", gap: 10 }}>
                        {movies.map((m) => (
                            <div
                                key={m.id}
                                style={{
                                    border: "1px solid var(--line)",
                                    borderRadius: 14,
                                    padding: 12,
                                    display: "flex",
                                    justifyContent: "space-between",
                                    gap: 10,
                                    alignItems: "center",
                                }}
                            >
                                <div style={{ display: "grid", gap: 4 }}>
                                    <div style={{ fontWeight: 900 }}>{m.title}</div>
                                    <div style={{ color: "var(--muted)", fontSize: 13 }}>
                                        #{m.id} • {m.language || "—"} • {m.duration ? `${m.duration} mins` : "—"}
                                    </div>
                                </div>

                                <div className="row" style={{ justifyContent: "flex-end", gap: 8 }}>
                                    <button className="btn" onClick={() => startEdit(m)}>Edit</button>
                                    <button className="btn btnDanger" onClick={() => remove(m.id)}>Delete</button>
                                </div>
                            </div>
                        ))}

                        {movies.length === 0 && <div style={{ color: "var(--muted)" }}>No movies.</div>}
                    </div>
                </div>
            </div>
        </div>
    );
}