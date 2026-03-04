import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";

function upper(v) {
  return String(v || "").toUpperCase();
}

function bookingPillClass(status) {
  const s = upper(status);
  if (s === "CONFIRMED") return "pill success";
  if (s === "PENDING") return "pill pending";
  if (s === "EXPIRED") return "pill failed";
  if (s === "CANCELLED") return "pill cancelled";
  return "pill";
}

function paymentPillClass(status) {
  const s = upper(status);
  if (s === "SUCCESS") return "pill success";
  if (s === "INITIATED") return "pill pending";
  if (s === "FAILED") return "pill failed";
  if (s === "REFUNDED") return "pill cancelled";
  return "pill";
}

function fmt(iso) {
  if (!iso) return "—";
  return String(iso).replace("T", " ");
}

function minutesUntil(iso) {
  if (!iso) return Infinity;
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return Infinity;
  return Math.floor((t - Date.now()) / 60000);
}

export default function MyBookings() {
  const nav = useNavigate();

  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  // bookingId -> boolean (true while cancelling)
  const [cancelling, setCancelling] = useState({});

  async function load() {
    const res = await api.get("/bookings/my-bookings");
    return Array.isArray(res.data) ? res.data : [];
  }

  useEffect(() => {
    (async () => {
      try {
        setErr("");
        setLoading(true);
        setBookings(await load());
      } catch (e) {
        const status = e?.response?.status;
        if (status === 401 || status === 403) {
          nav("/login");
          return;
        }
        setErr(e?.response?.data?.message || "Failed to load bookings");
      } finally {
        setLoading(false);
      }
    })();
  }, [nav]);

  const hasAny = bookings.length > 0;

  const summary = useMemo(() => {
    const total = bookings.reduce((sum, b) => sum + Number(b.totalAmount || 0), 0);
    const confirmed = bookings.filter((b) => upper(b.bookingStatus) === "CONFIRMED").length;
    return { total, confirmed };
  }, [bookings]);

  async function cancelBooking(bookingId, showTime) {
    // prevent double click
    if (cancelling[bookingId]) return;

    // frontend guard: don't even try if within 30 mins
    const minsLeft = minutesUntil(showTime);
    if (minsLeft <= 30) return;

    try {
      setErr("");
      // disable/cursor lock immediately
      setCancelling((p) => ({ ...p, [bookingId]: true }));

      await api.delete(`/bookings/${bookingId}`);
      setBookings(await load());
    } catch (e) {
      setErr(e?.response?.data?.message || "Failed to cancel booking");
    } finally {
      // re-enable button
      setCancelling((p) => ({ ...p, [bookingId]: false }));
    }
  }

  if (loading) return <div className="container">Loading bookings...</div>;

  return (
    <div className="container">
      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-end" }}>
        <div>
          <div className="h1">My Bookings</div>
          <div className="sub">Track your tickets, payments and cancellations.</div>
        </div>

        {hasAny ? (
          <div className="row" style={{ gap: 10 }}>
            <span className="pill">Confirmed: {summary.confirmed}</span>
          </div>
        ) : null}
      </div>

      {err && <div className="err">{err}</div>}

      {!err && bookings.length === 0 && (
        <div className="card">
          <b>No bookings found.</b>
          <div style={{ marginTop: 20 }}>
            <Link className="btn btnPrimary" to="/movies">
              Browse Movies
            </Link>
          </div>
        </div>
      )}

      <div className="grid" style={{ marginTop: 14 }}>
        {bookings.map((b) => {
          const bookingId = b.bookingId;
          const bookingStatus = b.bookingStatus;
          const paymentStatus = b.paymentStatus;

          const canPay = upper(bookingStatus) === "PENDING";

          const minsLeft = minutesUntil(b.showTime);
          const cancelLocked = minsLeft <= 30;
          const showCancelUI = upper(bookingStatus) === "CONFIRMED";

          const isCancelling = !!cancelling[bookingId];

          return (
            <div key={bookingId} className="card">
              <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-start" }}>
                <div style={{ display: "grid", gap: 6 }}>
                  <div style={{ fontWeight: 900, fontSize: 16 }}>{b.movieTitle}</div>
                  <div style={{ color: "var(--muted)" }}>{b.theaterName}</div>
                  <div style={{ color: "var(--muted)", fontSize: 13 }}>
                    Show: <b style={{ color: "var(--text)" }}>{fmt(b.showTime)}</b>
                  </div>
                  <div style={{ color: "var(--muted)", fontSize: 13 }}>
                    Seats:{" "}
                    <b style={{ color: "var(--text)" }}>
                      {Array.isArray(b.seats) ? b.seats.join(", ") : "—"}
                    </b>
                  </div>

                  {b.totalAmount != null && (
                    <div style={{ color: "var(--muted)", fontSize: 13 }}>
                      Total: <b style={{ color: "var(--text)" }}>₹{b.totalAmount}</b>
                    </div>
                  )}

                  <div style={{ color: "var(--muted)", fontSize: 12 }}>Booked at: {fmt(b.bookedAt)}</div>
                </div>

                <div style={{ display: "grid", gap: 8, justifyItems: "end" }}>
                  <span className={bookingPillClass(bookingStatus)}>{bookingStatus}</span>
                  <span className={paymentPillClass(paymentStatus)}>Payment: {paymentStatus}</span>

                  <div className="row" style={{ justifyContent: "flex-end" }}>
                    {canPay && (
                      <Link className="btn btnPrimary" to={`/bookings/${bookingId}/pay`}>
                        Pay Now
                      </Link>
                    )}

                    {/* Cancel button: shown for CONFIRMED, disabled if cancelling or within 30 mins */}
                    {showCancelUI && (
                      <button
                        className="btn btnDanger"
                        onClick={() => cancelBooking(bookingId, b.showTime)}
                        disabled={isCancelling || cancelLocked}
                        style={{
                          cursor: isCancelling || cancelLocked ? "not-allowed" : "pointer",
                          opacity: isCancelling || cancelLocked ? 0.6 : 1,
                        }}
                        title={cancelLocked ? "Cancellation not allowed within 30 minutes of showtime" : ""}
                      >
                        {isCancelling ? "Cancelling..." : "Cancel"}
                      </button>
                    )}
                  </div>

                  {showCancelUI && cancelLocked && (
                    <div style={{ fontSize: 12, color: "var(--muted)", marginTop: 6, textAlign: "right" }}>
                      Cancellation not allowed within 30 minutes of showtime
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}