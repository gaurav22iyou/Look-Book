import { createContext, useState, useContext } from 'react';
import { apiLogin, setAuthCredentials, clearAuthCredentials } from '../services/api';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const storedUser = sessionStorage.getItem('username');
    const storedAuth = sessionStorage.getItem('authHeader');
    if (storedUser && storedAuth) {
      setAuthCredentials(storedAuth);
      return { username: storedUser };
    }
    return null;
  });

  const login = async (username, password) => {
    // Using btoa to Base64 encode credentials for HTTP Basic Auth
    const authHeader = 'Basic ' + btoa(`${username}:${password}`);
    
    // Verify by calling apiLogin which fetches the protected GET /slots endpoint
    await apiLogin(authHeader);
    
    // If successful, retain credentials in session storage for stateless auth
    setAuthCredentials(authHeader);
    sessionStorage.setItem('authHeader', authHeader);
    sessionStorage.setItem('username', username);
    setUser({ username });
  };

  const logout = () => {
    clearAuthCredentials();
    sessionStorage.removeItem('authHeader');
    sessionStorage.removeItem('username');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
