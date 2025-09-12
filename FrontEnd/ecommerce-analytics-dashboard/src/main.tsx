// src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';  // Vite uses React 18+ and the new root API
import App from './app';

// Create the root container for React
const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);

// Render your App component inside the root element
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
