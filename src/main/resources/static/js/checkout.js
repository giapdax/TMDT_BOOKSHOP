(function () {
    "use strict";

    // helpers
    const $one  = (s, ctx = document) => ctx.querySelector(s);
    const $all = (s, ctx = document) => Array.from(ctx.querySelectorAll(s));

    const fmt = (num) => new Intl.NumberFormat("vi-VN").format(Math.round(num)) + " đ";
    const getUnitPrice = (row) => parseFloat(row.dataset.price || 0);

    function clamp(value, min, max) {
        let v = parseInt(value);
        if (isNaN(v)) v = min;
        if (v < min) v = min;
        if (v > max) v = max;
        return v;
    }

    function setError(input) {
        const td = input.closest("td");
        const err = td && td.querySelector(".qty-error");
        if (err) {
            err.textContent = "Số lượng trong kho không đủ!";
            err.style.display = "block";
        }
    }
    function clearError(input) {
        const td = input.closest("td");
        const err = td && td.querySelector(".qty-error");
        if (err) err.style.display = "none";
    }

    function updateRowTotal(row) {
        const input = row.querySelector(".qty-input");
        const max = parseInt(input.getAttribute("max")) || 999999;
        const min = parseInt(input.getAttribute("min")) || 1;
        const qty = clamp(input.value, min, max);
        const price = getUnitPrice(row);
        const el = row.querySelector(".line-total");
        if (el) el.textContent = new Intl.NumberFormat("vi-VN").format(Math.round(price * qty)); // chỉ số
    }

    function updateCartTotal() {
        let total = 0;
        $all(".cart-row").forEach((row) => {
            const input = row.querySelector(".qty-input");
            const max = parseInt(input.getAttribute("max")) || 999999;
            const min = parseInt(input.getAttribute("min")) || 1;
            const qty = clamp(input.value, min, max);
            total += qty * getUnitPrice(row);
        });
        const el = $one("#cart-total");
        if (el) el.textContent = fmt(total);
    }

    function bindRow(row) {
        const input = row.querySelector(".qty-input");
        const max = parseInt(input.getAttribute("max")) || 999999;
        const min = parseInt(input.getAttribute("min")) || 1;

        // gõ: hiển thị lỗi nếu vượt max (không clamp ngay)
        input.addEventListener("input", () => {
            input.value = input.value.replace(/[^\d]/g, "");
            const v = parseInt(input.value || "0");
            if (v > max) setError(input);
            else clearError(input);

            updateRowTotal(row);
            updateCartTotal();
        });

        // blur/Enter: clamp về range + giữ/ẩn lỗi đúng
        input.addEventListener("blur", () => {
            const raw = parseInt(input.value || "0");
            const capped = clamp(raw, min, max);
            if (raw > max) setError(input);
            else clearError(input);

            input.value = capped;
            updateRowTotal(row);
            updateCartTotal();
        });

        // init
        updateRowTotal(row);
    }

    // Xác nhận xoá (Bootstrap cũ)
    function setupRemoveHandlers() {
        document.addEventListener("click", function (e) {
            const link = e.target.closest("a.trash");
            if (!link) return;

            const id = link.getAttribute("data-id");
            const name = link.getAttribute("data-name");

            const nameEl = document.getElementById("productName");
            const yesEl = document.getElementById("yesOption");
            if (nameEl) nameEl.textContent = name || "";
            if (yesEl) yesEl.setAttribute("href", "/remove/" + id);

            // Bootstrap 3/4 jQuery modal
            if (window.$ && typeof $("#configmationId").modal === "function") {
                $("#configmationId").modal("show");
            }
        });
    }

    // Gom cartJson khi submit
    function setupCheckoutSubmit() {
        const form = document.getElementById("checkoutForm");
        if (!form) return;

        form.addEventListener("submit", function () {
            // clamp trước khi gửi
            $all(".cart-row").forEach((row) => {
                const input = row.querySelector(".qty-input");
                const max = parseInt(input.getAttribute("max")) || 999999;
                const min = parseInt(input.getAttribute("min")) || 1;
                input.value = clamp(input.value, min, max);
                clearError(input);
            });

            const cart = [];
            $all(".cart-row").forEach((row) => {
                cart.push({
                    productId: row.dataset.id,
                    qty: parseInt(row.querySelector(".qty-input").value) || 1,
                });
            });
            const cartJsonInput = document.getElementById("cartJson");
            if (cartJsonInput) cartJsonInput.value = JSON.stringify(cart);
        });
    }

    // chặn click khi aria-disabled="true" (an toàn)
    function guardDisabledClicks() {
        document.addEventListener("click", function(e){
            const a = e.target.closest('a.btn, a.qty-btn');
            if (a && a.getAttribute('aria-disabled') === 'true') {
                e.preventDefault(); e.stopPropagation();
            }
        });
    }

    // boot
    document.addEventListener("DOMContentLoaded", function () {
        $all(".cart-row").forEach(bindRow);
        updateCartTotal();
        setupRemoveHandlers();
        setupCheckoutSubmit();
        guardDisabledClicks();
    });
})();
