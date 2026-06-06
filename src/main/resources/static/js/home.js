// Read CSRF token written into the XSRF-TOKEN cookie by Spring Security.
function getCsrfToken() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
}

/**
 * POST helper that surfaces real backend errors instead of silently falling
 * through to a misleading toast. Returns the parsed body on success, throws
 * on any non-2xx (with the server's `message` field if present).
 */
async function postJson(url) {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': getCsrfToken() }
    });
    if (res.status === 401) {
        window.location.href = '/user_redirect';
        throw new Error('not authenticated');
    }
    let body = null;
    try { body = await res.json(); } catch (_) { /* may have no body */ }
    if (!res.ok) {
        const msg = (body && body.message) ? body.message : ('HTTP ' + res.status);
        const err = new Error(msg);
        err.status = res.status;
        throw err;
    }
    return body;
}

// Fade-in animation on scroll
const fadeElements = document.querySelectorAll('.fade-in');
const appearOnScroll = new IntersectionObserver(function (entries) {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            appearOnScroll.unobserve(entry.target);
        }
    });
}, { threshold: 0.1 });
fadeElements.forEach(element => appearOnScroll.observe(element));

// ---------- Wishlist: pick a variant before saving ----------
// (window.WISHLIST_VARIANT_IDS is seeded by an inline script in home.html.)
if (!window.WISHLIST_VARIANT_IDS) window.WISHLIST_VARIANT_IDS = new Set();

async function toggleWishlistVariant(variantId) {
    const data = await postJson('/wishlist/toggle/' + variantId);
    const wishlistBadge = document.querySelector('.wishlist-icon .cart-count');
    if (data && data.added) window.WISHLIST_VARIANT_IDS.add(variantId);
    else window.WISHLIST_VARIANT_IDS.delete(variantId);
    if (wishlistBadge) {
        const n = parseInt(wishlistBadge.textContent || '0', 10) + (data && data.added ? 1 : -1);
        wishlistBadge.textContent = Math.max(0, n);
    }
    return data && data.added;
}

function openWishlistModal(product, heartEl) {
    injectVariantModalStyles();
    const variants = product.variants || [];
    const overlay = document.createElement('div');
    overlay.className = 'vmodal-overlay';
    const hero = product.imageUrl || '';

    overlay.innerHTML =
        '<div class="vmodal" role="dialog" aria-modal="true">' +
          '<div class="vmodal-head">' +
            (hero ? '<img src="' + hero + '" alt="">' : '') +
            '<div class="t"><h5></h5><span class="p">Save your favourite colours</span></div>' +
            '<button class="x" aria-label="Close">&times;</button>' +
          '</div>' +
          '<div class="vmodal-body"><div class="lbl">Tap a variant to add/remove</div><div class="vchip-list"></div></div>' +
          '<div class="vmodal-foot"><button class="add">Done</button></div>' +
        '</div>';
    overlay.querySelector('.t h5').textContent = product.name || 'Product';

    const list = overlay.querySelector('.vchip-list');

    variants.forEach(v => {
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'vchip' + (window.WISHLIST_VARIANT_IDS.has(v.variantId) ? ' active' : '');

        const swatchColor = (v.color || '').toLowerCase().replace(/\s+/g, '');
        const sw = document.createElement('span');
        sw.className = 'sw';
        if (swatchColor) sw.style.backgroundColor = swatchColor;

        const ci = document.createElement('span');
        ci.className = 'ci';
        const c = document.createElement('span'); c.className = 'c'; c.textContent = v.color || 'Variant';
        const m = document.createElement('span'); m.className = 'm';
        m.textContent = (v.size ? ('Size ' + v.size + ' · ') : '') +
                        (window.WISHLIST_VARIANT_IDS.has(v.variantId) ? 'In wishlist ♥' : 'Tap to add');
        ci.appendChild(c); ci.appendChild(m);
        chip.appendChild(sw); chip.appendChild(ci);

        chip.addEventListener('click', async () => {
            chip.disabled = true;
            try {
                const added = await toggleWishlistVariant(v.variantId);
                chip.classList.toggle('active', added);
                m.textContent = (v.size ? ('Size ' + v.size + ' · ') : '') +
                                (added ? 'In wishlist ♥' : 'Tap to add');
                const anySaved = variants.some(x => window.WISHLIST_VARIANT_IDS.has(x.variantId));
                const icon = heartEl ? heartEl.querySelector('i') : null;
                if (heartEl) heartEl.classList.toggle('active', anySaved);
                if (icon) { icon.classList.toggle('fas', anySaved); icon.classList.toggle('far', !anySaved); }
                showNotification(added ? `${product.name} (${v.color || ''}) added to wishlist!` : 'Removed from wishlist');
            } catch (e) { showNotification(e.message, 'error'); }
            finally { chip.disabled = false; }
        });
        list.appendChild(chip);
    });

    function close() { overlay.remove(); }
    overlay.querySelector('.x').addEventListener('click', close);
    overlay.querySelector('.add').addEventListener('click', close);
    overlay.addEventListener('click', e => { if (e.target === overlay) close(); });
    document.body.appendChild(overlay);
}

