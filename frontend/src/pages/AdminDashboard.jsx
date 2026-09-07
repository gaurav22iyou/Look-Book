import { useState, useEffect } from 'react';
import Header from '../components/Header';
import { fetchSlots, createSlot, adminCancelBooking } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function AdminDashboard({ onNavigate }) {
  const { logout } = useAuth();
  
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const getTodayDateTimeLocal = () => {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    const localISOTime = new Date(now.getTime() - offset).toISOString().slice(0, 16);
    return localISOTime;
  };
  
  const [startTime, setStartTime] = useState(getTodayDateTimeLocal());
  const [endTime, setEndTime] = useState(getTodayDateTimeLocal());
  const [createMsg, setCreateMsg] = useState({ type: '', text: '' });
  const [isCreating, setIsCreating] = useState(false);

  const [bookingIdToCancel, setBookingIdToCancel] = useState('');
  const [cancelMsg, setCancelMsg] = useState({ type: '', text: '' });
  const [isCancelling, setIsCancelling] = useState(false);

  const loadSlots = () => {
    setLoading(true);
    fetchSlots()
      .then(data => {
        setSlots(data);
      })
      .catch(() => {
        // silently fail or log
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    loadSlots();
  }, []);

  const handleCreateSlot = async (e) => {
    e.preventDefault();
    setCreateMsg({ type: '', text: '' });
    
    if (!startTime || !endTime) {
      setCreateMsg({ type: 'error', text: 'Start time and end time are required.' });
      return;
    }
    if (new Date(endTime) <= new Date(startTime)) {
      setCreateMsg({ type: 'error', text: 'End time must be after start time.' });
      return;
    }

    setIsCreating(true);
    try {
      // Backend expects LocalDateTime. Ensure seconds are appended if datetime-local only outputs YYYY-MM-DDTHH:mm
      let st = startTime;
      let et = endTime;
      if (st.length === 16) st += ':00';
      if (et.length === 16) et += ':00';

      await createSlot(st, et);
      setCreateMsg({ type: 'success', text: 'Slot created successfully.' });
      setStartTime('');
      setEndTime('');
      loadSlots();
    } catch (err) {
      setCreateMsg({ type: 'error', text: err.message });
      if (err.status === 401) {
        logout();
        onNavigate('booking');
      }
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancelBooking = async (e) => {
    e.preventDefault();
    setCancelMsg({ type: '', text: '' });

    if (!bookingIdToCancel) return;

    setIsCancelling(true);
    try {
      await adminCancelBooking(bookingIdToCancel);
      setCancelMsg({ type: 'success', text: 'Booking cancelled successfully.' });
      setBookingIdToCancel('');
      loadSlots();
    } catch (err) {
      setCancelMsg({ type: 'error', text: err.message });
      if (err.status === 401) {
        logout();
        onNavigate('booking');
      }
    } finally {
      setIsCancelling(false);
    }
  };

  const formatTime = (dateString) => {
    return new Date(dateString).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
  };

  return (
    <div>
      <Header currentView="admin" onNavigate={onNavigate} />
      <main className="container">
        <h1 style={{ marginBottom: '2rem', fontSize: '1.5rem', color: 'var(--primary)' }}>Admin Dashboard</h1>
        
        <div className="admin-grid">
          {/* CREATE SLOT SECTION */}
          <section style={{ background: 'var(--surface)', padding: '1.5rem', borderRadius: '0.5rem', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column' }}>
          <h2 style={{ marginBottom: '1rem', fontSize: '1.25rem' }}>Create New Slot</h2>
          {createMsg.text && (
            <div style={{ marginBottom: '1rem', padding: '1rem', borderRadius: '0.375rem', backgroundColor: createMsg.type === 'error' ? '#fee2e2' : '#d1fae5', color: createMsg.type === 'error' ? 'var(--error)' : '#065f46' }}>
              {createMsg.text}
            </div>
          )}
          <form onSubmit={handleCreateSlot} className="admin-form">
            <div className="form-group" style={{ marginBottom: 0, flexGrow: 1 }}>
              <label>Start Time</label>
              <input type="datetime-local" className="date-input" value={startTime} onChange={e => setStartTime(e.target.value)} required />
            </div>
            <div className="form-group" style={{ marginBottom: 0, flexGrow: 1 }}>
              <label>End Time</label>
              <input type="datetime-local" className="date-input" value={endTime} onChange={e => setEndTime(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" disabled={isCreating}>
              {isCreating ? 'Creating...' : 'Create Slot'}
            </button>
          </form>
        </section>

          {/* CANCEL BOOKING SECTION */}
          <section style={{ background: 'var(--surface)', padding: '1.5rem', borderRadius: '0.5rem', border: '1px solid var(--border)', display: 'flex', flexDirection: 'column' }}>
          <h2 style={{ marginBottom: '0.5rem', fontSize: '1.25rem' }}>Admin Cancellation</h2>
          <p style={{ marginBottom: '0.75rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Cancel an active booking using its unique booking record identifier.
          </p>
          <div style={{ marginBottom: '1rem', padding: '0.75rem', borderRadius: '0.375rem', background: 'rgba(235, 211, 142, 0.08)', border: '1px solid rgba(235, 211, 142, 0.25)', fontSize: '0.8125rem', color: '#ebd38e', lineHeight: '1.4' }}>
            <strong>Important:</strong> Enter the ID from the <code>BOOKINGS</code> table. Slot ID and Booking ID are different database identifiers. Entering a Slot ID here will result in <em>&quot;Booking not found&quot;</em> unless a booking happens to share the same primary key.
          </div>
          
          {cancelMsg.text && (
            <div style={{ marginBottom: '1rem', padding: '1rem', borderRadius: '0.375rem', backgroundColor: cancelMsg.type === 'error' ? '#fee2e2' : '#d1fae5', color: cancelMsg.type === 'error' ? 'var(--error)' : '#065f46' }}>
              {cancelMsg.text}
            </div>
          )}

          <form onSubmit={handleCancelBooking} className="admin-form">
            <div className="form-group" style={{ marginBottom: 0, flexGrow: 1 }}>
              <label htmlFor="admin-cancel-booking-id">Booking ID (NOT Slot ID)</label>
              <input 
                id="admin-cancel-booking-id"
                type="number" 
                className="date-input" 
                value={bookingIdToCancel} 
                onChange={e => setBookingIdToCancel(e.target.value)} 
                placeholder="e.g. 1" 
                required 
              />
              <small style={{ display: 'block', marginTop: '0.25rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Enter the ID from the BOOKINGS table. Slot ID and Booking ID are different.
              </small>
            </div>
            <button type="submit" className="btn btn-outline" style={{ color: 'var(--error)', borderColor: 'var(--error)' }} disabled={isCancelling}>
              {isCancelling ? 'Cancelling...' : 'Cancel Booking'}
            </button>
          </form>
          </section>
        </div>

        {/* SLOTS LIST SECTION */}
        <section>
          <h2 style={{ marginBottom: '1rem', fontSize: '1.25rem' }}>All Slots Directory</h2>
          {loading ? (
            <div className="empty-state"><p>Loading slots...</p></div>
          ) : slots.length === 0 ? (
            <div className="empty-state"><p>No slots exist in the system.</p></div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {slots.map(slot => (
                <div key={slot.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem 1.25rem', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '0.5rem' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                      <span style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text)' }}>
                        Slot ID: <span style={{ color: 'var(--primary)' }}>{slot.id}</span>
                      </span>
                      <span style={{ color: 'var(--border)' }}>|</span>
                      <span style={{ fontSize: '0.95rem', fontWeight: 600, color: slot.activeBookingId ? '#ebd38e' : 'var(--text-muted)' }}>
                        Booking ID: <span>{slot.activeBookingId != null ? slot.activeBookingId : '—'}</span>
                      </span>
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {formatTime(slot.startTime)} to {formatTime(slot.endTime)}
                    </div>
                  </div>
                  <div className={`slot-status ${slot.status === 'AVAILABLE' ? 'status-available' : 'status-booked'}`}>
                    {slot.status}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
