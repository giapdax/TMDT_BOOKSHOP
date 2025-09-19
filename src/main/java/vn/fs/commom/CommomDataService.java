package vn.fs.commom;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import vn.fs.entities.CartItem;
import vn.fs.entities.Order;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.FavoriteRepository;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.service.ShoppingCartService;

@Service
public class CommomDataService {

    @Autowired FavoriteRepository favoriteRepository;
    @Autowired ShoppingCartService shoppingCartService;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired NxbRepository nxbRepository;

    @Autowired public JavaMailSender emailSender;
    @Autowired TemplateEngine templateEngine;

    /**
     * Dữ liệu dùng chung cho header/footer/menu.
     * - totalCartItems (giữ tương thích) = số mặt hàng KHÁC NHAU
     * - totalCartDistinct: số mặt hàng KHÁC NHAU (badge header)
     * - totalCartQtySum: tổng số lượng (mini-cart)
     * - totalPrice: tổng tiền
     * - cartItems: danh sách item mini-cart
     */
    public void commonData(Model model, User user) {
        listCategoryByProductName(model);

        Integer totalSave = 0;
        if (user != null) {
            totalSave = favoriteRepository.selectCountSave(user.getUserId());
        }

        int distinct = 0;
        int qtySum   = 0;
        double totalPrice = 0.0;
        Collection<CartItem> cartItems = null;

        try {
            distinct   = shoppingCartService.getDistinctCount(); // <<=== thay cho getCount()
            qtySum     = shoppingCartService.getQuantitySum();
            totalPrice = shoppingCartService.getAmount();
            cartItems  = shoppingCartService.getCartItems();
        } catch (Exception ignore) {
            // user chưa đăng nhập / giỏ trống -> để mặc định
        }

        model.addAttribute("totalSave", totalSave);

        // Giữ biến cũ để không vỡ view
        model.addAttribute("totalCartItems", distinct);

        // Biến rõ nghĩa
        model.addAttribute("totalCartDistinct", distinct);
        model.addAttribute("totalCartQtySum", qtySum);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);

        // Navbar
        model.addAttribute("categoryList", categoryRepository.findAll());
        model.addAttribute("nxbList", nxbRepository.findAll());
    }

    // count product by category
    public void listCategoryByProductName(Model model) {
        List<Object[]> countProductByCategory = productRepository.listCategoryByProductName();
        model.addAttribute("countProductByCategory", countProductByCategory);
    }

    // Gửi email xác nhận đặt hàng
    public void sendSimpleEmail(String email, String subject, String contentEmail,
                                Collection<CartItem> cartItems, double totalPrice, Order orderFinal)
            throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();

        Context ctx = new Context(locale);
        ctx.setVariable("cartItems", cartItems);
        ctx.setVariable("totalPrice", totalPrice);
        ctx.setVariable("orderFinal", orderFinal);

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setSubject(subject);
        helper.setTo(email);

        String htmlContent = templateEngine.process("mail/email_en.html", ctx);
        helper.setText(htmlContent, true);

        emailSender.send(mimeMessage);
    }
}
