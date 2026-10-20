import { createContext, useContext, useState, useEffect } from "react";
import { authApi } from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => authApi.getCurrentUser());
  const [token, setToken] = useState(() => authApi.getToken());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if token exists, optionally verify with /auth/me
    const verifyAuth = async () => {
      const storedToken = authApi.getToken();
      if (storedToken) {
        try {
          const res = await authApi.getMe();
          if (res?.data) {
            setUser((prev) => ({ ...prev, ...res.data }));
          }
        } catch (err) {
          console.warn("Session check failed or expired:", err.message);
          // Don't violently log out on offline error; only on 401
          if (err.status === 401) {
            setUser(null);
            setToken(null);
            authApi.logout();
          }
        }
      }
      setLoading(false);
    };

    verifyAuth();
  }, []);

  const login = async (username, password) => {
    const res = await authApi.login({ username, password });
    const userData = res.data;
    setUser(userData);
    setToken(userData?.accessToken);
    return userData;
  };

  const register = async (username, email, password, role = "ROLE_USER") => {
    const res = await authApi.register({ username, email, password, role });
    const userData = res.data;
    setUser(userData);
    setToken(userData?.accessToken);
    return userData;
  };

  const logout = async () => {
    await authApi.logout();
    setUser(null);
    setToken(null);
  };

  const isAuthenticated = Boolean(token);
  const isAdmin = user?.role === "ROLE_ADMIN" || user?.roles?.includes("ROLE_ADMIN");

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isAdmin,
        loading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
