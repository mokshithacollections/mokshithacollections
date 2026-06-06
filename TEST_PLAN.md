# Manual test plan — Mokshitha Collections

Run through this end-to-end after starting the app (`mvnw spring-boot:run`). Every checklist item below should pass on a fresh DB after step 0.

> **Most likely reason something fails today**: a product has **no variants** (or all variants have `stock_quantity = 0`). The cart will refuse to add it with a clear toast message. Fix it by going into the admin panel → that product → **Add variant**.

---

## 0. Prep — clean dev data

```sql
-- in psql (DB: mokshitha_collections)
DELETE FROM cart_items;
DELETE FROM user_carts;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM product_reviews;
DELETE FROM user_wishlists;
DELETE FROM product_variant_images;
DELETE FROM product_variants;
DELETE FROM products;
DELETE FROM product_categories;
DELETE FROM addresses;
DELETE FROM persistent_logins;
DELETE FROM users;
```

Restart the app — the `AdminSeeder` recreates the default admin (`admin@mokshitha.local` / `ChangeMe!2026`). Watch the boot log for the banner.

---

## 1. Admin can log in and see the dashboard

1. Go to `http://localhost:8081/login_register`
2. Sign in with the admin credentials
3. **Expected:** notification "Welcome, Admin!" → auto-redirect to `/admin` after 2 s
4. Dashboard shows 0 products / 0 orders / 1 user / 0 pending reviews

## 2. Admin can create a category

1. From the dashboard → sidebar → **Categories** → **New category**
2. Fill in: Name = `Sarees`, Slug = `sarees`, Active = ✓, Save
3. **Expected:** toast "Created" → redirect to `/admin/categories` listing the new row
4. Repeat with Name = `Dresses`, Slug = `dresses`

## 3. Admin can create a product (the part that fixes the Add-to-Cart bug)

1. **Products** → **New product**
2. Name = `Test Silk Saree`, SKU = `MC-TEST-001`, Price = `1999`, Category = `Sarees`, Image URL = any URL (e.g. `https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800`), check Featured + Active, **Create product**
3. **Expected:** redirected to the edit page (now with a Variants section)
4. In the Variants section, add a variant: Color = `Royal Blue`, Size = `Free Size`, **Stock = 5**, **Add variant**
5. **Expected:** toast "Variant added", page reloads with one variant row
6. *(Optional)* click **Upload image** on the variant row and pick an image file. Expected: toast "Image uploaded".

> 🟢 **Without step 4, every cart toggle will say "Product has no purchasable variants"** — that's the bug from the question.

## 4. Storefront sees the product

1. Open a new private/incognito browser window — visit `/home`
2. **Expected:** "Test Silk Saree" appears under Featured Products
3. Go to `/shop`
4. **Expected:** the product appears in the grid. Sidebar lists `Sarees (active)` and `Dresses`. Search "silk" filters to the saree.

## 5. Anonymous user gets bounced to login when adding to cart

1. Still incognito, click **Add to Cart** on the Featured product
2. **Expected:** the JS gets 401 → redirected to `/user_redirect` → since you're not logged in, that renders `login_register.html`

## 6. Customer registration

1. From `login_register`, click **Sign up** tab
2. Fill: First name `Alice`, Last name `Test`, Email `alice@example.com`, Phone `9999999999`, Password `Password1`, Confirm `Password1`
3. **Expected:** toast "Account created!", switches to Login tab with email pre-filled

## 7. Customer login + Add-to-Cart

