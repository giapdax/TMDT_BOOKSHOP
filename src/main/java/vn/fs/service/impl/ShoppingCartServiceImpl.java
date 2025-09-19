package vn.fs.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.fs.entities.CartItem;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;
import vn.fs.service.ShoppingCartService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu giỏ hàng theo USER (in-memory).
 * Map: userId -> (productId -> CartItem)
 */
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final UserRepository userRepository;

    private final Map<Long, Map<Long, CartItem>> store = new ConcurrentHashMap<>();

    /* ===== Current user ===== */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        String login = auth.getName();
        Optional<User> u = userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .or(() -> userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login));
        return u.map(User::getUserId).orElse(null);
    }

    private Map<Long, CartItem> cart() {
        Long uid = currentUserId();
        if (uid == null) return new HashMap<>(); // anonymous -> empty (không xài session)
        return store.computeIfAbsent(uid, k -> new ConcurrentHashMap<>());
    }

    @Override
    public void add(CartItem item) {
        Map<Long, CartItem> c = cart();
        CartItem existed = c.get(item.getId());
        if (existed != null) {
            existed.setQuantity(existed.getQuantity() + item.getQuantity());
        } else {
            c.put(item.getId(), item);
        }
    }

    @Override
    public void remove(CartItem item) {
        cart().remove(item.getId());
    }

    @Override
    public Collection<CartItem> getCartItems() {
        return cart().values();
    }

    @Override
    public void clear() {
        cart().clear();
    }

    @Override
    public double getAmount() {
        return cart().values().stream()
                .mapToDouble(CartItem::getLineTotal)
                .sum();
    }

    @Override
    public int getCount() {
        // sum qty (chuẩn UX)
        return cart().values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Override
    public void remove(Product product) {
        if (product != null) cart().remove(product.getProductId());
    }

    @Override
    public CartItem getItem(Long productId) {
        return cart().get(productId);
    }

    @Override
    public void updateQuantity(Long productId, int quantity) {
        CartItem ci = cart().get(productId);
        if (ci != null) ci.setQuantity(Math.max(1, quantity));
    }

    @Override
    public void increase(Long productId, int step) {
        CartItem ci = cart().get(productId);
        if (ci != null) ci.setQuantity(ci.getQuantity() + Math.max(1, step));
    }

    @Override
    public void decrease(Long productId, int step) {
        CartItem ci = cart().get(productId);
        if (ci != null) ci.setQuantity(Math.max(1, ci.getQuantity() - Math.max(1, step)));
    }
}
