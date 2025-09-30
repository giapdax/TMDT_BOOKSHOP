package vn.fs.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.commom.CommomDataService;
import vn.fs.dto.RefundResult;
import vn.fs.entities.Order;
import vn.fs.repository.OrderPaymentRepository;
import vn.fs.repository.OrderRepository;
import vn.fs.service.paypal.PaypalRefundService;

import java.util.Date;
import java.util.Optional;

// Admin controller cho refund 1 lần toàn phần (PayPal)
@Controller
@RequestMapping("/admin/paypal")
public class AdminPaypalController {

    // 0 Chờ xác nhận, 1 Đang giao, 2 Đã thanh toán, 3 Đã hủy, 4 Đã hoàn tiền
    public static final int ORDER_STATUS_REFUNDED = 4;

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository paymentRepo;
    private final PaypalRefundService paypalRefundService;
    private final CommomDataService commomDataService;

    public AdminPaypalController(OrderRepository orderRepository,
                                 OrderPaymentRepository paymentRepo,
                                 PaypalRefundService paypalRefundService,
                                 CommomDataService commomDataService) {
        this.orderRepository = orderRepository;
        this.paymentRepo = paymentRepo;
        this.paypalRefundService = paypalRefundService;
        this.commomDataService = commomDataService;
    }

    @GetMapping("/refund/{orderId}")
    public String showRefundPage(@PathVariable("orderId") Long orderId,
                                 Model model,
                                 RedirectAttributes ra) {
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("toast_error", "Không tìm thấy đơn hàng: " + orderId);
            return "redirect:/admin/orders";
        }
        Order order = opt.get();

        if (order.getStatus() == ORDER_STATUS_REFUNDED) {
            ra.addFlashAttribute("toast_error", "Đơn đã được hoàn trước đó.");
            return "redirect:/admin/orders";
        }
        if (order.getPaypalCaptureId() == null || order.getPaypalCaptureId().isBlank()) {
            ra.addFlashAttribute("toast_error", "Đơn này không có captureId PayPal → không thể hoàn online.");
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        return "admin/paypal-refund";
    }

    // Hoàn tiền toàn phần (one-shot)
    @PostMapping("/refund")
    @Transactional
    public String doRefund(@RequestParam("orderId") Long orderId, RedirectAttributes ra) {
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("toast_error", "Không tìm thấy đơn hàng: " + orderId);
            return "redirect:/admin/orders";
        }
        Order order = opt.get();

        if (order.getStatus() == ORDER_STATUS_REFUNDED) {
            ra.addFlashAttribute("toast_error", "Đơn đã hoàn 1 lần rồi.");
            return "redirect:/admin/orders";
        }
        String captureId = order.getPaypalCaptureId();
        if (captureId == null || captureId.isBlank()) {
            ra.addFlashAttribute("toast_error", "Đơn này không có captureId PayPal.");
            return "redirect:/admin/orders";
        }

        try {
            // amount = null => FULL REFUND
            RefundResult result = paypalRefundService.refundCapture(
                    captureId, null, null, "Admin full refund (one shot)");

            // Cập nhật payment (nếu có record)
            paymentRepo.findByProviderAndExternalCaptureId("PAYPAL", captureId).ifPresent(pay -> {
                pay.setStatus("REFUNDED");
                pay.setRefundId(result.getRefundId());
                pay.setRefundAmount(null); // nếu bạn lưu USD ở chỗ khác, set ở đây
                pay.setRefundCurrency("USD");
                Date now = new Date();
                pay.setRefundedAt(now);
                pay.setUpdatedAt(now);
                pay.setMessage("Refund OK - RefundID=" + result.getRefundId());
                paymentRepo.save(pay);
            });

            // Cập nhật đơn: 4 - Đã hoàn tiền
            order.setStatus(ORDER_STATUS_REFUNDED);
            orderRepository.save(order);

            // Lên lịch gửi mail SAU KHI COMMIT
            String email = (order.getUser() != null) ? order.getUser().getEmail() : null;
            if (email != null && !email.isEmpty()) {
                double refundedAmount = order.getAmount(); // full refund = tổng tiền đơn
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            commomDataService.enqueueRefundSuccessEmail(
                                    email, order, refundedAmount, "Hoàn tiền toàn phần");
                        } catch (Exception ignore) { }
                    }
                });
            }

            ra.addFlashAttribute("toast",
                    "Hoàn tiền thành công. RefundID: " + result.getRefundId() + " · Trạng thái: " + result.getStatus());
        } catch (Exception ex) {
            String msg = (ex.getMessage() == null) ? "" : ex.getMessage();

            if (msg.contains("CAPTURE_FULLY_REFUNDED")) {
                // Đồng bộ nếu PayPal báo đã hoàn trước đó
                order.setStatus(ORDER_STATUS_REFUNDED);
                orderRepository.save(order);

                String email = (order.getUser() != null) ? order.getUser().getEmail() : null;
                if (email != null && !email.isEmpty()) {
                    double refundedAmount = order.getAmount();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                commomDataService.enqueueRefundSuccessEmail(
                                        email, order, refundedAmount, "Đồng bộ trạng thái refund");
                            } catch (Exception ignore) { }
                        }
                    });
                }

                ra.addFlashAttribute("toast", "Capture đã hoàn trước đó. Đã cập nhật đơn về 'Đã hoàn tiền'.");
            } else {
                // Lên lịch gửi mail thất bại sau commit (trạng thái đơn có thể không đổi)
                String email = (order.getUser() != null) ? order.getUser().getEmail() : null;
                if (email != null && !email.isEmpty()) {
                    final String reason = msg;
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                commomDataService.enqueueRefundFailedEmail(
                                        email, order, reason, "support@bookshop.local");
                            } catch (Exception ignore) { }
                        }
                    });
                }
                ra.addFlashAttribute("toast_error", "Hoàn tiền thất bại: " + msg);
            }
        }

        return "redirect:/admin/orders";
    }
}