document.querySelectorAll('.product-wishlist').forEach(btn => {
    btn.addEventListener('click', async function () {
        const productId = this.dataset.productId;
        if (!productId) return;
        try {
            const product = await fetchProductVariants(productId);
            const variants = product.variants || [];
            if (variants.length === 0) {
                showNotification('This product has no variants to wishlist.', 'error');
            } else if (variants.length === 1) {
                const added = await toggleWishlistVariant(variants[0].variantId);
                const icon = this.querySelector('i');
                this.classList.toggle('active', added);
                if (icon) { icon.classList.toggle('fas', added); icon.classList.toggle('far', !added); }
                showNotification(added ? `${product.name} added to wishlist!` : 'Removed from wishlist');
            } else {
                openWishlistModal(product, this);
            }
        } catch (err) {
            showNotification(err.message, 'error');
        }
    });
});

// ---------- Header icon shortcuts ----------
const userIcon = document.getElementById('userIcon');
if (userIcon) {
    userIcon.addEventListener('click', () => { window.location.href = '/user_redirect'; });
}
const cartIcon = document.getElementById('cartIcon');
if (cartIcon) {
    // Was pointing at the literal "cart.html" — fixed to the real Spring route.
    cartIcon.addEventListener('click', () => { window.location.href = '/cart'; });
}

// ---------- Add-to-cart on product cards ----------
// Briefly enlarges the cart badge so a +1 increment is impossible to miss.
function pulseBadge(badge) {
    if (!badge) return;
    badge.style.transition = 'transform 0.3s ease, background-color 0.3s ease';
    const original = badge.style.background;
    badge.style.transform = 'scale(1.6)';
    badge.style.background = '#16a34a';
    badge.style.color = '#fff';
    setTimeout(() => {
        badge.style.transform = 'scale(1)';
        badge.style.background = original;
    }, 350);
}

// ---------- Variant quick-select popup (shared behaviour with the shop page) ----------
// Featured cards only know the productId, so on "Add to Cart" we fetch the
// product's variants and either add the single option directly or let the
// customer pick a colour/size first.

// Inject the modal's styles once (keeps this self-contained — no home.html edit).
function injectVariantModalStyles() {
    if (document.getElementById('vmodal-styles')) return;
    const css = `
    .vmodal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.55);
        display: flex; align-items: center; justify-content: center; z-index: 11000; padding: 20px; }
    .vmodal { background: #fff; border-radius: 14px; width: 100%; max-width: 460px;
        max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
    .vmodal-head { display: flex; gap: 14px; align-items: center; padding: 18px 20px; border-bottom: 1px solid #eee; }
    .vmodal-head img { width: 56px; height: 56px; object-fit: cover; border-radius: 8px; }
    .vmodal-head .t { flex-grow: 1; }
    .vmodal-head .t h5 { margin: 0; font-family: 'Playfair Display', serif; font-size: 1.1rem; }
    .vmodal-head .t .p { color: var(--dark-sea-green, #2E8B57); font-weight: 700; }
    .vmodal-head .x { background: none; border: none; font-size: 1.4rem; color: #999; cursor: pointer; line-height: 1; }
    .vmodal-body { padding: 18px 20px; }
    .vmodal-body .lbl { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.5px; color: #888; margin-bottom: 10px; }
    .vchip-list { display: flex; flex-wrap: wrap; gap: 10px; }
    .vchip { display: inline-flex; align-items: center; gap: 9px; padding: 7px 13px 7px 7px;
        border: 2px solid #e5e7eb; border-radius: 999px; background: #fff; cursor: pointer; font-family: inherit; transition: all .15s; }
    .vchip:hover:not(.disabled) { border-color: var(--dark-sea-green, #2E8B57); background: #f0fdf4; }
    .vchip.active { border-color: var(--dark-sea-green, #2E8B57); background: linear-gradient(135deg,#f0fdf4,#ecfdf5); box-shadow: 0 4px 12px rgba(46,139,87,0.18); }
    .vchip.disabled { opacity: .5; cursor: not-allowed; text-decoration: line-through; }
    .vchip .sw { width: 28px; height: 28px; border-radius: 50%; background: #cbd5e1; border: 1px solid rgba(0,0,0,0.12); box-shadow: inset 0 2px 4px rgba(0,0,0,0.12); flex-shrink: 0; }
    .vchip .ci { display: flex; flex-direction: column; line-height: 1.2; text-align: left; }
    .vchip .ci .c { font-weight: 600; font-size: 0.9rem; color: #1f2937; }
    .vchip .ci .m { font-size: 0.72rem; color: #6b7280; }
    .vmodal-foot { padding: 0 20px 20px; }
    .vmodal-foot .add { width: 100%; padding: 13px; border: none; border-radius: 8px; font-weight: 600; cursor: pointer;
        color: #fff; background: linear-gradient(135deg, var(--dark-sea-green, #2E8B57), var(--light-sea-green, #20B2AA)); }
    .vmodal-foot .add:disabled { opacity: .6; cursor: not-allowed; }`;
    const style = document.createElement('style');
    style.id = 'vmodal-styles';
    style.textContent = css;
    document.head.appendChild(style);
}

