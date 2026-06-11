package com.netcoffee.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.netcoffee.dto.request.SePayWebhookRequest;
import com.netcoffee.service.FoodOrderService;
import com.netcoffee.service.QrPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Bảo vệ webhook nạp tiền — đây là điểm gian lận nghiêm trọng nhất: nếu không xác thực, kẻ gian
 * POST webhook giả để nạp tiền khống. Test đảm bảo webhook chỉ chạy khi đúng chữ ký SePay.
 */
@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock QrPaymentService qrPaymentService;
    @Mock FoodOrderService foodOrderService;
    @Mock HttpServletRequest httpRequest;

    WebhookController controller;

    private static final String SECRET = "super-secret-key";

    @BeforeEach
    void setUp() {
        controller = new WebhookController(qrPaymentService, foodOrderService);
        ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
    }

    private SePayWebhookRequest topUpRequest() {
        SePayWebhookRequest req = new SePayWebhookRequest();
        req.setTransferType("in");
        req.setContent("NCABC123 chuyen tien");
        req.setTransferAmount(new BigDecimal("9999999"));
        return req;
    }

    @Test
    @DisplayName("Không có header Authorization → 401, KHÔNG xử lý nạp tiền")
    void missingHeader_rejected() {
        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        var resp = controller.receivePayment(topUpRequest(), httpRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(qrPaymentService, foodOrderService);
    }

    @Test
    @DisplayName("Sai API key → 401, KHÔNG nạp tiền khống")
    void wrongKey_rejected() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Apikey wrong-key");

        var resp = controller.receivePayment(topUpRequest(), httpRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(qrPaymentService, foodOrderService);
    }

    @Test
    @DisplayName("Đúng API key (định dạng 'Apikey <key>') → xử lý nạp tiền")
    void correctKey_processed() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Apikey " + SECRET);

        var resp = controller.receivePayment(topUpRequest(), httpRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(qrPaymentService, times(1)).processWebhook(any());
    }

    @Test
    @DisplayName("Đúng key dạng raw (không tiền tố Apikey) → vẫn chấp nhận")
    void correctRawKey_processed() {
        when(httpRequest.getHeader("Authorization")).thenReturn(SECRET);

        var resp = controller.receivePayment(topUpRequest(), httpRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(qrPaymentService, times(1)).processWebhook(any());
    }

    @Test
    @DisplayName("Fail-closed: secret chưa cấu hình → từ chối mọi webhook")
    void noSecretConfigured_rejectsAll() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "");
        // Không cần stub header: secret rỗng → từ chối ngay trước khi đọc header

        var resp = controller.receivePayment(topUpRequest(), httpRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(qrPaymentService, foodOrderService);
    }
}
