(function () {
    "use strict";

    const $  = (s, ctx = document) => ctx.querySelector(s);
    const $$ = (s, ctx = document) => Array.from(ctx.querySelectorAll(s));

    const formatCurrency = (num) =>
        new Intl.NumberFormat("vi-VN").format(Math.round(num)) + " đ";

    const clamp = (value, min, max) => {
        let v = parseInt(value);
        if (isNaN(v)) v = min;
        if (v < min) v = min;
        if (v > max) v = max;
        return v;
    };

    const setError = (input) => {
        const row = input.closest(".cart-row");
        const err = row && row.querySelector(".qty-error");
        if (err) {
            err.textContent = "Số lượng trong kho không đủ!";
            err.style.display = "block";
        }
    };
    const clearError = (input) => {
        const row = input.closest(".cart-row");
        const err = row && row.querySelector(".qty-error");
        if (err) err.style.display = "none";
    };

    function setupCartDrawer() {
        const openBtn  = $(".js-open-cart");
        const closeBtn = $(".js-close-cart");
        const sidebar  = $("#miniCart");
        const backdrop = $(".backdrop");

        function open()  { sidebar && sidebar.classList.add("active");  backdrop && backdrop.classList.add("active"); }
        function close() { sidebar && sidebar.classList.remove("active"); backdrop && backdrop.classList.remove("active"); }

        openBtn && openBtn.addEventListener("click", open);
        closeBtn && closeBtn.addEventListener("click", close);
        backdrop && backdrop.addEventListener("click", close);

        document.addEventListener("click", (e) => {
            const a = e.target.closest('a[aria-disabled="true"]');
            if (a) { e.preventDefault(); e.stopPropagation(); }
        });
    }

    function updateRowTotal(row) {
        const input = row.querySelector(".qty-input");
        const price = parseFloat(row.dataset.unitprice || 0);
        const max   = parseInt(input.getAttribute("max")) || 999999;
        const min   = parseInt(input.getAttribute("min")) || 1;
        const qty   = clamp(input.value, min, max);
        const el    = row.querySelector(".line-total");
        if (el) el.textContent = formatCurrency(price * qty);
    }

    function updateCartTotal() {
        let total = 0, totalQty = 0;
        $$(".cart-row").forEach((row) => {
            const input = row.querySelector(".qty-input");
            const price = parseFloat(row.dataset.unitprice || 0);
            const max   = parseInt(input.getAttribute("max")) || 999999;
            const min   = parseInt(input.getAttribute("min")) || 1;
            const qty   = clamp(input.value, min, max);
            total += qty * price;
            totalQty += qty;
        });
        const elTotal = $("#miniCartTotal");
        const elCount = $("#miniCartCount");
        if (elTotal) elTotal.textContent = formatCurrency(total);
        if (elCount) elCount.textContent = totalQty;
    }

    function bindMiniCartRow(row) {
        const input  = row.querySelector(".qty-input");
        const decBtn = row.querySelector(".dec-btn");
        const incBtn = row.querySelector(".inc-btn");
        const max    = parseInt(input.getAttribute("max")) || 999999;
        const min    = parseInt(input.getAttribute("min")) || 1;

        // bảo đảm ẩn lỗi lúc khởi tạo
        clearError(input);

        // nút -
        decBtn && decBtn.addEventListener("click", () => {
            let v = Math.max(min, (parseInt(input.value) || min) - 1);
            input.value = v;
            clearError(input);             // không báo khi giảm
            updateRowTotal(row);
            updateCartTotal();
        });

        // nút +
        incBtn && incBtn.addEventListener("click", () => {
            const current = parseInt(input.value) || min;
            const next = current + 1;
            if (next > max) {
                // vượt -> clamp về max và chỉ CẢNH BÁO nếu muốn (ở đây KHÔNG báo để tránh “lúc nào cũng đỏ”)
                input.value = max;
                clearError(input);
            } else {
                input.value = next;
                clearError(input);
            }
            updateRowTotal(row);
            updateCartTotal();
        });

        // nhập trực tiếp: chỉ báo khi > max
        input && input.addEventListener("input", () => {
            input.value = input.value.replace(/[^\d]/g, "");
            const v = parseInt(input.value || "0");
            if (v > max) setError(input); else clearError(input);
            updateRowTotal(row);
            updateCartTotal();
        });

        // blur: clamp lại, chỉ báo nếu raw > max
        input && input.addEventListener("blur", () => {
            const raw = parseInt(input.value || "0");
            if (raw > max) setError(input); else clearError(input);
            input.value = clamp(raw, min, max);
            updateRowTotal(row);
            updateCartTotal();
        });

        // init
        updateRowTotal(row);
    }

    function setupMiniCheckout() {
        const btn  = $("#miniCartCheckoutBtn");
        const form = $("#miniCartForm");
        const inp  = $("#miniCartJson");
        if (!btn || !form || !inp) return;

        btn.addEventListener("click", (e) => {
            e.preventDefault();
            $$(".cart-row").forEach((row) => {
                const input = row.querySelector(".qty-input");
                const max   = parseInt(input.getAttribute("max")) || 999999;
                const min   = parseInt(input.getAttribute("min")) || 1;
                input.value = clamp(input.value, min, max);
                clearError(input);
            });

            const cart = [];
            $$(".cart-row").forEach((row) => {
                cart.push({
                    productId: row.dataset.id,
                    qty: parseInt(row.querySelector(".qty-input").value) || 1,
                });
            });
            inp.value = JSON.stringify(cart);
            form.submit();
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        setupCartDrawer();
        $$(".cart-row").forEach(bindMiniCartRow);
        updateCartTotal();
        setupMiniCheckout();
    });
})();
