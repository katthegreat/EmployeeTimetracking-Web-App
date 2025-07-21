import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { LoginPage } from '../App';
import { useNavigate } from 'react-router-dom';
import '@testing-library/jest-dom';

// Mock useNavigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Mock localStorage
const localStorageMock = (function() {
  let store = {};
  return {
    getItem: jest.fn((key) => store[key]),
    setItem: jest.fn((key, value) => {
      store[key] = value.toString();
    }),
    clear: jest.fn(() => {
      store = {};
    }),
    removeItem: jest.fn((key) => {
      delete store[key];
    })
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
});

describe('LoginPage Component', () => {
  beforeEach(() => {
    // Clear all mocks and localStorage before each test
    jest.clearAllMocks();
    window.localStorage.clear();
  });

  it('renders login form correctly', () => {
    render(<LoginPage />);
    
    expect(screen.getByText('Employee Login')).toBeInTheDocument();
    expect(screen.getByLabelText('Username:')).toBeInTheDocument();
    expect(screen.getByLabelText('Password:')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument();
  });

  it('updates form data on input change', () => {
    render(<LoginPage />);
    
    const usernameInput = screen.getByLabelText('Username:');
    const passwordInput = screen.getByLabelText('Password:');

    fireEvent.change(usernameInput, { target: { value: 'admin' } });
    fireEvent.change(passwordInput, { target: { value: 'adminpass' } });

    expect(usernameInput.value).toBe('admin');
    expect(passwordInput.value).toBe('adminpass');
  });

  it('shows error on invalid login', () => {
    render(<LoginPage />);
    
    fireEvent.change(screen.getByLabelText('Username:'), { target: { value: 'wrong' } });
    fireEvent.change(screen.getByLabelText('Password:'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    expect(screen.getByText('Invalid username or password')).toBeInTheDocument();
    expect(window.localStorage.setItem).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('navigates to admin dashboard on successful admin login', () => {
    render(<LoginPage />);
    
    fireEvent.change(screen.getByLabelText('Username:'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('Password:'), { target: { value: 'adminpass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    expect(window.localStorage.setItem).toHaveBeenCalledWith(
      'auth',
      JSON.stringify({
        username: 'admin',
        role: 'admin',
        empid: 11
      })
    );
    expect(mockNavigate).toHaveBeenCalledWith('/admin-dashboard');
  });

  it('navigates to employee dashboard on successful employee login', () => {
    render(<LoginPage />);
    
    fireEvent.change(screen.getByLabelText('Username:'), { target: { value: 'snoopy' } });
    fireEvent.change(screen.getByLabelText('Password:'), { target: { value: 'emppass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    expect(window.localStorage.setItem).toHaveBeenCalledWith(
      'auth',
      JSON.stringify({
        username: 'snoopy',
        role: 'employee',
        empid: 1
      })
    );
    expect(mockNavigate).toHaveBeenCalledWith('/employee-dashboard');
  });
});