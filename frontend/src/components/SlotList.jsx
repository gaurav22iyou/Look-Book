import SlotCard from './SlotCard';

export default function SlotList({ slots, onBook, bookingSlotId }) {
  if (!slots || slots.length === 0) {
    return (
      <div className="empty-state">
        <p>No slots available for the selected date.</p>
      </div>
    );
  }

  return (
    <div className="slot-list">
      {slots.map(slot => (
        <SlotCard 
          key={slot.id} 
          slot={slot} 
          onBook={onBook} 
          isBooking={bookingSlotId === slot.id} 
        />
      ))}
    </div>
  );
}
