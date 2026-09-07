export default function DateSelector({ selectedDate, onChange }) {
  return (
    <div className="date-selector">
      <label htmlFor="date-input">Select Date</label>
      <input 
        id="date-input"
        type="date" 
        className="date-input" 
        value={selectedDate}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}
