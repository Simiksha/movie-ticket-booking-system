import { useEffect, useState } from "react";
import api from "../../api/axios";

export default function AdminTheaters() {
  const [err, setErr] = useState("");
  const [ok, setOk] = useState("");

  // theaters list for dropdown
  const [theaters, setTheaters] = useState([]);
  const [selectedTheaterId, setSelectedTheaterId] = useState("");

  // Create Theater
  const [theaterName, setTheaterName] = useState("");
  const [theaterCity, setTheaterCity] = useState("");
  const [theaterAddress, setTheaterAddress] = useState("");

  // Add Screen
  const [screenName, setScreenName] = useState("");
  const [totalSeats, setTotalSeats] = useState("");

  async function loadTheaters(pickId) {
    const res = await api.get("/admin/theaters");
    const data = Array.isArray(res.data) ? res.data : [];
    setTheaters(data);

    // auto select
    if (pickId) setSelectedTheaterId(String(pickId));
    else if (!selectedTheaterId && data.length) setSelectedTheaterId(String(data[0].id));
  }

  useEffect(() => {
    (async () => {
      try {
        await loadTheaters();
      } catch (e) {
        setErr(e?.response?.data?.message || "Failed to load theaters");
      }
    })();
  }, []);

  async function createTheater(e) {
    e.preventDefault();
    setErr("");
    setOk("");

    try {
      if (!theaterName.trim()) throw new Error("Theater name is required");

      const payload = {
        name: theaterName,
        city: theaterCity,
        address: theaterAddress,
      };

      const res = await api.post("/admin/theaters", payload);

      setOk(`Theater created ✅ ID: ${res.data}`);
      setTheaterName("");
      setTheaterCity("");
      setTheaterAddress("");

      await loadTheaters(res.data); 
    } catch (e2) {
      setErr(e2?.response?.data?.message || e2.message || "Create theater failed");
    }
  }

  async function addScreen(e) {
    e.preventDefault();
    setErr("");
    setOk("");

    try {
      if (!selectedTheaterId) throw new Error("Select a theater");
      if (!screenName.trim()) throw new Error("Screen name is required");
      if (!totalSeats || Number(totalSeats) < 1) throw new Error("Total seats must be at least 1");

      const payload = {
        name: screenName,
        totalSeats: Number(totalSeats),
      };

      const res = await api.post(`/admin/theaters/${selectedTheaterId}/screens`, payload);

      setOk(`Screen created ✅ ID: ${res.data}`);
      setScreenName("");
      setTotalSeats("");
    } catch (e2) {
      setErr(e2?.response?.data?.message || e2.message || "Add screen failed");
    }
  }

  return (
    <div className="container">
      <div className="h1">Admin • Theaters</div>
      <div className="sub">Add New Theaters and Screens.</div>

      {err && <div className="err">{err}</div>}
      {ok && <div className="card" style={{ borderColor: "var(--ok)" }}>{ok}</div>}

      <div className="grid" style={{ marginTop: 14 }}>
        {/* Create Theater */}
        <div className="card">
          <div style={{ fontWeight: 900, marginBottom: 10 }}>Create Theater</div>

          <form onSubmit={createTheater} className="grid" style={{ gap: 12 }}>
            <div>
              <div className="label">Theater Name</div>
              <input className="input" placeholder="e.g. PVR Phoenix Mall" value={theaterName} onChange={(e) => setTheaterName(e.target.value)} />
            </div>

            <div>
              <div className="label">City</div>
              <input className="input" placeholder="e.g. Chennai" value={theaterCity} onChange={(e) => setTheaterCity(e.target.value)} />
            </div>

            <div>
              <div className="label">Address</div>
              <input className="input" placeholder="Full address" value={theaterAddress} onChange={(e) => setTheaterAddress(e.target.value)} />
            </div>

            <button className="btn btnPrimary">Create Theater</button>
          </form>
        </div>

        {/* Add Screen */}
        <div className="card">
          <div style={{ fontWeight: 900, marginBottom: 10 }}>Add Screen</div>

          <form onSubmit={addScreen} className="grid" style={{ gap: 12 }}>
            <div>
              <div className="label">Select Theater</div>
              <select className="input" value={selectedTheaterId} onChange={(e) => setSelectedTheaterId(e.target.value)}>
                <option value="">Select...</option>
                {theaters.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} {t.city ? `• ${t.city}` : ""} (#{t.id})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="label">Screen Name</div>
              <input className="input" placeholder="e.g. Screen 1" value={screenName} onChange={(e) => setScreenName(e.target.value)} />
            </div>

            <div>
              <div className="label">Number of Seats</div>
              <input className="input" type="number" min={1} placeholder="e.g. 120" value={totalSeats} onChange={(e) => setTotalSeats(e.target.value)} />
            </div>

            <button className="btn btnPrimary" disabled={!selectedTheaterId || !screenName || !totalSeats}>
              Add Screen
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}