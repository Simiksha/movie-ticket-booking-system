import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../api/axios";
import { useAuth } from "../auth/AuthContext"; 

// booking uses physical seatId
function getSeatId(s) {
  return s.seatId;
}
function getSeatNumber(s) {
  return s.seatNumber || "";
}
function isBooked(s) {
  return !!s.booked;
}
function getRowLabel(seatNumber) {
  const m = String(seatNumber || "").match(/^[A-Za-z]+/);
  return m ? m[0].toUpperCase() : "?";
}
function getSeatIndex(seatNumber) {
  const m = String(seatNumber || "").match(/(\d+)/);
  return m ? Number(m[1]) : 0;
}

export default function ShowSeatsPage() {
  const { showId } = useParams();
  const nav = useNavigate();

  const { role } = useAuth(); 
  const isAdmin = role === "ADMIN"; 

  const [seats, setSeats] = useState([]);
  const [selected, setSelected] = useState(() => new Set());
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);

  const selectedIds = useMemo(() => Array.from(selected), [selected]);

  const seatPriceMap = useMemo(() => {
    return new Map(seats.map((s) => [getSeatId(s), Number(s.price || 0)]));
  }, [seats]);

  const totalPrice = useMemo(() => {
    return selectedIds.reduce((sum, id) => sum + (seatPriceMap.get(id) || 0), 0);
  }, [selectedIds, seatPriceMap]);

  const selectedSeatNumbers = useMemo(() => {
    const map = new Map(seats.map((s) => [getSeatId(s), getSeatNumber(s)]));
    return selectedIds.map((id) => map.get(id)).filter(Boolean);
  }, [seats, selectedIds]);

  const rows = useMemo(() => {
    const grouped = new Map();
    for (const s of seats) {
      const row = getRowLabel(getSeatNumber(s));
      if (!grouped.has(row)) grouped.set(row, []);
      grouped.get(row).push(s);
    }
    const sortedRows = Array.from(grouped.entries()).sort((a, b) =>
      String(a[0]).localeCompare(String(b[0]))
    );
    for (const [row, list] of sortedRows) {
      list.sort((x, y) => getSeatIndex(getSeatNumber(x)) - getSeatIndex(getSeatNumber(y)));
      grouped.set(row, list);
    }
    return sortedRows;
  }, [seats]);

  useEffect(() => {
    (async () => {
      try {
        setErr("");
        setLoading(true);

        const res = await api.get(`/shows/${showId}/seats`);
        const data = Array.isArray(res.data)
          ? res.data
          : Array.isArray(res.data?.data)
            ? res.data.data
            : [];

        setSeats(data);
        setSelected(new Set());
      } catch (e) {
        setErr(e?.response?.data?.message || "Failed to load seats");
      } finally {
        setLoading(false);
      }
    })();
  }, [showId]);

  function toggleSeat(seat) {
    if (isAdmin) return; 
    if (isBooked(seat)) return;

    const id = getSeatId(seat);
    if (id == null) return;

    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function createBooking() {
    if (isAdmin) {
      setErr("Admin accounts can’t book tickets. Please login as a USER to book.");
      return;
    }

    if (selectedIds.length === 0) {
      setErr("Please select at least one seat.");
      return;
    }

    try {
      setErr("");
      setBookingLoading(true);

      const body = { showId: Number(showId), seatIds: selectedIds };
      const res = await api.post("/bookings", body);

      const bookingId =
        typeof res.data === "number" ? res.data : (res.data?.bookingId || res.data?.id);

      if (!bookingId) throw new Error("Booking created but bookingId not found");

      nav(`/bookings/${bookingId}/pay`);
    } catch (e) {
      setErr(
        e?.response?.data?.message ||
        (typeof e?.response?.data === "string" ? e.response.data : "") ||
        "Booking failed"
      );
    } finally {
      setBookingLoading(false);
    }
  }

  if (loading) return <div className="container">Loading seats...</div>;

  return (
    <div className="container" style={{ paddingBottom: isAdmin ? 20 : 90 }}>
      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-end" }}>
        <div>
          <div className="h1">Select Seats</div>
          <div className="sub">Pick your seats for Show #{showId}.</div>
        </div>

        <Link className="btn" to="/movies">
          ← Back
        </Link>
      </div>

      {/* Admin info banner */}
      {isAdmin && (
        <div
          className="card"
          style={{
            marginTop: 12,
            border: "1px solid var(--line)",
            background: "rgba(59,130,246,.06)",
          }}
        >
          <div style={{ fontWeight: 900, marginBottom: 4 }}>Admin view</div>
          <div style={{ color: "var(--muted)", fontSize: 13 }}>
            Admin can only view seat availability.
          </div>
        </div>
      )}

      {err && <div className="err" style={{ marginTop: 12 }}>{err}</div>}

      <div className="card" style={{ marginTop: 14 }}>
        <div className="row" style={{ justifyContent: "space-between" }}>
          <div className="row" style={{ gap: 10 }}>
            <span className="pill">Available</span>
            <span
              className="pill"
              style={{
                background: "rgba(185,28,28,.12)",
                color: "var(--brand)",
                border: "1px solid rgba(185,28,28,.25)",
              }}
            >
              Selected
            </span>
            <span
              className="pill"
              style={{
                background: "#f3f4f6",
                color: "#6b7280",
                border: "1px solid var(--line)",
              }}
            >
              Booked
            </span>
          </div>

          {!isAdmin && (
            <div style={{ color: "var(--muted)", fontSize: 13 }}>
              Tip: click again to unselect.
            </div>
          )}
        </div>

        {/* Screen */}
        <div
          style={{
            marginTop: 14,
            padding: "10px 12px",
            borderRadius: 12,
            background: "#f3f4f6",
            textAlign: "center",
            fontWeight: 800,
            letterSpacing: ".4px",
            color: "#374151",
          }}
        >
          SCREEN THIS WAY
        </div>

        {/* Seats */}
        {seats.length === 0 && !err ? (
          <div style={{ marginTop: 14, color: "var(--muted)" }}>No seats found for this show.</div>
        ) : (
          <div style={{ marginTop: 16, display: "grid", gap: 12 }}>
            {rows.map(([rowLabel, list]) => (
              <div key={rowLabel} style={{ display: "grid", gap: 10 }}>
                <div style={{ fontWeight: 800, color: "#374151" }}>Row {rowLabel}</div>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fill, minmax(54px, 1fr))",
                    gap: 10,
                  }}
                >
                  {list.map((s) => {
                    const seatId = getSeatId(s);
                    const booked = isBooked(s);
                    const isSel = selected.has(seatId);

                    const disabled = booked || isAdmin;

                    return (
                      <button
                        key={s.showSeatId}
                        onClick={() => toggleSeat(s)}
                        disabled={disabled}
                        className="btn"
                        style={{
                          padding: "10px 6px",
                          borderRadius: 12,
                          border: booked
                            ? "1px solid var(--line)"
                            : isSel
                              ? "1px solid rgba(185,28,28,.4)"
                              : "1px solid var(--line)",
                          background: booked
                            ? "#f3f4f6"
                            : isSel
                              ? "rgba(185,28,28,.10)"
                              : "white",
                          color: booked
                            ? "#9ca3af"
                            : isSel
                              ? "var(--brand)"
                              : "var(--text)",
                          fontWeight: 800,
                          cursor: disabled ? "not-allowed" : "pointer",
                          opacity: isAdmin && !booked ? 0.75 : 1,
                        }}
                        title={
                          booked
                            ? "Booked"
                            : isAdmin
                              ? "Admin view only (booking disabled)"
                              : isSel
                                ? "Selected"
                                : "Available"
                        }
                      >
                        {getSeatNumber(s)}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Sticky booking bar hidden for admin */}
      {!isAdmin && (
        <div
          style={{
            position: "fixed",
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(248,250,252,.9)",
            borderTop: "1px solid var(--line)",
            padding: "12px 0",
            backdropFilter: "blur(8px)",
          }}
        >
          <div className="container" style={{ padding: "0 22px" }}>
            <div className="card" style={{ padding: 12 }}>
              <div className="row" style={{ justifyContent: "space-between" }}>
                <div style={{ display: "grid", gap: 4 }}>
                  <div style={{ fontWeight: 850 }}>
                    Selected: {selectedIds.length} seat{selectedIds.length === 1 ? "" : "s"}
                  </div>
                  <div style={{ color: "var(--muted)", fontSize: 13 }}>
                    {selectedSeatNumbers.length ? selectedSeatNumbers.join(", ") : "No seats selected"}
                  </div>
                </div>

                <div className="row">
                  <div style={{ fontWeight: 900, fontSize: 16 }}>₹{totalPrice}</div>
                  <button
                    className="btn btnPrimary"
                    onClick={createBooking}
                    disabled={bookingLoading || selectedIds.length === 0}
                  >
                    {bookingLoading ? "Booking..." : "Book & Pay"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}