1. Sign in as `alice@example.com` / `Password1`
2. **Expected:** redirect to `/home` (not `/admin` — Alice isn't admin)
3. The header should show **Welcome back, Alice!** on the top bar; cart count starts at 0
4. Click **Add to Cart** on the saree
5. **Expected:** toast `Test Silk Saree added to cart!`, cart badge becomes `1`
6. Click it again
7. **Expected:** toast `Test Silk Saree removed from cart!`, badge back to `0`. *(This is the toggle behaviour — clicking the same Add-to-Cart button on a card removes it. To add a different quantity, use the product detail page.)*
8. Click once more so it's in the cart again

## 8. Cart page works

1. Click the cart icon (top right) → goes to `/cart`
2. **Expected:** shows the saree, qty 1, unit price ₹1999, total ₹1999
3. Click **Remove** on the line → cart empties
4. Click **Continue Shopping** is gone (because the cart is now empty); empty state visible

## 9. Product detail page + variants

1. From `/shop` click the product name
2. **Expected:** product detail page with image, name, ₹price, the Royal Blue swatch, quantity stepper, Add-to-Cart / Buy Now / Wishlist buttons
3. Click the Royal Blue swatch (already selected by default), set quantity to 2, click **Add to Cart**
4. **Expected:** toast `Added 2 to cart!`, cart badge becomes `2`
5. Click the heart icon
6. **Expected:** toast `Added to wishlist!`, wishlist badge becomes `1`

## 10. Wishlist persists on the account page

1. Click the user icon → `/user_redirect` → account page
2. Switch to **Wishlist** tab
3. **Expected:** the saree is there, with a remove (×) button
4. Click × → toast "removed", the card fades out

## 11. Save an address

1. Account → **Address Book** → **Add New Address**
2. Fill: First name `Alice`, Last name `Test`, Phone `9999999999`, Street `1 Test Lane`, City `Chennai`, State `Tamil Nadu`, Pin `600001`, Country `India`, Type `HOME`, Default ✓ → **Save**
3. **Expected:** toast "Address added", page reloads showing the address card

## 12. Place an order (COD)

1. Add an item to cart, go to `/checkout`
2. **Expected:** your saved address is pre-selected (because it's the default). Cart summary shows the line items.
3. Tick the "I agree to terms" checkbox → click **Place Order**
4. **Expected:** toast "Placed order…" → redirect to `/user_redirect` (account page) → **My Orders** tab shows the new order (`#MC-000001`, status `PLACED`)
5. Go back to `/cart` — it's empty (the order placement clears the cart)

## 13. Admin sees and progresses the order

1. Switch to the admin browser (or sign out and back in as admin) → `/admin/orders`
2. **Expected:** the new order shows up at the top with status `PLACED`
3. Click → order detail page
4. Change **Update order status** to `SHIPPED`, click Update
5. **Expected:** toast "Order updated", page reloads, status pill shows `SHIPPED`
6. Optionally set payment status to `PAID` on a follow-up update

## 14. Variant stock decrements on order placement

1. Go back to `/admin/products/{id}` for the test saree
2. The variant's stock should have decreased by however many you ordered (e.g. 5 → 3 if you placed two orders of 1 unit each)

## 15. Reviews

1. As Alice, go to the product detail page → **Reviews** tab → **Write a Review**
2. Pick a 5-star rating, type a comment, submit
3. **Expected:** toast "Thanks! Your review is pending approval." — modal closes
4. As admin: `/admin/reviews` → the new review appears with **Approve** / **Reject** buttons
5. Click **Approve**. As Alice (refresh the product page), the review is now visible.

## 16. Login throttling

1. Log out
2. Try logging in 5 times with a wrong password
3. **Expected:** the 6th attempt (right or wrong) returns `429 Too Many Requests` for 15 minutes
4. *(Optional)* restart the app — the in-memory counter clears

## 17. Logout

1. From account page → click Logout
2. **Expected:** redirected to `/home`. Cart badge becomes 0 (no session), wishlist count 0.

---

## Common failure → cause → fix

| Symptom | Most likely cause | Fix |
|---|---|---|
| Add to Cart shows error "Product has no purchasable variants" | Product has no variants in admin | Go to `/admin/products/{id}` → **Add variant** with stock ≥ 1 |
| Add to Cart shows "Only N left in stock" / "All variants are out of stock" | Variant stock = 0 | Same — update stock in admin |
| Login says "Invalid email or password" with the seeded admin credentials | An old admin row from before the BCrypt rewrite is in the DB | `DELETE FROM users WHERE is_admin=TRUE;` then restart |
| Featured Products section is empty on home | No products are marked `isFeatured=true` | Edit the product in admin, tick **Featured**, save |
| `/shop` shows "No products match your filters." | No active products in that category | Make sure the product has `isActive=true` and a category assigned |
| Checkout says "You don't have any saved addresses" | No address yet for the logged-in user | Account → Address Book → Add address |
| `/admin` returns 403 | Logged-in user isn't admin | Log in as `admin@mokshitha.local`, or promote your user via `/admin/users` (needs an existing admin to do this) |

## What this round of fixes changed

1. **`home.js`** — cart/wishlist toggles now properly check `res.ok`, show the backend's real error message (e.g. "Product has no purchasable variants") in a red toast instead of silently saying "removed from cart". Cart badge uses the authoritative `totalQuantity` returned by the server. Cart icon points at `/cart` (was the literal `cart.html`).
2. **`shop.html`** — product cards' Add-to-Cart and Wishlist buttons are now wired (they did nothing before). Same error-aware pattern.
3. **`product-detail.html`** — Add to Cart now POSTs `variantId + quantity` to `/api/cart/items`; Buy Now adds and jumps to checkout; the heart button writes to the wishlist; "Write a review" actually submits to the review API. Variant colour swatches set the active variant ID so Add to Cart knows what to buy.
4. **`account.js`** — wishlist remove now respects the server response (no card removal if the server didn't actually remove it) and surfaces errors.
5. **Admin nav link** — every customer-facing page header has a green **⚙ Admin** link that only renders when `user.isAdmin` is true.
