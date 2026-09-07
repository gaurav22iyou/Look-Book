import { useState } from 'react';
import BookingPage from './pages/BookingPage';
import AdminDashboard from './pages/AdminDashboard';
import ExplorePage from './pages/ExplorePage';
import { AuthProvider, useAuth } from './context/AuthContext';

function MainApp() {
  const { user } = useAuth();
  const [currentView, setCurrentView] = useState('explore');

  if (currentView === 'explore') {
    return <ExplorePage onNavigate={setCurrentView} />;
  }

  if (currentView === 'admin' && user?.username === 'admin') {
    return <AdminDashboard onNavigate={setCurrentView} />;
  }

  return <BookingPage onNavigate={setCurrentView} />;
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
