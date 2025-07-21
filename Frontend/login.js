document.getElementById('login-form').addEventListener('submit', async function(event) {
    event.preventDefault();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    try {
        const response = await fetch('http://localhost:4567/api/login', {
 
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (data.success) {
            localStorage.setItem('employeeName', data.employeeName);
            localStorage.setItem('userRole', data.role);

            // Redirect based on role
            if (data.role === 'admin') {
                window.location.href = 'admin_dashboard.html';
            } else {
                window.location.href = 'employee_dashboard.html';
            }
        } else {
            document.getElementById('error-message').textContent = 'Invalid credentials';
        }
    } catch (error) {
        console.error('Login failed:', error);
        document.getElementById('error-message').textContent = 'Login failed. Please try again.';
    }
});
