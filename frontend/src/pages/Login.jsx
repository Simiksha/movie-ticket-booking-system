import { useState } from "react";
import { loginApi } from "../api/auth";
import { useAuth } from "../auth/AuthContext";
import { Link, useNavigate } from "react-router-dom";
import { FaEye, FaEyeSlash } from "react-icons/fa";

export default function Login() {
  const [form, setForm] = useState({ email: "", password: "" });

  // client validation
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({ email: false, password: false });
  const [submitted, setSubmitted] = useState(false);

  // server validation 
  const [serverErrors, setServerErrors] = useState({});
  const [err, setErr] = useState(""); 

  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const { login, logout } = useAuth();
  const nav = useNavigate();

  function validate(values) {
    const newErrors = {};

    if (!values.email.trim()) newErrors.email = "Email is required";
    else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(values.email))
      newErrors.email = "Invalid email address";

    if (!values.password) newErrors.password = "Password is required";
    else if (values.password.length < 6)
      newErrors.password = "Password must be at least 6 characters";

    return newErrors;
  }

  function handleChange(e) {
    const { name, value } = e.target;
    const updated = { ...form, [name]: value };
    setForm(updated);

    // re-validate client errors
    setErrors(validate(updated));

    // clear server error for that field when user edits it
    setServerErrors((prev) => ({ ...prev, [name]: "" }));
    setErr("");
  }

  function handleBlur(e) {
    const { name } = e.target;
    setTouched((t) => ({ ...t, [name]: true }));
  }

  function shouldShowClientError(field) {
    return (touched[field] || submitted) && errors[field];
  }

  function fieldError(field) {
    return shouldShowClientError(field) ? errors[field] : serverErrors[field];
  }

  async function onSubmit(e) {
    e.preventDefault();
    setSubmitted(true);
    setErr("");
    setServerErrors({});

    setTouched({ email: true, password: true });

    const validationErrors = validate(form);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    try {
      setLoading(true);
      const token = await loginApi(form.email, form.password);

      login(token);
      nav("/movies");
    } catch (ex) {
      logout();
      const status = ex?.response?.status;
      const data = ex?.response?.data;

      // Case 1: field errors from backend
      const backendFieldErrors = data?.errors || data?.fieldErrors;
      if (backendFieldErrors && typeof backendFieldErrors === "object") {
        setServerErrors({
          email: backendFieldErrors.email || "",
          password: backendFieldErrors.password || "",
        });
        return;
      }

      // Case 2: invalid credentials -> password field
      const msg = data?.message || ex?.message || "Login failed";
      const looksLikeInvalidCreds =
        status === 401 ||
        status === 403 ||
        /invalid credentials|bad credentials|unauthorized|wrong password/i.test(msg);

      if (looksLikeInvalidCreds) {
        setServerErrors({ password: msg || "Invalid email or password" });
        return;
      }

      // Case 3: general banner
      setErr(msg);
    } finally {
      setLoading(false);
    }
  }

  const isValid =
    Object.keys(validate(form)).length === 0 &&
    form.email.trim() &&
    form.password;

  return (
    <div
      className="container"
      style={{ display: "grid", placeItems: "center", minHeight: "calc(100vh - 90px)" }}
    >
      <div className="card" style={{ width: 420, maxWidth: "100%" }}>
        <div className="h1" style={{ marginBottom: 6 }}>Welcome back</div>
        <div className="sub" style={{ marginBottom: 18 }}>
          Login to continue booking your movie tickets.
        </div>

        {err && <div className="err" style={{ marginBottom: 12 }}>{err}</div>}

        <form onSubmit={onSubmit} className="grid" style={{ gap: 12 }}>
          {/* Email */}
          <div>
            <div style={{ fontWeight: 700, marginBottom: 6 }}>Email</div>
            <input
              className="input"
              name="email"
              placeholder="user@gmail.com"
              value={form.email}
              onChange={handleChange}
              onBlur={handleBlur}
              autoComplete="email"
            />
            {fieldError("email") && (
              <div style={{ color: "red", fontSize: 12 }}>{fieldError("email")}</div>
            )}
          </div>

          {/* Password */}
          <div style={{ position: "relative", marginTop: 10 }}>
            <input
              name="password"
              placeholder="Password"
              type={showPassword ? "text" : "password"}
              value={form.password}
              onChange={handleChange}
              onBlur={handleBlur}
              style={{ width: "100%", padding: 8, paddingRight: 40 }}
            />

            <span
              onClick={() => setShowPassword((s) => !s)}
              style={{
                position: "absolute",
                right: 10,
                top: "50%",
                transform: "translateY(-50%)",
                cursor: "pointer",
                userSelect: "none",
              }}
            >
              {showPassword ? <FaEyeSlash /> : <FaEye />}
            </span>

            {fieldError("password") && (
              <div style={{ color: "red", fontSize: 12, marginTop: 4 }}>
                {fieldError("password")}
              </div>
            )}
          </div>

          <button className="btn btnPrimary" disabled={loading || !isValid}>
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <div style={{ marginTop: 14, color: "var(--muted)", fontSize: 13 }}>
          New here?{" "}
          <Link to="/register" style={{ color: "var(--brand)", fontWeight: 800 }}>
            Create an account
          </Link>
        </div>
      </div>
    </div>
  );
}