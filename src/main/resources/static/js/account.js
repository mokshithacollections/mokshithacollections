// Read CSRF token written into the XSRF-TOKEN cookie by Spring Security.
function getCsrfToken() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
}

// Initialize
        document.addEventListener('DOMContentLoaded', function() {
            setupEventListeners();
            checkEmptyWishlist();
            restoreActiveTab();
        });

        // Wire PIN auto-fill independently so an error in the init code above
        // can never stop it from attaching. Runs whether or not DOM is ready.
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', wirePincodeLookups);
        } else {
            wirePincodeLookups();
        }

        // ---- Auto-fill City + State from the PIN code (India Post public API) ----
        // Free, no key, CORS-enabled: https://api.postalpincode.in/pincode/{PIN}
        function wirePincodeLookups() {
            ['addAddressForm', 'editAddressForm'].forEach(function (formId) {
                const form = document.getElementById(formId);
                if (!form) return;
                const pin = form.querySelector('[name="pinCode"]');
                const city = form.querySelector('[name="city"]');
                const district = form.querySelector('[name="district"]');
                const state = form.querySelector('[name="state"]');
                const status = form.querySelector('.pincode-status');
                if (!pin) return;
                let timer;
                pin.addEventListener('input', function () {
                    // Digits only, max 6 — strips any letters/symbols as they're typed/pasted.
                    pin.value = pin.value.replace(/[^0-9]/g, '').slice(0, 6);
                    const v = pin.value;
                    clearTimeout(timer);
                    if (/^\d{6}$/.test(v)) {
                        timer = setTimeout(() => lookupPincode(v, city, district, state, status), 350);
                    } else {
                        // Incomplete pincode → reset City/District/State to fresh defaults.
                        if (city) city.value = '';
                        if (district) district.value = '';
                        if (state) state.value = '';
                        if (status) status.textContent = '';
                        if (typeof syncDeliveryGroup === 'function') syncDeliveryGroup(form);
                    }
                });
            });
        }

        // Show the Home Delivery / Store Collect radios only when the city is the
        // store's city (Ongole); otherwise hide them and force HOME_DELIVERY.
        function syncDeliveryGroup(form) {
            if (!form) return;
            const group = form.querySelector('.delivery-method-group');
            if (!group) return;
            const cityInput = form.querySelector('[name="city"]');
            const isStoreCity = cityInput && cityInput.value.trim().toLowerCase() === 'ongole';
            group.style.display = isStoreCity ? '' : 'none';
            if (!isStoreCity) {
                const home = form.querySelector('input[name="deliveryMethod"][value="HOME_DELIVERY"]');
                if (home) home.checked = true;
            }
        }

        async function lookupPincode(pin, cityEl, districtEl, stateEl, statusEl) {
            if (statusEl) { statusEl.textContent = 'Looking up PIN…'; statusEl.style.color = '#666'; }
            const result = await fetchPincode(pin);
            console.log('[pincode]', pin, '→ city:', result && result.city, '| district:', result && result.district, '| state:', result && result.state);
            if (!result || (!result.city && !result.state)) {
                if (statusEl) { statusEl.textContent = 'PIN not found — please enter city/state manually.'; statusEl.style.color = '#dc2626'; }
                return;
            }
            // Town (city) + district; fill them in (user can still edit).
            if (cityEl && result.city) cityEl.value = result.city;
            if (districtEl && result.district) districtEl.value = result.district;
            // City drives whether store pickup is offered.
            if (cityEl) syncDeliveryGroup(cityEl.form);
            // State = matching dropdown option (case-insensitive).
            let stateMatched = false;
            if (stateEl && result.state) {
                const target = String(result.state).trim().toLowerCase();
                const opt = Array.from(stateEl.options).find(o => o.value.trim().toLowerCase() === target);
                if (opt) { stateEl.value = opt.value; stateMatched = true; }
            }
            if (statusEl) {
                statusEl.style.color = 'var(--dark-sea-green)';
                statusEl.textContent = '✓ ' + (result.city || '') + ', ' + (result.state || '')
                    + (stateMatched ? '' : ' (please pick the state)');
            }
        }

        // Mirror the server: HO → (single SO) → Block → (multi SO) → District.
        function townFromOffices(offices) {
            const real = s => s && s !== 'NA' && String(s).trim() !== '';
            const strip = n => String(n || '').replace(/\s*(H\.?O|S\.?O|B\.?O|G\.?P\.?O|GPO|\(O\)|Bazar|Bazaar)\.?$/i, '').trim();
            const head = offices.find(o => String(o.BranchType || '').toLowerCase().includes('head'));
            if (head && real(strip(head.Name))) return strip(head.Name);
            const subs = offices.filter(o => String(o.BranchType || '').toLowerCase().includes('sub'))
                                 .map(o => strip(o.Name)).filter(real);
            if (subs.length === 1) return subs[0];   // one SO = a town
            const blk = offices.find(o => real(o.Block));
            if (blk) return blk.Block;               // many SOs = localities → use the Block
            if (subs.length) return subs[0];
            return offices[0].District || '';
        }

        // Try our same-origin proxy first; fall back to India Post directly.
        async function fetchPincode(pin) {
            try {
                const res = await fetch('/api/pincode/' + pin, { headers: { 'Accept': 'application/json' } });
                if (res.ok) {
                    const d = await res.json();
                    if (d && d.found) return { city: d.city, district: d.district, state: d.state };
                }
            } catch (_) { /* fall through to direct call */ }
            try {
                const res = await fetch('https://api.postalpincode.in/pincode/' + pin);
                const arr = await res.json();
                const rec = Array.isArray(arr) ? arr[0] : null;
                const offices = (rec && rec.Status === 'Success' && Array.isArray(rec.PostOffice)) ? rec.PostOffice : null;
                if (offices && offices.length) {
                    return { city: townFromOffices(offices), district: offices[0].District, state: offices[0].State };
                }
            } catch (_) { /* give up — manual entry */ }
            return null;
        }

        // After an address add/edit/delete, the page reloads to show the new
        // state. We stash the active tab in localStorage so the user lands
        // back on the Addresses tab instead of the default Dashboard.
        function restoreActiveTab() {
            const stored = localStorage.getItem('activeAccountTab');
            if (!stored) return;
            const link = document.querySelector(`.menu-link[data-content="${stored}"]`);
            const content = document.getElementById(stored);
            if (link && content) {
                document.querySelectorAll('.menu-link').forEach(l => l.classList.remove('active'));
                link.classList.add('active');
                document.querySelectorAll('.account-content').forEach(c => c.classList.remove('active'));
                content.classList.add('active');
            }
            // Consume the flag so a manual refresh doesn't keep forcing this tab.
            localStorage.removeItem('activeAccountTab');
        }
        
        function setupEventListeners() {
            // Menu navigation
            document.querySelectorAll('.menu-link, .card-link[data-content]').forEach(link => {
                link.addEventListener('click', function(e) {
                    e.preventDefault();
                    const contentId = this.getAttribute('data-content') || 
                                     this.getAttribute('href').substring(1);
                    
                    // Update active menu item
                    document.querySelectorAll('.menu-link').forEach(item => {
                        item.classList.remove('active');
                    });
                    this.classList.add('active');
                    
                    // Show selected content
                    document.querySelectorAll('.account-content').forEach(content => {
                        content.classList.remove('active');
                    });
                    document.getElementById(contentId).classList.add('active');
                    
                    // Scroll to top of content
                    document.getElementById(contentId).scrollIntoView({ behavior: 'smooth', block: 'start' });
                });
            });
            
            // Logout button
            document.querySelector('.logout-btn').addEventListener('click', function() {
				fetch('/auth/logout', {
	                method: 'POST',
	                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
	            })
	            .then(response => response.json())
	            .then(data => {
	                if (data.status === 'success') {
	                    window.location.href = '/'; // Refresh the header
	                }
	            })
	            .catch(err => {
	                console.error('Logout error:', err);
	                window.location.href = '/'; // Refresh anyway
	            });
            });
            
            // Avatar edit
            const avatarEdit = document.querySelector('.avatar-edit');
            if (avatarEdit) {
                avatarEdit.addEventListener('click', function() {
                    showNotification('Avatar upload feature would be implemented here', 'info');
                });
            }

            // Add all to cart — note: this button only exists when the wishlist
            // has items (the template wraps it in th:if). On an empty wishlist
            // getElementById returns null, and calling .addEventListener on null
            // throws and KILLS THE REST of setupEventListeners (which is what
            // was breaking the Add Address form's submit handler from getting
            // registered).
            const addAllBtn = document.getElementById('addAllToCart');
            if (addAllBtn) addAllBtn.addEventListener('click', async function() {
                const cards = Array.from(document.querySelectorAll('.wishlist-card'));
                if (cards.length === 0) {
                    showNotification('Your wishlist is empty', 'error');
                    return;
                }
                this.disabled = true;
                let added = 0, failed = 0;
                // Add each saved variant to the cart for real, then move it out
                // of the wishlist (same "move to cart" behaviour as the per-item button).
                for (const card of cards) {
                    const variantId = card.dataset.variantId;
                    if (!variantId) continue;
                    try {
                        const res = await fetch('/api/cart/items', {
                            method: 'POST',
                            headers: { 'X-XSRF-TOKEN': getCsrfToken(), 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: new URLSearchParams({ variantId: variantId, quantity: 1 })
                        });
                        if (res.status === 401) { window.location.href = '/user_redirect'; return; }
                        if (res.ok) {
                            const data = await res.json();
                            const cartBadge = document.querySelector('.cart-icon .cart-count');
                            if (cartBadge && typeof data.totalQuantity === 'number') cartBadge.textContent = data.totalQuantity;
                            // Remove from wishlist now that it's in the cart.
                            try {
                                await fetch('/wishlist/toggle/' + variantId, {
                                    method: 'POST', headers: { 'X-XSRF-TOKEN': getCsrfToken() }
                                });
                            } catch (_) { /* ignore */ }
                            if (typeof dropWishlistCard === 'function') dropWishlistCard(variantId);
                            added++;
                        } else { failed++; }
                    } catch (_) { failed++; }
                }
                this.disabled = false;
                if (added > 0) showNotification(`Added ${added} item${added === 1 ? '' : 's'} to cart!`
                    + (failed ? ` (${failed} could not be added)` : ''), 'success');
                else showNotification('Could not add items to cart (out of stock?)', 'error');
            });
			
			// Helper: surface real error messages from the backend instead of
			// the generic "Failed to ..." that hid every validation problem.
			async function readError(res) {
			    let body = null;
			    try { body = await res.json(); } catch (_) {}
			    return (body && body.message) ? body.message : ('HTTP ' + res.status);
			}

			document.querySelectorAll('.address-actions .action-btn').forEach(btn => {
			    btn.addEventListener('click', async function() {
			        const addressId = this.dataset.id;
			        const action = this.textContent.trim();

					if (action === 'Remove') {
					    if (!confirm('Are you sure you want to remove this address?')) return;
					    try {
					        const res = await fetch(`/account/address/delete/${addressId}`, {
					            method: 'POST',
					            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
					        });
					        if (!res.ok) { showNotification(await readError(res), 'error'); return; }
					        showNotification('Address removed', 'success');
					        localStorage.setItem('activeAccountTab', 'addresses');
					        setTimeout(() => location.reload(), 600);
					    } catch (e) {
					        showNotification('Network error removing address', 'error');
					    }
					}
					else if (action === 'Set as Default') {
					    try {
					        const res = await fetch(`/account/address/default/${addressId}`, {
					            method: 'POST',
					            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
					        });
					        if (!res.ok) { showNotification(await readError(res), 'error'); return; }
					        showNotification('Default address updated', 'success');
					        localStorage.setItem('activeAccountTab', 'addresses');
					        setTimeout(() => location.reload(), 600);
					    } catch (e) {
					        showNotification('Network error', 'error');
					    }
					}
			        else if (action === 'Edit') {
			            // Prefill modal from data-* attributes
			            document.getElementById('editAddressId').value = addressId;
			            document.getElementById('editFirstName').value = this.dataset.firstname;
			            document.getElementById('editLastName').value = this.dataset.lastname;
			            document.getElementById('editPhone').value = this.dataset.phone;
			            document.getElementById('editStreetAddress').value = this.dataset.street;
			            document.getElementById('editCity').value = this.dataset.city;
			            document.getElementById('editState').value = this.dataset.state;
			            document.getElementById('editPinCode').value = this.dataset.pincode;
			            document.getElementById('editDistrict').value = this.dataset.district || '';
			            document.getElementById('editCountry').value = this.dataset.country;

			            document.getElementById('editTypeHome').checked = this.dataset.type === 'HOME';
			            document.getElementById('editTypeWork').checked = this.dataset.type === 'WORK';
			            document.getElementById('editTypeOther').checked = this.dataset.type === 'OTHER';

			            document.getElementById('editIsDefault').checked = this.dataset.default === 'true';

			            // Delivery method: pick the saved radio, then show/hide by city.
			            const editForm = document.getElementById('editAddressForm');
			            const dm = (this.dataset.delivery === 'STORE_COLLECT') ? 'STORE_COLLECT' : 'HOME_DELIVERY';
			            const dmRadio = editForm.querySelector('input[name="deliveryMethod"][value="' + dm + '"]');
			            if (dmRadio) dmRadio.checked = true;
			            syncDeliveryGroup(editForm);

			            // Show modal
			            const editModal = new bootstrap.Modal(document.getElementById('editAddressModal'));
			            editModal.show();
			        }
			    });
			});
			
			// Helper that builds a clean form-urlencoded body from a form,
			// converting the `isDefault` checkbox to a real boolean and
			// dropping fields the backend doesn't expect (userId, _csrf).
			function buildAddressBody(form) {
			    const fd = new FormData(form);
			    const data = Object.fromEntries(fd.entries());
			    data.isDefault = form.querySelector('input[name="isDefault"]').checked;
			    delete data.userId; // backend uses the authenticated principal
			    delete data._csrf;  // header already carries the token
			    return new URLSearchParams(data);
			}

			// ----- Add Address -----
			document.getElementById('addAddressForm').addEventListener('submit', async function (e) {
			    e.preventDefault();
			    const submitBtn = this.querySelector('button[type="submit"]');
			    const originalLabel = submitBtn ? submitBtn.innerHTML : '';
			    if (submitBtn) { submitBtn.disabled = true; submitBtn.innerHTML = 'Saving...'; }
			    try {
			        const res = await fetch('/account/address/add', {
			            method: 'POST',
			            headers: {
			                'Content-Type': 'application/x-www-form-urlencoded',
			                'X-XSRF-TOKEN': getCsrfToken()
			            },
			            body: buildAddressBody(this)
			        });
			        if (!res.ok) {
			            showNotification(await readError(res), 'error');
			            return;
			        }
			        showNotification('Address added successfully', 'success');
			        const modal = bootstrap.Modal.getInstance(document.getElementById('addAddressModal'));
			        if (modal) modal.hide();
			        localStorage.setItem('activeAccountTab', 'addresses');
			        setTimeout(() => location.reload(), 600);
			    } catch (error) {
			        console.error('Error:', error);
			        showNotification('Network error while adding address', 'error');
			    } finally {
			        if (submitBtn) { submitBtn.disabled = false; submitBtn.innerHTML = originalLabel; }
			    }
			});

			// ----- Edit Address -----
			document.getElementById('editAddressForm').addEventListener('submit', async function (e) {
			    e.preventDefault();
			    const addressId = document.getElementById('editAddressId').value;
			    if (!addressId) {
			        showNotification('Could not determine which address to update', 'error');
			        return;
			    }
			    const submitBtn = this.querySelector('button[type="submit"]');
			    const originalLabel = submitBtn ? submitBtn.innerHTML : '';
			    if (submitBtn) { submitBtn.disabled = true; submitBtn.innerHTML = 'Saving...'; }
			    try {
			        const res = await fetch(`/account/address/update/${addressId}`, {
			            method: 'POST',
			            headers: {
			                'Content-Type': 'application/x-www-form-urlencoded',
			                'X-XSRF-TOKEN': getCsrfToken()
			            },
			            body: buildAddressBody(this)
			        });
			        if (!res.ok) {
			            showNotification(await readError(res), 'error');
			            return;
			        }
			        showNotification('Address updated successfully', 'success');
			        const modal = bootstrap.Modal.getInstance(document.getElementById('editAddressModal'));
			        if (modal) modal.hide();
			        localStorage.setItem('activeAccountTab', 'addresses');
			        setTimeout(() => location.reload(), 600);
			    } catch (error) {
			        console.error('Error:', error);
			        showNotification('Network error while updating address', 'error');
			    } finally {
			        if (submitBtn) { submitBtn.disabled = false; submitBtn.innerHTML = originalLabel; }
			    }
			});
			
		
            
            // Profile form submission
            document.getElementById('profileForm').addEventListener('submit', function(e) {
                e.preventDefault();
                const saveBtn = this.querySelector('.btn-primary');
                const originalText = saveBtn.innerHTML;
                
                saveBtn.innerHTML = '<span class="spinner"></span> Saving...';
                saveBtn.disabled = true;
                
                // Simulate API call
                setTimeout(() => {
                    saveBtn.innerHTML = originalText;
                    saveBtn.disabled = false;
                    
                    document.getElementById('messageText').textContent = 'Profile updated successfully!';
                    document.getElementById('successMessage').style.display = 'block';
                    
                    setTimeout(() => {
                        document.getElementById('successMessage').style.display = 'none';
                    }, 3000);
                    
                    // Update displayed name in sidebar
                    const firstName = document.getElementById('profileFirstName').value;
                    const lastName = document.getElementById('profileLastName').value;
                    document.querySelector('.user-name').textContent = `${firstName} ${lastName}`;
                    
                    // Update welcome message in header
                    document.querySelector('.header-top span').textContent = `Welcome back, ${firstName} ${lastName}!`;
                    
                }, 1500);
            });
            
            // Cancel profile changes
            document.getElementById('cancelProfileChanges').addEventListener('click', function() {
                document.getElementById('profileForm').reset();
            });
            
            // Security form submission
            document.getElementById('securityForm').addEventListener('submit', function(e) {
                e.preventDefault();
                
                const currentPass = document.getElementById('currentPassword').value;
                const newPass = document.getElementById('newPassword').value;
                const confirmPass = document.getElementById('confirmNewPassword').value;
                
                if (!currentPass || !newPass || !confirmPass) {
                    showNotification('Please fill in all password fields', 'error');
                    return;
                }
                
                if (newPass !== confirmPass) {
                    showNotification('New passwords do not match', 'error');
                    return;
                }
                
                if (newPass.length < 8) {
                    showNotification('Password must be at least 8 characters', 'error');
                    return;
                }
                
                // Simulate API call
                const saveBtn = this.querySelector('.btn-primary');
                const originalText = saveBtn.innerHTML;
                
                saveBtn.innerHTML = '<span class="spinner"></span> Updating...';
                saveBtn.disabled = true;
                
                setTimeout(() => {
                    saveBtn.innerHTML = originalText;
                    saveBtn.disabled = false;
                    this.reset();
                    showNotification('Password updated successfully', 'success');
                }, 1500);
            });
            
            // Notifications form submission
            document.getElementById('notificationsForm').addEventListener('submit', function(e) {
                e.preventDefault();
                showNotification('Notification preferences saved', 'success');
            });
            
            
            
            // Search functionality
            document.querySelector('.search-icon').addEventListener('click', showSearchModal);
        }
        
        function checkEmptyWishlist() {
            const emptyState = document.getElementById('emptyWishlist');
            const wishlistContent = document.getElementById('wishlist');
            // Wishlist moved to its own /wishlist page — nothing to do on the account page.
            if (!emptyState || !wishlistContent) return;
            const wishlistItems = document.querySelectorAll('.wishlist-card').length;

            if (wishlistItems === 0) {
                emptyState.style.display = 'block';
                wishlistContent.querySelector('.wishlist-grid').style.display = 'none';
            } else {
                emptyState.style.display = 'none';
                wishlistContent.querySelector('.wishlist-grid').style.display = 'grid';
            }
        }
        
        function updateWishlistCount() {
            const count = document.querySelectorAll('.wishlist-card').length;
            
            // Update sidebar badge
            document.querySelector('.menu-link[data-content="wishlist"] .menu-badge').textContent = count;
            
            // Update header count
            document.querySelector('.wishlist-icon .cart-count').textContent = count;
            
            // Update dashboard card
            document.querySelector('.overview-card:nth-child(2) .card-value').textContent = count;
            
            // Update account stats
            document.querySelector('.account-stats .stat-item:nth-child(3) .stat-value').textContent = count;
        }
		
		// Wishlist is variant-level now, so these key on the VARIANT id.
		async function removeFromWishlist(variantId) {
		    try {
		        const res = await fetch('/wishlist/toggle/' + variantId, {
		            method: 'POST',
		            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
		        });
		        if (res.status === 401) { window.location.href = '/user_redirect'; return; }
		        let data = null;
		        try { data = await res.json(); } catch (_) {}
		        if (!res.ok) {
		            showNotification((data && data.message) || ('HTTP ' + res.status), 'error');
		            return;
		        }
		        // Only remove the card from the UI when the server confirms removal (added === false).
		        if (data && !data.added) {
		            const wishlistCard = document.querySelector(`[data-variant-id="${variantId}"]`);
		            if (wishlistCard) {
		                wishlistCard.style.opacity = '0.5';
		                wishlistCard.style.transform = 'scale(0.95)';
		                setTimeout(() => {
		                    wishlistCard.remove();
		                    checkEmptyWishlist();
		                    updateWishlistCount();
		                }, 300);
		            }
		            showNotification('Item removed from wishlist!', 'info');
		        } else {
		            showNotification('Item is in your wishlist', 'info');
		        }
		    } catch (error) {
		        console.error('Error:', error);
		        showNotification('An error occurred', 'error');
		    }
		}

		// Removes a wishlist card from the page and refreshes the counts/empty state.
		function dropWishlistCard(variantId) {
		    const card = document.querySelector(`[data-variant-id="${variantId}"]`);
		    if (!card) return;
		    card.style.opacity = '0.5';
		    card.style.transform = 'scale(0.95)';
		    setTimeout(() => {
		        card.remove();
		        checkEmptyWishlist();
		        updateWishlistCount();
		    }, 300);
		}

		// Add the saved variant to the cart (form-encoded; matches AddCartItemRequest),
		// then move it OUT of the wishlist — "Add to Cart" here means "move to cart".
		async function addToCartFromWishlist(variantId) {
		    try {
		        const res = await fetch('/api/cart/items', {
		            method: 'POST',
		            headers: { 'X-XSRF-TOKEN': getCsrfToken(), 'Content-Type': 'application/x-www-form-urlencoded' },
		            body: new URLSearchParams({ variantId: variantId, quantity: 1 })
		        });
		        if (res.status === 401) { window.location.href = '/user_redirect'; return; }
		        let data = null;
		        try { data = await res.json(); } catch (_) {}
		        if (!res.ok) {
		            showNotification((data && data.message) || ('HTTP ' + res.status), 'error');
		            return;
		        }
		        const cartBadge = document.querySelector('.cart-icon .cart-count');
		        if (cartBadge && data && typeof data.totalQuantity === 'number') cartBadge.textContent = data.totalQuantity;

		        // Now remove it from the wishlist (it's saved, so toggle removes it).
		        try {
		            await fetch('/wishlist/toggle/' + variantId, {
		                method: 'POST',
		                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
		            });
		        } catch (_) { /* cart add already succeeded; ignore */ }
		        dropWishlistCard(variantId);
		        showNotification('Moved to cart!', 'success');
		    } catch (error) {
		        showNotification('An error occurred', 'error');
		    }
		}

		// Open the product page with the saved variant pre-selected.
		function viewProduct(productId, variantId) {
		    let url = '/product-detail/' + productId;
		    if (variantId) url += '?variant=' + variantId;
		    window.location.href = url;
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
            // Make action buttons larger
            document.querySelectorAll('.action-btn').forEach(btn => {
                btn.style.padding = '12px 20px';
                btn.style.minHeight = '44px';
            });
            
            // Make form inputs larger
            document.querySelectorAll('.form-control').forEach(input => {
                input.style.minHeight = '48px';
                input.style.fontSize = '16px';
            });
        }