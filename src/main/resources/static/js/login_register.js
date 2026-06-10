// Read CSRF token written into the XSRF-TOKEN cookie by Spring Security.
function getCsrfToken() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
}

// Initialize
        document.addEventListener('DOMContentLoaded', function() {
            setupEventListeners();
            checkRememberedUser();
        });
        
        function setupEventListeners() {
            // Tab switching
            document.querySelectorAll('.auth-tab, .auth-switch-link').forEach(tab => {
                tab.addEventListener('click', function() {
                    const targetTab = this.getAttribute('data-tab');
                    switchTab(targetTab);
                });
            });
            
            // Password visibility toggle
            document.querySelectorAll('.toggle-password').forEach(btn => {
                btn.addEventListener('click', function() {
                    const targetId = this.getAttribute('data-target');
                    const input = document.getElementById(targetId);
                    const icon = this.querySelector('i');
                    
                    if (input.type === 'password') {
                        input.type = 'text';
                        icon.classList.remove('fa-eye');
                        icon.classList.add('fa-eye-slash');
                    } else {
                        input.type = 'password';
                        icon.classList.remove('fa-eye-slash');
                        icon.classList.add('fa-eye');
                    }
                });
            });
            
            // Form validation
            document.querySelectorAll('input[required]').forEach(input => {
                input.addEventListener('blur', validateField);
                input.addEventListener('input', function() {
                    if (this.classList.contains('is-invalid')) {
                        validateField.call(this);
                    }
                });
            });
            
            // Login form submission
            document.getElementById('loginForm').addEventListener('submit', loginUser);
            
            // Registration form submission
            document.getElementById('registerForm').addEventListener('submit', registerUser);
            
            // Social login buttons
            document.querySelector('.social-btn.google').addEventListener('click', function() {
                showNotification('Google login would be implemented here', 'info');
            });
            
            document.querySelector('.social-btn.facebook').addEventListener('click', function() {
                showNotification('Facebook login would be implemented here', 'info');
            });
            
            // Search functionality
            document.querySelector('.search-icon').addEventListener('click', showSearchModal);
            
            // Format phone number
            document.getElementById('phone').addEventListener('input', function() {
                this.value = this.value.replace(/[^0-9+]/g, '');
            });
            
            // Password validation on register
            document.getElementById('registerPassword').addEventListener('input', validatePassword);
            document.getElementById('confirmPassword').addEventListener('input', validatePasswordConfirmation);
        }
        
        function switchTab(tabName) {
            // Update tabs
            document.querySelectorAll('.auth-tab').forEach(tab => {
                tab.classList.remove('active');
                if (tab.getAttribute('data-tab') === tabName) {
                    tab.classList.add('active');
                }
            });
            
            // Update forms
            document.querySelectorAll('.auth-form').forEach(form => {
                form.classList.remove('active');
            });
            document.getElementById(tabName + 'Form').classList.add('active');
            
            // Update title
            const title = document.querySelector('.auth-title');
            const subtitle = document.querySelector('.auth-subtitle');
            
            if (tabName === 'login') {
                title.textContent = 'Welcome Back';
                subtitle.textContent = 'Sign in to your account or create a new one';
            } else {
                title.textContent = 'Create Account';
                subtitle.textContent = 'Join our community of elegant women';
            }
            
            // Hide success message
            document.getElementById('successMessage').style.display = 'none';
        }
        
        function validateField() {
            const field = this;
            const value = field.value.trim();
            let isValid = true;
            
            // Clear previous error
            field.classList.remove('is-invalid');
            if (field.nextElementSibling && field.nextElementSibling.classList.contains('invalid-feedback')) {
                field.nextElementSibling.style.display = 'none';
            }
            
            // Required field validation
            if (field.hasAttribute('required') && value === '') {
                isValid = false;
            }
            
            // Email validation
            if (field.type === 'email' && value !== '') {
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(value)) {
                    isValid = false;
                }
            }
            
            // Phone validation
            if (field.id === 'phone' && value !== '') {
                const phoneRegex = /^[0-9+]{10,15}$/;
                if (!phoneRegex.test(value)) {
                    isValid = false;
                }
            }
            
            if (!isValid) {
                field.classList.add('is-invalid');
                if (field.nextElementSibling && field.nextElementSibling.classList.contains('invalid-feedback')) {
                    field.nextElementSibling.style.display = 'block';
                }
                return false;
            }
            
            return true;
        }
        
		function validatePassword() {
		    const password = document.getElementById('registerPassword');
		    const value = password.value;
		    let isValid = true;
		    
		    // Clear previous error - FIXED
		    password.classList.remove('is-invalid');
		    
		    // Get the error message - FIXED: Go to parent then find error message
		    const passwordInputContainer = password.closest('.password-input');
		    const errorElement = passwordInputContainer?.nextElementSibling;
		    if (errorElement && errorElement.classList.contains('invalid-feedback')) {
		        errorElement.style.display = 'none';
		    }
		    
		    // Password strength validation
		    if (value.length < 8) {
		        isValid = false;
		    } else if (!/[a-zA-Z]/.test(value)) {
		        isValid = false;
		    } else if (!/[0-9]/.test(value)) {
		        isValid = false;
		    }
		    
		    if (!isValid) {
		        password.classList.add('is-invalid');
		        if (errorElement) {
		            errorElement.style.display = 'block';
		        }
		        return false;
		    }
		    
		    // Validate confirmation if it has value
		    const confirmPassword = document.getElementById('confirmPassword');
		    if (confirmPassword.value !== '') {
		        validatePasswordConfirmation();
		    }
		    
		    return true;
		}

		function validatePasswordConfirmation() {
		    const password = document.getElementById('registerPassword').value;
		    const confirmPassword = document.getElementById('confirmPassword');
		    const value = confirmPassword.value;
		    
		    // Clear previous error - FIXED
		    confirmPassword.classList.remove('is-invalid');
		    
		    // Get the error message - FIXED: Same approach
		    const confirmInputContainer = confirmPassword.closest('.password-input');
		    const errorElement = confirmInputContainer?.nextElementSibling;
		    if (errorElement && errorElement.classList.contains('invalid-feedback')) {
		        errorElement.style.display = 'none';
		    }
		    
		    if (value !== '' && value !== password) {
		        confirmPassword.classList.add('is-invalid');
		        if (errorElement) {
		            errorElement.style.display = 'block';
		        }
		        return false;
		    }
		    
		    return true;
		}
        
        function checkRememberedUser() {
            // Check if user was remembered
            const rememberedEmail = localStorage.getItem('rememberedEmail');
            if (rememberedEmail) {
                document.getElementById('loginEmail').value = rememberedEmail;
                document.getElementById('rememberMe').checked = true;
            }
        }
        
		function loginUser(e) {
		    e.preventDefault();
		    
		    const email = document.getElementById('loginEmail').value.trim();
		    const password = document.getElementById('loginPassword').value;
		    const rememberMe = document.getElementById('rememberMe').checked;
		    const loginBtn = document.getElementById('loginBtn');

		    // Validate form
		    let isValid = true;
		    document.querySelectorAll('#loginForm input[required]').forEach(input => {
		        if (!validateField.call(input)) {
		            isValid = false;
		        }
		    });

		    if (!isValid) {
		        showNotification('Please fill in all required fields correctly.', 'error');
		        return;
		    }

		    // Show loading state
		    loginBtn.innerHTML = '<span class="spinner"></span> Signing In...';
		    loginBtn.disabled = true;

		    // Make AJAX POST request to Spring Boot backend
		    fetch('/auth/login', {
		        method: 'POST',
		        headers: {
		            'Content-Type': 'application/x-www-form-urlencoded',
		            'X-XSRF-TOKEN': getCsrfToken()
		        },
		        body: new URLSearchParams({
		            email: email,
		            password: password,
		            rememberMe: rememberMe ? 'true' : 'false'
		        })
		    })
		    .then(response => response.json())
		    .then(payload => {
		        // Reset button
		        loginBtn.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i> Sign In';
		        loginBtn.disabled = false;

		        if (payload.status === 'success') {
		            // Server returns { status, message, data: { firstName, isAdmin } }
		            const firstName = payload.data ? payload.data.firstName : '';
		            const isAdmin   = !!(payload.data && payload.data.isAdmin);
		            const target    = isAdmin ? '/admin' : '/';
		            const where     = isAdmin ? 'admin dashboard' : 'home page';

		            let countdown = 1;
		            const notification = showNotification(
		                `${payload.message} Welcome, ${firstName}! Redirecting to ${where} in ${countdown} seconds...`,
		                'success'
		            );

		            if (rememberMe) {
		                localStorage.setItem('rememberedEmail', email);
		            } else {
		                localStorage.removeItem('rememberedEmail');
		            }

		            document.getElementById('loginEmail').value = '';
		            document.getElementById('loginPassword').value = '';

		            const countdownInterval = setInterval(() => {
		                countdown--;
		                if (countdown > 0) {
		                    if (notification && notification.querySelector('.notification-content')) {
		                        notification.querySelector('.notification-content').textContent =
		                            `${payload.message} Welcome, ${firstName}! Redirecting to ${where} in ${countdown} seconds...`;
		                    }
		                } else {
		                    clearInterval(countdownInterval);
		                    window.location.href = target;
		                }
		            }, 1000);

		        } else {
		            showNotification(payload.message, 'error');
		        }
		    })
		    .catch(err => {
		        console.error('Login error:', err);
		        loginBtn.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i> Sign In';
		        loginBtn.disabled = false;
		        showNotification('Something went wrong! Please try again.', 'error');
		    });
		}
        
        function registerUser(e) {
			e.preventDefault();
			
		    const firstName = document.getElementById('firstName').value.trim();
		    const lastName = document.getElementById('lastName').value.trim();
		    const email = document.getElementById('registerEmail').value.trim();
		    const phone = document.getElementById('phone').value.trim();
		    const password = document.getElementById('registerPassword').value;
		    const confirmPassword = document.getElementById('confirmPassword').value;
		    const registerBtn = document.getElementById('registerBtn');
			
		    // Validate all fields
		    let isValid = true;
		    document.querySelectorAll('#registerForm input[required]').forEach(input => {
		        if (!validateField.call(input)) {
		            isValid = false;
		        }
		    });
			
		    // Validate password strength
		    if (!validatePassword()) {
		        isValid = false;
		    }
			
		    // Validate password confirmation
		    if (password !== confirmPassword) {
		        document.getElementById('confirmPassword').classList.add('is-invalid');
		        document.getElementById('confirmPassword').nextElementSibling.nextElementSibling.style.display = 'block';
		        isValid = false;
		    }
			
		    if (!isValid) {
		        showNotification('Please fix the errors in the form.', 'error');
		        return;
		    }
			
		    // Show loading state with spinner
		    const originalText = registerBtn.innerHTML;
		    registerBtn.innerHTML = '<span class="spinner"></span> Creating Account...';
		    registerBtn.disabled = true;
			
		    // Make API call to your Spring Boot backend
		    fetch('/auth/register', {
		        method: 'POST',
		        headers: {
		            'Content-Type': 'application/x-www-form-urlencoded',
		            'X-XSRF-TOKEN': getCsrfToken()
		        },
		        body: new URLSearchParams({
		            email: email,
		            password: password,
		            firstName: firstName,
		            lastName: lastName,
		            phone: phone
		        })
		    })
		    .then(response => {
				if (!response.ok) {
		            // If status is 400 (Bad Request), it might be duplicate email
		            if (response.status === 400) {
		                return response.json().then(data => {
		                    // Check if it's a duplicate email error
		                    if (data.message && data.message.toLowerCase().includes('email')) {
		                        throw new Error('DUPLICATE_EMAIL');
		                    }
		                    throw new Error(data.message || 'Registration failed');
		                });
		            }
		            throw new Error(`HTTP error! status: ${response.status}`);
		        }
		        return response.json();
		    })
		    .then(data => {
		        // Reset button
		        registerBtn.innerHTML = originalText;
		        registerBtn.disabled = false;
		        
		        if (data.status === 'success') {
		            // Show success notification
		            showNotification(
		                `Account created successfully! Welcome to Mokshitha Collections, ${firstName}!`, 
		                'success'
		            );
		            
		            // Clear form
		            document.getElementById('registerForm').reset();
					
					// Wait 2 seconds, then switch to login tab
		            setTimeout(() => {
		                switchTab('login');
		                
		                // Pre-fill the email field
		                document.getElementById('loginEmail').value = email;
		                
		                // Show login prompt
		                showNotification('Please sign in with your new account', 'info');
		            }, 2000); // 2 second delay
		            
		            // Show countdown notification for redirect
		            /*let countdown = 3;
		            const redirectNotification = showNotificationWithCountdown(
		                `Registration successful!<br>Redirecting to login in ${countdown} seconds...`,
		                'success'
		            );
		            
		            // Update countdown every second
		            const countdownInterval = setInterval(() => {
		                countdown--;
		                if (countdown > 0) {
		                    updateNotificationCountdown(
		                        redirectNotification,
		                        `Registration successful!<br>Redirecting to login in ${countdown} seconds...`
		                    );
		                } else {
		                    clearInterval(countdownInterval);
		                    
		                    // Hide notification
		                    if (redirectNotification.parentNode) {
		                        redirectNotification.style.transform = 'translateX(150%)';
		                        setTimeout(() => {
		                            if (redirectNotification.parentNode) {
		                                document.body.removeChild(redirectNotification);
		                            }
		                        }, 300);
		                    }
		                    
		                    // Switch to login tab
		                    switchTab('login');
		                    
		                    // Pre-fill the email field
		                    document.getElementById('loginEmail').value = email;
		                    
		                    // Show login prompt
		                    setTimeout(() => {
		                        showNotification('Please sign in with your new account', 'info');
		                    }, 500);
		                }
		            }, 1000);*/
		            
		        } else {
		            // Show error from server
		            showNotification(data.message || 'Registration failed. Please try again.', 'error');
		        }
		    })
			.catch(error => {
		        console.error('Registration error:', error);
		        
		        // Reset button
		        registerBtn.innerHTML = originalText;
		        registerBtn.disabled = false;
		        
		        // Check for specific error types
		        if (error.message === 'DUPLICATE_EMAIL') {
		            // Show duplicate email error
		            showNotification('This email is already registered. Please use a different email or login.', 'error');
		            
		            // Highlight the email field
		            const emailInput = document.getElementById('registerEmail');
		            emailInput.classList.add('is-invalid');
		            
		            // Show email error message if exists
		            const emailError = emailInput.closest('.form-group')?.querySelector('.invalid-feedback');
		            if (emailError) {
		                emailError.textContent = 'This email is already registered';
		                emailError.style.display = 'block';
		            }
		            
		        } else if (error.name === 'TypeError' && error.message.includes('JSON')) {
		            // JSON parsing error
		            showNotification('Server error. Please try again later.', 'error');
		        } else {
		            // Generic error
		            showNotification(error.message || 'Something went wrong. Please try again later.', 'error');
		        }
		    });
		}
        
        // Common functions
        function showNotification(message, type = 'success') {
            const notification = document.createElement('div');
            notification.style.cssText = `
                position: fixed;
                top: 100px;
                right: 20px;
                background: ${type === 'error' ? '#ff6b6b' : 
                           type === 'info' ? '#17a2b8' : 
                           'linear-gradient(135deg, var(--dark-sea-green), var(--light-sea-green))'};
                color: white;
                padding: 15px 25px;
                border-radius: 8px;
                box-shadow: 0 5px 15px rgba(0,0,0,0.2);
                z-index: 10000;
                transform: translateX(150%);
                transition: transform 0.3s ease;
                max-width: 300px;
            `;
            notification.textContent = message;
            document.body.appendChild(notification);
            
            setTimeout(() => {
                notification.style.transform = 'translateX(0)';
            }, 10);
            
            setTimeout(() => {
                notification.style.transform = 'translateX(150%)';
                setTimeout(() => {
                    document.body.removeChild(notification);
                }, 300);
            }, 3000);
        }
        
        function showSearchModal() {
            const searchBox = document.createElement('div');
            searchBox.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0,0,0,0.8);
                z-index: 10001;
                display: flex;
                align-items: center;
                justify-content: center;
            `;
            
            searchBox.innerHTML = `
                <div style="background: white; padding: 30px; border-radius: 10px; width: 90%; max-width: 600px; position: relative;">
                    <button class="close-search" style="position: absolute; top: 15px; right: 15px; background: none; border: none; font-size: 1.5rem; cursor: pointer;">×</button>
                    <h3 style="margin-bottom: 20px; color: var(--dark-text);">Search Products</h3>
                    <div style="display: flex;">
                        <input type="text" placeholder="Search for sarees, dresses, collections..." 
                               style="flex: 1; padding: 15px; border: 2px solid #eee; border-radius: 5px 0 0 5px; font-size: 1rem;">
                        <button style="background: linear-gradient(135deg, var(--dark-sea-green), var(--light-sea-green)); color: white; border: none; padding: 0 30px; border-radius: 0 5px 5px 0; cursor: pointer;">
                            <i class="fas fa-search"></i>
                        </button>
                    </div>
                    <div style="margin-top: 20px; color: #666; font-size: 0.9rem;">
                        <p>Popular searches: <a href="#" style="color: var(--dark-sea-green); margin-right: 10px;">Silk Sarees</a> 
                        <a href="#" style="color: var(--dark-sea-green); margin-right: 10px;">Evening Dresses</a> 
                        <a href="#" style="color: var(--dark-sea-green);">Party Wear</a></p>
                    </div>
                </div>
            `;
            
            document.body.appendChild(searchBox);
            
            searchBox.querySelector('.close-search').addEventListener('click', function() {
                document.body.removeChild(searchBox);
            });
            
            searchBox.addEventListener('click', function(e) {
                if (e.target === searchBox) {
                    document.body.removeChild(searchBox);
                }
            });
        }
        
        // Mobile optimizations
        if (window.innerWidth <= 768) {
            // Make form inputs larger for mobile
            document.querySelectorAll('.form-control').forEach(input => {
                input.style.minHeight = '48px';
                input.style.fontSize = '16px';
            });
            
            // Make buttons larger
            document.querySelectorAll('button').forEach(btn => {
                btn.style.minHeight = '48px';
            });
            
            // Make checkboxes larger
            document.querySelectorAll('.form-check-input').forEach(input => {
                input.style.transform = 'scale(1.2)';
            });
        }