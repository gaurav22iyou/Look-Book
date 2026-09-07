import React from 'react';

export default function ExplorePage({ onNavigate }) {
  return (
    <div className="explore-page">
      <div className="explore-content">
        <h1 className="explore-title">
          Look <span className="text-gold">&amp;</span> Book
        </h1>
        <p className="explore-subtitle-1">D I S C O V E R &nbsp;&nbsp;&nbsp; B O O K &nbsp;&nbsp;&nbsp; E X P E R I E N C E</p>
        <p className="explore-subtitle-2">Your next experience is just a booking away.</p>
        <button className="btn-explore" onClick={() => onNavigate('booking')}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          Explore Now
        </button>
      </div>
      <div className="explore-footer">
        Browse. Book. Belong.
      </div>
    </div>
  );
}
