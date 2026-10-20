import React from 'react';

export default function ExplorePage({ onNavigate }) {
  return (
    <div className="explore-page">
      <div className="explore-content">
        <h1 className="explore-title">
          <span className="explore-title-white">Book</span>
          <span className="brand-gold-wrap">
            <span className="brand-gold-text">Now</span>
            <svg
              className="airplane-logo-icon"
              viewBox="0 0 100 60"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <defs>
                <linearGradient id="planeGoldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#fffda6" />
                  <stop offset="45%" stopColor="#ffd000" />
                  <stop offset="100%" stopColor="#ffc400" />
                </linearGradient>
                <filter id="planeGlow" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="1" stdDeviation="1.5" floodColor="#ffd000" floodOpacity="0.45" />
                </filter>
              </defs>
              {/* Swoosh trail curving from bottom-right of 'w' */}
              <path
                d="M 1 40 C 26 55, 54 48, 72 23"
                stroke="url(#planeGoldGrad)"
                strokeWidth="2.8"
                strokeLinecap="round"
              />
              {/* Jet Airplane angled up-right towards 45° */}
              <g transform="translate(80, 15) rotate(45)" filter="url(#planeGlow)">
                <path
                  d="M 0 -15
                     C 1.8 -15, 2.8 -10, 2.8 -4
                     L 2.8 0
                     L 15 4
                     C 16 4.3, 16 5.5, 15 6
                     L 2.8 4.6
                     L 2.8 11.5
                     L 7.5 14.5
                     C 8 14.8, 8 15.5, 7.5 15.8
                     L 0 14.2
                     L -7.5 15.8
                     C -8 15.5, -8 14.8, -7.5 14.5
                     L -2.8 11.5
                     L -2.8 4.6
                     L -15 6
                     C -16 5.5, -16 4.3, -15 4
                     L -2.8 0
                     L -2.8 -4
                     C -2.8 -10, -1.8 -15, 0 -15 Z"
                  fill="url(#planeGoldGrad)"
                />
              </g>
            </svg>
          </span>
        </h1>
        <p className="explore-subtitle-1">
          D I S C O V E R <span className="tagline-dot">•</span> B O O K <span className="tagline-dot">•</span> E X P E R I E N C E
        </p>
        <p className="explore-subtitle-2">
          Your <span className="text-gold-highlight">next experience</span> is just a booking away.
        </p>
        <button className="btn-explore" onClick={() => onNavigate('booking')}>
          <svg className="btn-icon-search" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <span>Explore Now</span>
          <svg className="btn-icon-arrow" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </button>
      </div>
      <div className="explore-footer">
        Browse. Book. Belong.
      </div>
    </div>
  );
}
