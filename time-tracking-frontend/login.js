document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    try {
        const response = await fetch('http://localhost:4567/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        
        if (data.success) {
            localStorage.setItem('employeeName', data.employeeName);
            localStorage.setItem('userRole', data.role);
            window.location.href = 'dashboard.html';
        } else {
            document.getElementById('error-message').textContent = 
                'Invalid credentials';
        }
    } catch (error) {
        console.error('Login failed:', error);
        document.getElementById('error-message').textContent = 
            'Login failed. Please try again.';
    }
});