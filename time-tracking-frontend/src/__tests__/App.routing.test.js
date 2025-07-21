import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppWrapper from './App';

describe('App Routing', () => {
  it('redirects to admin dashboard on admin login', () => {
    localStorage.setItem('auth', JSON.stringify({
      username: 'admin',
      role: 'admin',
      empid: 11
    }));
    
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.queryByText(/Employee Login/i)).not.toBeInTheDocument();
  });
});