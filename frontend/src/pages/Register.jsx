import { useState } from "react";
import { registerApi } from "../api/auth";
import { Link, useNavigate } from "react-router-dom";
import { FaEye, FaEyeSlash } from "react-icons/fa";

export default function Register() {
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({ name: false, email: false, password: false });
  const [submitted, setSubmitted] = useState(false);

  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const nav = useNavigate();

  function validate(values) {
    const newErrors = {};

    // Name
    if (!values.name.trim()) newErrors.name = "Full name is required";
    else if (values.name.trim().length < 3) newErrors.name = "Name must be at least 3 characters";

    // Email
    if (!values.email.trim()) newErrors.email = "Email is required";
    else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(values.email))
      newErrors.email = "Invalid email address";

    // Password
    if (!values.password) newErrors.password = "Password is required";
    else if (values.password.length < 6) newErrors.password = "Password must be at least 6 characters";

    return newErrors;
  }

  function handleChange(e) {
    const { name, value } = e.target;
    const updated = { ...form, [name]: value };
    setForm(updated);

    setErrors(validate(updated));
  }

  function handleBlur(e) {
    const { name } = e.target;
    setTouched((t) => ({ ...t, [name]: true }));
  }

  function shouldShowError(field) {
    return (touched[field] || submitted) && errors[field];
  }

  async function onSubmit(e) {
    e.preventDefault();
    setErr("");
    setSubmitted(true);

    setTouched({ name: true, email: true, password: true });

    const validationErrors = validate(form);
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) return;

    try {
      setLoading(true);
      await registerApi(form.name, form.email, form.password);
      nav("/login");
    } catch (ex) {
      setErr(ex?.response?.data?.message || ex?.message || "Register failed");
    } finally {
      setLoading(false);
    }
  }

  const isValid =
    Object.keys(validate(form)).length === 0 &&
    form.name.trim() &&
    form.email.trim() &&
    form.password;

  return (
    <div
      className="container"
      style={{ display: "grid", placeItems: "center", minHeight: "calc(100vh - 90px)" }}
    >
      <div className="card" style={{ width: 420, maxWidth: "100%" }}>
        <div className="h1" style={{ marginBottom: 6 }}>Create Account</div>
        <div className="sub" style={{ marginBottom: 18 }}>
          Sign up to start booking your movie tickets.
        </div>

        {err && <div className="err" style={{ marginBottom: 12 }}>{err}</div>}

        <form onSubmit={onSubmit} className="grid" style={{ gap: 12 }}>
          {/* Name */}
          <div>
            <div style={{ fontWeight: 700, marginBottom: 6 }}>Full Name</div>
            <input
              className="input"
              name="name"
              placeholder="Your name"
              value={form.name}
              onChange={handleChange}
              onBlur={handleBlur}
              autoComplete="name"
            />
            {shouldShowError("name") && (
              <div style={{ color: "red", fontSize: 12 }}>{errors.name}</div>
            )}
          </div>

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
            {shouldShowError("email") && (
              <div style={{ color: "red", fontSize: 12 }}>{errors.email}</div>
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
                right: 12,
                top: "50%",
                transform: "translateY(-50%)",
                cursor: "pointer",
                color: "#666",
              }}
            >
              {showPassword ? <FaEyeSlash /> : <FaEye />}
            </span>

            {shouldShowError("password") && (
              <div style={{ color: "red", fontSize: 12, marginTop: 4 }}>
                {errors.password}
              </div>
            )}
          </div>

          <button className="btn btnPrimary" disabled={loading || !isValid}>
            {loading ? "Creating account..." : "Create Account"}
          </button>
        </form>

        <div style={{ marginTop: 14, color: "var(--muted)", fontSize: 13 }}>
          Already have an account?{" "}
          <Link to="/login" style={{ color: "var(--brand)", fontWeight: 800 }}>
            Login here
          </Link>
        </div>
      </div>
    </div>
  );
}