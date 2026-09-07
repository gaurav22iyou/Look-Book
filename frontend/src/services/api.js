const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

let globalAuthHeader = null;

export function setAuthCredentials(header) {
  globalAuthHeader = header;
}

export function clearAuthCredentials() {
  globalAuthHeader = null;
}

export async function apiLogin(authHeader) {
  try {
    const response = await fetch(`${API_URL}/slots`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Authorization': authHeader
      }
    });

    if (response.status === 401) {
      throw new Error('Invalid username or password.');
    }
    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    if (error.message && error.message.includes('Failed to fetch') || error.name === 'TypeError') {
      throw new Error('Unable to connect to the server. Please try again.');
    }
    throw error;
  }
}

export async function fetchSlots() {
  const headers = {
    'Accept': 'application/json'
  };
  
  if (globalAuthHeader) {
    headers['Authorization'] = globalAuthHeader;
  }

  const response = await fetch(`${API_URL}/slots`, {
    method: 'GET',
    headers
  });

  if (response.status === 401) {
    throw new Error('401_UNAUTHORIZED');
  }
  
  if (!response.ok) {
    throw new Error(`API_ERROR: ${response.status}`);
  }
  return await response.json();
}

export async function fetchMyBookings() {
  const headers = {
    'Accept': 'application/json'
  };
  
  if (globalAuthHeader) {
    headers['Authorization'] = globalAuthHeader;
  }

  const response = await fetch(`${API_URL}/bookings`, {
    method: 'GET',
    headers
  });

  if (response.status === 401) {
    throw new Error('401_UNAUTHORIZED');
  }
  
  if (!response.ok) {
    throw new Error(`API_ERROR: ${response.status}`);
  }

  return await response.json();
}

export async function createBooking(slotId) {
  try {
    const headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };
    if (globalAuthHeader) headers['Authorization'] = globalAuthHeader;

    const response = await fetch(`${API_URL}/bookings`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ slotId })
    });

    if (response.status === 401) {
      const err = new Error('Session expired or not authorized. Please log in again.');
      err.status = 401;
      throw err;
    }
    if (response.status === 403) {
      const err = new Error('You are not authorized to perform this action.');
      err.status = 403;
      throw err;
    }
    if (response.status === 404) {
      const err = new Error('Slot not found.');
      err.status = 404;
      throw err;
    }
    if (response.status === 409) {
      const err = new Error('This slot was just booked by another user. Please choose another slot.');
      err.status = 409;
      throw err;
    }
    
    if (!response.ok) {
      let errorMsg = `Server error: ${response.status}`;
      try {
        const errJson = await response.json();
        if (errJson.message) errorMsg = errJson.message;
      } catch { }
      const err = new Error(errorMsg);
      err.status = response.status;
      throw err;
    }

    return await response.json();
  } catch (error) {
    if (error.status) throw error;
    throw new Error('Unable to connect to the server. Please try again.');
  }
}

export async function cancelBooking(bookingId) {
  try {
    const headers = {
      'Accept': 'application/json'
    };
    if (globalAuthHeader) headers['Authorization'] = globalAuthHeader;

    const response = await fetch(`${API_URL}/bookings/${bookingId}/cancel`, {
      method: 'POST',
      headers
    });

    if (response.status === 401) {
      const err = new Error('Session expired or not authorized. Please log in again.');
      err.status = 401;
      throw err;
    }
    if (response.status === 403) {
      const err = new Error('You are not authorized to cancel this booking.');
      err.status = 403;
      throw err;
    }
    if (response.status === 404) {
      const err = new Error('Booking not found.');
      err.status = 404;
      throw err;
    }
    if (response.status === 409) {
      const err = new Error('This booking can no longer be cancelled.');
      err.status = 409;
      throw err;
    }
    
    if (!response.ok) {
      let errorMsg = `Server error: ${response.status}`;
      try {
        const errJson = await response.json();
        if (errJson.message) errorMsg = errJson.message;
      } catch { }
      const err = new Error(errorMsg);
      err.status = response.status;
      throw err;
    }

    return await response.json();
  } catch (error) {
    if (error.status) throw error;
    throw new Error('Unable to connect to the server. Please try again.');
  }
}

export async function createSlot(startTime, endTime) {
  try {
    const headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };
    if (globalAuthHeader) headers['Authorization'] = globalAuthHeader;

    const response = await fetch(`${API_URL}/slots`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ startTime, endTime })
    });

    if (response.status === 400) {
      const err = new Error('Invalid slot data provided.');
      err.status = 400;
      throw err;
    }
    if (response.status === 401) {
      const err = new Error('Session expired or not authorized. Please log in again.');
      err.status = 401;
      throw err;
    }
    if (response.status === 403) {
      const err = new Error('You are not authorized to create slots.');
      err.status = 403;
      throw err;
    }

    if (!response.ok) {
      let errorMsg = `Server error: ${response.status}`;
      try {
        const errJson = await response.json();
        if (errJson.message) errorMsg = errJson.message;
      } catch { }
      const err = new Error(errorMsg);
      err.status = response.status;
      throw err;
    }

    return await response.json();
  } catch (error) {
    if (error.status) throw error;
    throw new Error('Unable to connect to the server. Please try again.');
  }
}

export async function adminCancelBooking(bookingId) {
  try {
    const headers = {
      'Accept': 'application/json'
    };
    if (globalAuthHeader) headers['Authorization'] = globalAuthHeader;

    const response = await fetch(`${API_URL}/admin/bookings/${bookingId}/cancel`, {
      method: 'POST',
      headers
    });

    if (response.status === 401) {
      const err = new Error('Session expired or not authorized. Please log in again.');
      err.status = 401;
      throw err;
    }
    if (response.status === 403) {
      const err = new Error('You are not authorized to cancel this booking.');
      err.status = 403;
      throw err;
    }
    if (response.status === 404) {
      const err = new Error('Booking not found.');
      err.status = 404;
      throw err;
    }
    if (response.status === 409) {
      const err = new Error('This booking can no longer be cancelled.');
      err.status = 409;
      throw err;
    }

    if (!response.ok) {
      let errorMsg = `Server error: ${response.status}`;
      try {
        const errJson = await response.json();
        if (errJson.message) errorMsg = errJson.message;
      } catch { }
      const err = new Error(errorMsg);
      err.status = response.status;
      throw err;
    }

    return await response.json();
  } catch (error) {
    if (error.status) throw error;
    throw new Error('Unable to connect to the server. Please try again.');
  }
}
