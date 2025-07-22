import { render, screen } from '@testing-library/react';
import React from 'react';

function TestComponent() {
  return <div>Test</div>;
}

test('renders test component', () => {
  render(<TestComponent />);
  expect(screen.getByText('Test')).toBeInTheDocument();
});