async function fetchProductVariants(productId) {
    const res = await fetch('/api/products/' + productId, { headers: { 'Accept': 'application/json' } });
    if (!res.ok) throw new Error('Could not load product options');
    return (await res.json()) || {};
}

async function addVariantToCart(variantId, qty) {
    const res = await fetch('/api/cart/items', {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': getCsrfToken(), 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ variantId: variantId, quantity: qty || 1 })
    });
    if (res.status === 401) { window.location.href = '/user_redirect'; throw new Error('not authenticated'); }
    let body = null;
    try { body = await res.json(); } catch (_) {}
    if (!res.ok) throw new Error((body && body.message) || ('HTTP ' + res.status));
    return body;
}

function markAdded(btn, cart, name) {
    const cartBadge = document.querySelector('.cart-icon .cart-count');
    if (cartBadge && cart && typeof cart.totalQuantity === 'number') {
        cartBadge.textContent = cart.totalQuantity;
        pulseBadge(cartBadge);
    }
    if (btn) { btn.textContent = 'View Cart'; btn.classList.add('in-cart'); }
    showNotification(`${name} added to cart!`);
}

function openVariantModal(product, originBtn) {
    injectVariantModalStyles();
    const variants = product.variants || [];
    const overlay = document.createElement('div');
    overlay.className = 'vmodal-overlay';

    const hero = product.imageUrl || '';
    const priceVal = (product.discountPrice != null ? product.discountPrice : product.price);

    overlay.innerHTML =
        '<div class="vmodal" role="dialog" aria-modal="true">' +
          '<div class="vmodal-head">' +
            (hero ? '<img src="' + hero + '" alt="">' : '') +
            '<div class="t"><h5></h5><span class="p"></span></div>' +
            '<button class="x" aria-label="Close">&times;</button>' +
          '</div>' +
          '<div class="vmodal-body"><div class="lbl">Choose a variant</div><div class="vchip-list"></div></div>' +
          '<div class="vmodal-foot"><button class="add" disabled>Select a variant</button></div>' +
        '</div>';

    overlay.querySelector('.t h5').textContent = product.name || 'Product';
    overlay.querySelector('.t .p').textContent = priceVal != null ? ('₹' + priceVal) : '';

    const list = overlay.querySelector('.vchip-list');
    const addBtn = overlay.querySelector('.add');
    let selectedVariantId = null;

    variants.forEach(v => {
        const stock = v.stockQuantity == null ? 0 : v.stockQuantity;
        const out = stock <= 0;
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'vchip' + (out ? ' disabled' : '');
        if (out) chip.disabled = true;

        const swatchColor = (v.color || '').toLowerCase().replace(/\s+/g, '');
        const meta = (v.size ? ('Size ' + v.size + ' · ') : '') + (out ? 'Out of stock' : (stock + ' in stock'));

        const sw = document.createElement('span');
        sw.className = 'sw';
        if (swatchColor) sw.style.backgroundColor = swatchColor;

        const ci = document.createElement('span');
        ci.className = 'ci';
        const c = document.createElement('span'); c.className = 'c'; c.textContent = v.color || 'Variant';
        const m = document.createElement('span'); m.className = 'm'; m.textContent = meta;
        ci.appendChild(c); ci.appendChild(m);
        chip.appendChild(sw); chip.appendChild(ci);

        if (!out) {
            chip.addEventListener('click', () => {
                list.querySelectorAll('.vchip').forEach(x => x.classList.remove('active'));
                chip.classList.add('active');
                selectedVariantId = v.variantId;
                addBtn.disabled = false;
                addBtn.textContent = 'Add to Cart';
            });
        }
        list.appendChild(chip);
    });

    function close() { overlay.remove(); }
    overlay.querySelector('.x').addEventListener('click', close);
    overlay.addEventListener('click', e => { if (e.target === overlay) close(); });

    addBtn.addEventListener('click', async () => {
        if (!selectedVariantId) return;
        addBtn.disabled = true;
        addBtn.textContent = 'Adding...';
        try {
            const cart = await addVariantToCart(selectedVariantId, 1);
            markAdded(originBtn, cart, product.name || 'Item');
            close();
        } catch (e) {
            showNotification(e.message, 'error');
            addBtn.disabled = false;
            addBtn.textContent = 'Add to Cart';
        }
    });

    document.body.appendChild(overlay);
}

