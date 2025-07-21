import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './App'; // Now importing the LoginPage component

describe('LoginPage Component', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  it('renders login form with all required elements', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Employee Login/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Username:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password:/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Login/i })).toBeInTheDocument();
  });

  it('shows error message on invalid login attempt', async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    
    fireEvent.change(screen.getByLabelText(/Username:/i), { 
      target: { value: 'wrong' } 
    });
    fireEvent.change(screen.getByLabelText(/Password:/i), { 
      target: { value: 'wrong' } 
    });
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));
    
    expect(await screen.findByText(/Invalid username or password/i)).toBeInTheDocument();
  });

  it('successfully logs in with admin credentials', async () => {
    const mockNavigate = jest.fn();
    jest.mock('react-router-dom', () => ({
      ...jest.requireActual('react-router-dom'),
      useNavigate: () => mockNavigate,
    }));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    
    fireEvent.change(screen.getByLabelText(/Username:/i), { 
      target: { value: 'admin' } 
    });
    fireEvent.change(screen.getByLabelText(/Password:/i), { 
      target: { value: 'adminpass' } 
    });
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));
    
    expect(localStorage.getItem('auth')).toEqual(JSON.stringify({
      username: 'admin',
      role: 'admin',
      empid: 11
    }));
    expect(mockNavigate).toHaveBeenCalledWith('/admin-dashboard');
  });

  it('successfully logs in with employee credentials', async () => {
    const mockNavigate = jest.fn();
    jest.mock('react-router-dom', () => ({
      ...jest.requireActual('react-router-dom'),
      useNavigate: () => mockNavigate,
    }));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
    
    fireEvent.change(screen.getByLabelText(/Username:/i), { 
      target: { value: 'snoopy' } 
    });
    fireEvent.change(screen.getByLabelText(/Password:/i), { 
      target: { value: 'emppass' } 
    });
    fireEvent.click(screen.getByRole('button', { name: /Login/i }));
    
    expect(localStorage.getItem('auth')).toEqual(JSON.stringify({
      username: 'snoopy',
      role: 'employee',
      empid: 1
    }));
    expect(mockNavigate).toHaveBeenCalledWith('/employee-dashboard');
  });
});