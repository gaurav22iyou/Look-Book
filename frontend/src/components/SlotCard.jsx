export default function SlotCard({ slot, onBook, isBooking }) {
  const isAvailable = slot.status === 'AVAILABLE';
  
  const formatTime = (dateString) => {
    return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const timeRange = `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`;

  return (
    <div className="slot-card">
      <div className="slot-info">
        <div className="slot-time">{timeRange}</div>
        <div className={`slot-status ${isAvailable ? 'status-available' : 'status-booked'}`}>
          {isAvailable ? 'Available' : 'Booked'}
        </div>
      </div>
      <div>
        {isAvailable ? (
          <button 
            className="btn btn-primary" 
            onClick={() => onBook(slot)}
            disabled={isBooking}
          >
            {isBooking ? 'Booking...' : 'BOOK'}
          </button>
        ) : (
          <button className="btn btn-primary" disabled>
            BOOKED
          </button>
        )}
      </div>
    </div>
  );
}