document.querySelectorAll('.product-btn').forEach(btn => {
    btn.addEventListener('click', async function () {
        // If the product is already in the cart, this button means "View Cart".
        if (this.classList.contains('in-cart')) {
            window.location.href = '/cart';
            return;
        }

        const productCard = this.closest('.product-card');
        const wishBtn     = productCard ? productCard.querySelector('.product-wishlist') : null;
        const productId   = wishBtn ? wishBtn.dataset.productId : this.dataset.productId;
        const productName = productCard ? productCard.querySelector('.product-title').textContent.trim() : 'Item';
        if (!productId) return;

        const original = this.textContent;
        this.disabled = true;
        this.textContent = 'Loading...';
        try {
            const product = await fetchProductVariants(productId);
            const inStock = (product.variants || []).filter(
                v => v.stockQuantity != null && v.stockQuantity > 0);

            if (inStock.length === 0) {
                showNotification('Sorry, this product is out of stock.', 'error');
            } else if (inStock.length === 1) {
                // Only one buyable option — add it directly, no popup needed.
                const cart = await addVariantToCart(inStock[0].variantId, 1);
                markAdded(this, cart, productName);
            } else {
                // Multiple variants — let the customer choose.
                openVariantModal(product, this);
            }
        } catch (err) {
            showNotification(err.message, 'error');
        } finally {
            this.disabled = false;
            if (!this.classList.contains('in-cart')) this.textContent = original;
        }
    });
});

// ---------- Notification toast ----------
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    const bg = type === 'error'
        ? '#dc2626'
        : (type === 'info' ? '#0284c7' : 'linear-gradient(135deg, var(--dark-sea-green), var(--light-sea-green))');
    notification.style.cssText = `
        position: fixed;
        top: 100px;
        right: 20px;
        background: ${bg};
        color: white;
        padding: 15px 25px;
        border-radius: 8px;
        box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        z-index: 10000;
        transform: translateX(150%);
        transition: transform 0.3s ease;
        max-width: 320px;
    `;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => { notification.style.transform = 'translateX(0)'; }, 10);
    setTimeout(() => {
        notification.style.transform = 'translateX(150%)';
        setTimeout(() => { if (notification.parentNode) document.body.removeChild(notification); }, 300);
    }, 3000);
}

// ---------- Search modal ----------
const searchIcon = document.querySelector('.search-icon');
if (searchIcon) {
    searchIcon.addEventListener('click', function () {
        const searchBox = document.createElement('div');
        searchBox.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0,0,0,0.8); z-index: 10001;
            display: flex; align-items: center; justify-content: center;
        `;
        searchBox.innerHTML = `
            <div style="background: white; padding: 30px; border-radius: 10px; width: 90%; max-width: 600px; position: relative;">
                <button class="close-search" style="position: absolute; top: 15px; right: 15px; background: none; border: none; font-size: 1.5rem; cursor: pointer;">×</button>
                <h3 style="margin-bottom: 20px;">Search Products</h3>
                <form method="get" action="/shop">
                    <div style="display: flex;">
                        <input type="text" name="search" placeholder="Search for sarees, dresses, collections..."
                               style="flex: 1; padding: 15px; border: 2px solid #eee; border-radius: 5px 0 0 5px; font-size: 1rem;">
                        <button type="submit" style="background: linear-gradient(135deg, var(--dark-sea-green), var(--light-sea-green)); color: white; border: none; padding: 0 30px; border-radius: 0 5px 5px 0; cursor: pointer;">
                            <i class="fas fa-search"></i>
                        </button>
                    </div>
                </form>
            </div>
        `;
        document.body.appendChild(searchBox);
        searchBox.querySelector('.close-search').addEventListener('click', () => document.body.removeChild(searchBox));
        searchBox.addEventListener('click', (e) => { if (e.target === searchBox) document.body.removeChild(searchBox); });
    });
}
