import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function Header({ onLoginClick, currentView, onNavigate }) {
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  // We use user.username === 'admin' ONLY for UI navigation visibility, NOT as an authorization check. 
  // The backend remains the source of truth for all security rules.
  const isAdmin = user?.username === 'admin';

  const handleNavigate = (view) => {
    onNavigate?.(view);
    setMenuOpen(false);
  };

  const handleLogout = () => {
    logout();
    onNavigate?.('booking');
    setMenuOpen(false);
  };

  const handleLogin = () => {
    onLoginClick?.();
    setMenuOpen(false);
  };

  return (
    <header className="header">
      <div className="header-content">
        <div className="header-title" onClick={() => handleNavigate('explore')} style={{ cursor: 'pointer' }}>
          Book<span className="text-gold">Now</span>
        </div>
        
        {/* Desktop Navigation */}
        {user && (
          <nav className="header-nav header-nav-desktop apple-segmented-nav">
            <button 
              className={`apple-nav-item ${currentView === 'booking' ? 'active' : ''}`}
              onClick={() => handleNavigate('booking')}
            >
              Bookings
            </button>
            {isAdmin && (
              <button 
                className={`apple-nav-item ${currentView === 'admin' ? 'active' : ''}`}
                onClick={() => handleNavigate('admin')}
              >
                Admin Dashboard
              </button>
            )}
          </nav>
        )}

        {/* Desktop Actions */}
        <div className="header-actions header-actions-desktop">
          {user ? (
            <div className="user-info">
              <span className="welcome-text">Welcome, {user.username}</span>
              <button className="apple-btn-logout" onClick={handleLogout}>
                Logout
              </button>
            </div>
          ) : (
            <button className="apple-btn-login" onClick={handleLogin}>
              Login
            </button>
          )}
        </div>

        {/* Mobile Hamburger Button */}
        <button
          className={`hamburger-btn ${menuOpen ? 'is-active' : ''}`}
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle navigation menu"
        >
          <span className="hamburger-bar"></span>
          <span className="hamburger-bar"></span>
          <span className="hamburger-bar"></span>
        </button>
      </div>

      {/* Mobile Menu Dropdown */}
      {menuOpen && (
        <div className="header-mobile-menu">
          {user ? (
            <div className="mobile-menu-content">
              <div className="mobile-user-greeting">
                <span className="user-avatar">👤</span>
                <span className="user-name">Welcome, <strong>{user.username}</strong></span>
              </div>
              <button
                className={`mobile-menu-item ${currentView === 'booking' ? 'active' : ''}`}
                onClick={() => handleNavigate('booking')}
              >
                Bookings
              </button>
              {isAdmin && (
                <button
                  className={`mobile-menu-item ${currentView === 'admin' ? 'active' : ''}`}
                  onClick={() => handleNavigate('admin')}
                >
                  Admin Dashboard
                </button>
              )}
              <button className="mobile-menu-item mobile-logout-btn" onClick={handleLogout}>
                Logout
              </button>
            </div>
          ) : (
            <div className="mobile-menu-content">
              <button className="mobile-menu-item mobile-login-btn" onClick={handleLogin}>
                Login
              </button>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
