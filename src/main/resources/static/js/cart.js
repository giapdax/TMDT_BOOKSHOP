(function () {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const cart = {
        open() {
            $('#miniCart')?.classList.add('active');
            $('.backdrop')?.classList.add('active');
        },
        close() {
            $('#miniCart')?.classList.remove('active');
            $('.backdrop')?.classList.remove('active');
        },
        async post(url, data) {
            const body = new URLSearchParams(data || {});
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body
            });
            return res.json();
        },
        async refresh() {
            const res = await fetch('/api/cart/mini', { method: 'GET' }).then(r => r.json());
            updateMiniFromResponse(res);
        }
    };

    // ====== Render helpers ======
    function fmt(s) { return s == null ? '' : s; }

    function renderMiniItems(mini = []) {
        const ul = $('#miniCartList');
        const empty = $('#miniCartEmpty');
        if (!ul) return;

        if (!mini || mini.length === 0) {
            ul.innerHTML = '';
            if (empty) empty.style.display = 'block';
            return;
        }
        if (empty) empty.style.display = 'none';

        ul.innerHTML = mini.map(it => `
      <li class="cart-item" data-id="${it.productId}">
        <div class="cart-media">
          <a href="/productDetail?id=${it.productId}">
            <img src="/loadImage?imageName=${encodeURIComponent(it.image || '')}" alt="product"/>
          </a>
        </div>
        <div class="cart-info-group">
          <div class="cart-info">
            <h6 class="mb-1"><a href="/productDetail?id=${it.productId}" class="prod-name">${fmt(it.name)}</a></h6>
            <div class="price-row">Tạm tính: <strong class="line-total js-line">${fmt(it.lineTotalText)}</strong></div>
          </div>

          <div class="action-row">
            <div class="qty-row">
              <button class="qty-btn js-dec" data-id="${it.productId}">−</button>
              <input class="qty-input js-qty" type="tel" data-id="${it.productId}" value="${it.qty}" />
              <button class="qty-btn js-inc" data-id="${it.productId}">+</button>
            </div>
            <div class="stock-row">Còn: <span class="stock js-stock">${fmt(it.remainingStockText || '')}</span></div>
            <button class="rm-btn js-remove" data-id="${it.productId}" title="Xoá"><i class="icofont-trash"></i></button>
          </div>
        </div>
      </li>
    `).join('');
    }

    function updateMiniFromResponse(res) {
        // count
        if ($('#totalCartItems')) $('#totalCartItems').textContent = res.totalCartItems ?? 0;
        if ($('#miniCartCount')) $('#miniCartCount').textContent = res.totalCartItems ?? 0;
        // total
        if ($('#miniCartTotal')) $('#miniCartTotal').textContent = res.cartTotalText ?? '0 đ';
        // list
        if (Array.isArray(res.mini)) renderMiniItems(res.mini);
    }

    // ====== Add To Cart (intercept) ======
    function bindAddToCart() {
        // 1) buttons <button class="btn-add-to-cart" data-id="..." data-qty="...">
        document.addEventListener('click', async (e) => {
            const btn = e.target.closest('.btn-add-to-cart');
            if (!btn) return;
            e.preventDefault();
            const productId = btn.dataset.id || btn.dataset.productId;
            const qty = btn.dataset.qty || 1;
            if (!productId) return;

            const res = await cart.post('/api/cart/add', { productId, qty });
            if (!res.success) { alert(res.message || 'Không thể thêm giỏ hàng'); return; }
            await cart.refresh();
            cart.open();
        });

        // 2) anchors <a data-add-to-cart href="/addToCart?productId=...">
        document.addEventListener('click', async (e) => {
            const a = e.target.closest('a[data-add-to-cart]');
            if (!a) return;
            e.preventDefault();
            const url = new URL(a.href, location.origin);
            const productId = a.dataset.id || url.searchParams.get('productId');
            const qty = a.dataset.qty || url.searchParams.get('qty') || 1;
            if (!productId) return;

            const res = await cart.post('/api/cart/add', { productId, qty });
            if (!res.success) { alert(res.message || 'Không thể thêm giỏ hàng'); return; }
            await cart.refresh();
            cart.open();
        });
    }

    // ====== Mini cart actions (+ / − / set / remove) ======
    function bindMiniActions() {
        // open/close
        $('.js-open-cart')?.addEventListener('click', () => cart.open());
        $('.js-close-cart')?.addEventListener('click', () => cart.close());
        $('.backdrop')?.addEventListener('click', () => cart.close());

        // increase
        document.addEventListener('click', async (e) => {
            const btn = e.target.closest('.js-inc');
            if (!btn) return;
            const pid = btn.dataset.id;
            const res = await cart.post('/api/cart/increase', { productId: pid });
            if (!res.success && res.message) alert(res.message);
            await cart.refresh();
        });

        // decrease
        document.addEventListener('click', async (e) => {
            const btn = e.target.closest('.js-dec');
            if (!btn) return;
            const pid = btn.dataset.id;
            const res = await cart.post('/api/cart/decrease', { productId: pid });
            if (!res.success && res.message) alert(res.message);
            await cart.refresh();
        });

        // remove
        document.addEventListener('click', async (e) => {
            const btn = e.target.closest('.js-remove');
            if (!btn) return;
            const pid = btn.dataset.id;
            const res = await cart.post('/api/cart/remove', { productId: pid });
            if (!res.success && res.message) alert(res.message);
            await cart.refresh();
        });

        // set by input
        document.addEventListener('change', async (e) => {
            const ip = e.target.closest('.js-qty');
            if (!ip) return;
            const pid = ip.dataset.id;
            let qty = parseInt(ip.value.replace(/[^\d]/g,''), 10);
            if (!Number.isFinite(qty) || qty < 1) qty = 1;
            const res = await cart.post('/api/cart/set', { productId: pid, qty });
            if (!res.success && res.message) alert(res.message);
            await cart.refresh();
        });

        // Enter in input => blur => change triggers
        document.addEventListener('keydown', (e) => {
            const ip = e.target.closest('.js-qty');
            if (!ip) return;
            if (e.key === 'Enter') ip.blur();
        });
    }

    // init
    bindAddToCart();
    bindMiniActions();
})();
