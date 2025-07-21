import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppWrapper from './AppWrapper';

// Mock the Dashboard component
jest.mock('./pages/Dashboard', () => ({ role }) => (
  <div data-testid="dashboard">Mock Dashboard - {role}</div>
));

describe('AppWrapper Routing', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows login page by default', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Employee Login/i)).toBeInTheDocument();
    expect(screen.queryByTestId('dashboard')).not.toBeInTheDocument();
  });

  it('redirects to admin dashboard when authenticated as admin', () => {
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
    
    expect(screen.getByTestId('dashboard')).toBeInTheDocument();
    expect(screen.getByText(/Mock Dashboard - admin/i)).toBeInTheDocument();
    expect(screen.queryByText(/Employee Login/i)).not.toBeInTheDocument();
  });

  it('redirects to employee dashboard when authenticated as employee', () => {
    localStorage.setItem('auth', JSON.stringify({
      username: 'snoopy',
      role: 'employee',
      empid: 1
    }));
    
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByTestId('dashboard')).toBeInTheDocument();
    expect(screen.getByText(/Mock Dashboard - employee/i)).toBeInTheDocument();
    expect(screen.queryByText(/Employee Login/i)).not.toBeInTheDocument();
  });

  it('shows login page when accessing /admin-dashboard unauthenticated', () => {
    render(
      <MemoryRouter initialEntries={['/admin-dashboard']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Employee Login/i)).toBeInTheDocument();
    expect(screen.queryByTestId('dashboard')).not.toBeInTheDocument();
  });

  it('shows login page when accessing /employee-dashboard unauthenticated', () => {
    render(
      <MemoryRouter initialEntries={['/employee-dashboard']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Employee Login/i)).toBeInTheDocument();
    expect(screen.queryByTestId('dashboard')).not.toBeInTheDocument();
  });

  it('shows admin dashboard when accessing /admin-dashboard as admin', () => {
    localStorage.setItem('auth', JSON.stringify({
      username: 'admin',
      role: 'admin',
      empid: 11
    }));
    
    render(
      <MemoryRouter initialEntries={['/admin-dashboard']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByTestId('dashboard')).toBeInTheDocument();
    expect(screen.getByText(/Mock Dashboard - admin/i)).toBeInTheDocument();
  });

  it('shows employee dashboard when accessing /employee-dashboard as employee', () => {
    localStorage.setItem('auth', JSON.stringify({
      username: 'snoopy',
      role: 'employee',
      empid: 1
    }));
    
    render(
      <MemoryRouter initialEntries={['/employee-dashboard']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByTestId('dashboard')).toBeInTheDocument();
    expect(screen.getByText(/Mock Dashboard - employee/i)).toBeInTheDocument();
  });

  it('redirects to login when accessing protected route with wrong role', () => {
    localStorage.setItem('auth', JSON.stringify({
      username: 'snoopy',
      role: 'employee',
      empid: 1
    }));
    
    render(
      <MemoryRouter initialEntries={['/admin-dashboard']}>
        <AppWrapper />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Employee Login/i)).toBeInTheDocument();
    expect(screen.queryByTestId('dashboard')).not.toBeInTheDocument();
  });
});