import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../api/axios";

export default function Payment() {
  const { bookingId } = useParams();
  const nav = useNavigate();

  const [err, setErr] = useState("");
  const [phase, setPhase] = useState("creating");

  const startedRef = useRef(false);
  const openedRef = useRef(false);
  const successRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    console.log("Payment page mounted for booking:", bookingId);

    if (startedRef.current) return;
    startedRef.current = true;

    (async () => {
      try {
        setErr("");
        setPhase("creating");

        console.log("Calling create-order for booking:", bookingId);
        const res = await api.post(`/payments/create-order/${bookingId}`);
        console.log("Create-order response:", res.data);

        const { orderId, currency, key } = res.data;

        if (!window.Razorpay) throw new Error("Razorpay script not loaded");

        const options = {
          key,
          currency,
          name: "Movie Ticket Booking",
          description: `Booking #${bookingId}`,
          order_id: orderId,

          handler: async function (response) {
            console.log("Razorpay SUCCESS handler fired:", JSON.stringify(response));

            successRef.current = true;

            try {
              setErr("");
              setPhase("verifying");

              console.log("Calling VERIFY...");
              const vr = await api.post(`/payments/verify/${bookingId}`, response);
              console.log("VERIFY response:", vr.status, vr.data);

              nav("/my-bookings");
            } catch (e) {
              console.log("VERIFY failed:", e?.response?.status, e?.response?.data, e);
              successRef.current = false;
              setErr(e?.response?.data?.message || "Payment verification failed");
              setPhase("paying");
            }
          },

          modal: {
            ondismiss: function () {
              console.log("Razorpay MODAL dismissed. successRef:", successRef.current);
              if (cancelled) return;
              if (!successRef.current) nav("/my-bookings");
            },
          },
        };

        if (!openedRef.current) {
          openedRef.current = true;
          setPhase("paying");

          const rzp = new window.Razorpay(options);

          rzp.on("payment.failed", function (resp) {
            console.log("Razorpay payment.failed:", resp);
            successRef.current = false;
            setErr(resp?.error?.description || "Payment failed");
          });

          rzp.open();
        }
      } catch (e) {
        if (cancelled) return;
        setErr(e?.response?.data?.message || e.message || "Payment failed");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [bookingId, nav]);

  return (
    <div className="container" style={{ maxWidth: 720 }}>
      <div className="card" style={{ display: "grid", gap: 10 }}>
        <div className="h1">Processing Payment</div>

        {phase === "creating" && <div className="sub">Creating order…</div>}
        {phase === "paying" && <div className="sub">Opening Razorpay…</div>}
        {phase === "verifying" && <div className="sub">Verifying payment… please wait.</div>}

        {err && <div className="err">{err}</div>}

        {err && (
          <div className="row" style={{ gap: 10 }}>
            <button className="btn btnPrimary" onClick={() => nav("/my-bookings")}>
              Go to My Bookings
            </button>
          </div>
        )}
      </div>
    </div>
  );
}