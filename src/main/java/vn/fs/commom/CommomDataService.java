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
     * Dữ liệu dùng chung cho header/footer/menu:
     * - countProductByCategory
     * - categoryList (navbar)
     * - nxbList (navbar)
     * - totalSave, totalCartItems, cartItems
     */
    public void commonData(Model model, User user) {
        listCategoryByProductName(model);

        Integer totalSave = 0;
        if (user != null) totalSave = favoriteRepository.selectCountSave(user.getUserId());

        Integer totalCartItems = shoppingCartService.getCount();
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();

        model.addAttribute("totalSave", totalSave);
        model.addAttribute("totalCartItems", totalCartItems);
        model.addAttribute("cartItems", cartItems);

        // Navbar data
        model.addAttribute("categoryList", categoryRepository.findAll());
        model.addAttribute("nxbList", nxbRepository.findAll()); // NEW
    }

    // count product by category (đặt đúng tên biến cho template)
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
