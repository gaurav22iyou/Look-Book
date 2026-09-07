import { useAuth } from '../context/AuthContext';

export default function Header({ onLoginClick, currentView, onNavigate }) {
  const { user, logout } = useAuth();
  // We use user.username === 'admin' ONLY for UI navigation visibility, NOT as an authorization check. 
  // The backend remains the source of truth for all security rules.
  const isAdmin = user?.username === 'admin';

  return (
    <header className="header">
      <div className="header-content">
        <div className="header-title">
          Look<span className="text-gold">&amp;</span>Book
        </div>
        
        {user && (
          <nav className="header-nav">
            <button 
              className={`btn ${currentView === 'booking' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => onNavigate?.('booking')}
            >
              Bookings
            </button>
            {isAdmin && (
              <button 
                className={`btn ${currentView === 'admin' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => onNavigate?.('admin')}
              >
                Admin Dashboard
              </button>
            )}
          </nav>
        )}

        <div className="header-actions">
          {user ? (
            <div className="user-info">
              <span className="welcome-text">Welcome, {user.username}</span>
              <button className="btn btn-outline" onClick={() => {
                logout();
                onNavigate?.('booking');
              }}>
                Logout
              </button>
            </div>
          ) : (
            <button className="btn btn-outline" onClick={onLoginClick}>
              Login
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
