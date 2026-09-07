import { useState, useMemo, useEffect } from 'react';
import Header from '../components/Header';
import DateSelector from '../components/DateSelector';
import SlotList from '../components/SlotList';
import LoginModal from '../components/LoginModal';
import { fetchSlots, createBooking, cancelBooking, fetchMyBookings } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function BookingPage({ onNavigate }) {
  const { user, logout } = useAuth();
  
  const getTodayString = () => {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offset).toISOString().split('T')[0];
  };
  const [selectedDate, setSelectedDate] = useState(getTodayString());
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [pendingSlot, setPendingSlot] = useState(null);
  
  const [bookingSlotId, setBookingSlotId] = useState(null);
  const [actionMessage, setActionMessage] = useState({ type: '', text: '' }); 

  const [myBookings, setMyBookings] = useState([]);
  const [cancellingId, setCancellingId] = useState(null);

  const loadSlots = () => {
    setLoading(true);
    setError(null);
    fetchSlots()
      .then(data => {
        setSlots(data);
        setLoading(false);
      })
      .catch(err => {
        if (err.message.includes('401_UNAUTHORIZED')) {
          setError('Please log in to view available slots.');
        } else if (err.message.includes('Failed to fetch') || err.name === 'TypeError') {
          setError('Network error: Unable to reach the server. This may be a CORS issue or the server is down.');
        } else {
          setError('An error occurred while fetching slots.');
        }
        setLoading(false);
      });
  };

  useEffect(() => {
    loadSlots();
    if (user) {
      fetchMyBookings().then(setMyBookings).catch(console.error);
    } else {
      setMyBookings([]);
    }
  }, [user]);

  const filteredSlots = useMemo(() => {
    return slots.filter(slot => {
      if (!slot.startTime) return false;
      const slotDate = slot.startTime.split('T')[0];
      return slotDate === selectedDate;
    });
  }, [slots, selectedDate]);

  const executeBooking = async (slotId) => {
    setBookingSlotId(slotId);
    setActionMessage({ type: '', text: '' });
    
    try {
      const newBooking = await createBooking(slotId);
      setActionMessage({ type: 'success', text: 'Booking confirmed successfully.' });
      setMyBookings(prev => [...prev, newBooking]);
      loadSlots(); 
    } catch (err) {
      setActionMessage({ type: 'error', text: err.message });
      if (err.status === 401) {
        logout();
      } else if (err.status === 409) {
        loadSlots(); 
      }
    } finally {
      setBookingSlotId(null);
      setPendingSlot(null);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    setCancellingId(bookingId);
    setActionMessage({ type: '', text: '' });
    
    try {
      await cancelBooking(bookingId);
      setActionMessage({ type: 'success', text: 'Booking cancelled successfully.' });
      setMyBookings(prev => prev.filter(b => b.id !== bookingId));
      loadSlots(); 
    } catch (err) {
      setActionMessage({ type: 'error', text: err.message });
      if (err.status === 401) {
        logout();
      } else if (err.status === 404 || err.status === 409) {
        setMyBookings(prev => prev.filter(b => b.id !== bookingId));
        loadSlots();
      }
    } finally {
      setCancellingId(null);
    }
  };

  const handleBook = (slot) => {
    setActionMessage({ type: '', text: '' });
    if (!user) {
      setPendingSlot(slot);
      setIsLoginModalOpen(true);
    } else {
      executeBooking(slot.id);
    }
  };

  const handleLoginSuccess = () => {
    setIsLoginModalOpen(false);
    if (pendingSlot) {
      executeBooking(pendingSlot.id);
    }
  };

  const handleLoginCancel = () => {
    setIsLoginModalOpen(false);
    setPendingSlot(null);
  };

  const renderMyBookings = () => {
    if (!user) return null;
    return (
      <div style={{ marginBottom: '3rem' }}>
        <h2 style={{ marginBottom: '1rem', fontSize: '1.25rem', color: '#ffffff', textShadow: '0 4px 8px rgba(0,0,0,0.8)' }}>My Bookings (Current Session)</h2>
        {myBookings.length === 0 ? (
          <div className="empty-state" style={{ padding: '2rem' }}>
            <p style={{ marginBottom: '0.5rem' }}>You have no active bookings.</p>
          </div>
        ) : (
          <div className="slot-list">
            {myBookings.map(booking => {
              const associatedSlot = slots.find(s => s.id === booking.slotId) || {};
              const formatTime = (d) => d ? new Date(d).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '...';
              const datePart = associatedSlot.startTime ? associatedSlot.startTime.split('T')[0] : '...';
              
              return (
                <div key={booking.id} className="slot-card" style={{ borderLeft: '4px solid var(--primary)' }}>
                  <div className="slot-info">
                    <div style={{ fontWeight: '600', fontSize: '0.875rem', color: 'var(--text-muted)' }}>Booking #{booking.id}</div>
                    <div className="slot-time">{datePart} | {formatTime(associatedSlot.startTime)} - {formatTime(associatedSlot.endTime)}</div>
                    <div className="slot-status" style={{ backgroundColor: '#dbeafe', color: '#1e40af' }}>
                      {booking.status}
                    </div>
                  </div>
                  <div>
                    <button 
                      className="btn btn-outline"
                      style={{ color: 'var(--error)', borderColor: 'var(--error)' }}
                      disabled={cancellingId === booking.id}
                      onClick={() => handleCancelBooking(booking.id)}
                    >
                      {cancellingId === booking.id ? 'Cancelling...' : 'Cancel'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="booking-page-bg">
      <Header onLoginClick={() => setIsLoginModalOpen(true)} currentView="booking" onNavigate={onNavigate} />
      <main className="container" style={{ paddingBottom: '3rem', paddingTop: '1rem' }}>
        
        {actionMessage.text && (
          <div style={{ marginBottom: '1rem', padding: '1rem', borderRadius: '0.375rem', backgroundColor: actionMessage.type === 'error' ? '#fee2e2' : '#d1fae5', color: actionMessage.type === 'error' ? 'var(--error)' : '#065f46', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
            {actionMessage.text}
          </div>
        )}

        {renderMyBookings()}

        <h2 style={{ marginBottom: '1rem', fontSize: '1.25rem', color: '#ffffff', textShadow: '0 4px 8px rgba(0,0,0,0.8)' }}>Available Slots</h2>
        <div style={{ background: 'var(--surface)', padding: '1rem', borderRadius: '0.5rem', marginBottom: '2rem', backdropFilter: 'blur(8px)', border: '1px solid var(--border)', boxShadow: '0 10px 30px rgba(0,0,0,0.35)' }}>
          <DateSelector selectedDate={selectedDate} onChange={setSelectedDate} />
        </div>

        {loading ? (
          <div className="empty-state">
            <p>Loading available slots...</p>
          </div>
        ) : error ? (
          <div className="empty-state" style={{ color: 'var(--error)', borderColor: 'var(--error)' }}>
            <p>{error}</p>
          </div>
        ) : (
          <SlotList slots={filteredSlots} onBook={handleBook} bookingSlotId={bookingSlotId} />
        )}
      </main>

      <LoginModal 
        isOpen={isLoginModalOpen} 
        onClose={handleLoginCancel} 
        onSuccess={handleLoginSuccess}
      />
    </div>
  );
}
