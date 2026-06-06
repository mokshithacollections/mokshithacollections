/* Shared admin helpers — used by every page under /admin/**.
 * Exposes: getCsrfToken, adminApi, adminUpload, toast, confirmThen, adminLogout.
 */

function getCsrfToken() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
}

/**
 * JSON fetch wrapper for admin REST endpoints.
 *   adminApi('GET',  '/api/admin/products?page=0')
 *   adminApi('POST', '/api/admin/products', { name, sku, price, ... })
 *   adminApi('DELETE', '/api/admin/products/42')
 * Resolves to the parsed JSON body; rejects (with a thrown Error) on any non-2xx.
 */
async function adminApi(method, url, body) {
    const opts = {
        method,
        headers: {
            'Accept': 'application/json',
            'X-XSRF-TOKEN': getCsrfToken()
        }
    };
    if (body !== undefined && body !== null) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
    }
    const res = await fetch(url, opts);
    let data = null;
    try { data = await res.json(); } catch (_) { /* may have no body */ }
    if (!res.ok) {
        const msg = (data && data.message) ? data.message : ('Request failed (HTTP ' + res.status + ')');
        const err = new Error(msg);
        err.status = res.status;
        err.data = data;
        throw err;
    }
    return data;
}

/**
 * Multipart upload helper for image uploads.
 *   adminUpload('/api/admin/products/1/variants/2/images', { file, viewType, isPrimary })
 */
async function adminUpload(url, fields) {
    const fd = new FormData();
    Object.entries(fields).forEach(([k, v]) => {
        if (v !== undefined && v !== null) fd.append(k, v);
    });
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': getCsrfToken() },
        body: fd
    });
    let data = null;
    try { data = await res.json(); } catch (_) {}
    if (!res.ok) {
        throw new Error((data && data.message) || ('Upload failed (HTTP ' + res.status + ')'));
    }
    return data;
}

/**
 * Pop a toast in the top-right corner.
 *   toast('Saved!', 'success')
 *   toast('Something failed', 'error')
 */
function toast(message, kind = 'info', durationMs = 3500) {
    const stack = document.getElementById('adminToastStack');
    if (!stack) return;
    const el = document.createElement('div');
    el.className = 'toast ' + (kind === 'success' || kind === 'error' ? kind : '');
    el.textContent = message;
    stack.appendChild(el);
    setTimeout(() => {
        el.style.transition = 'opacity .25s';
        el.style.opacity = '0';
        setTimeout(() => el.remove(), 250);
    }, durationMs);
}

/** Tiny wrapper around window.confirm so callers don't have to wire the dialog. */
function confirmThen(message, fn) {
    if (window.confirm(message)) fn();
}

/** Posts /auth/logout and bounces to home. */
async function adminLogout() {
    try {
        await fetch('/auth/logout', {
            method: 'POST',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
    } catch (_) { /* ignore — go home anyway */ }
    window.location.href = '/home';
}
