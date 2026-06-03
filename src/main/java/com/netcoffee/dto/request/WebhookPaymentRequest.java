package com.netcoffee.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Request từ webhook ngân hàng (SePay / Casso / etc.) Các field có thể thay đổi tuỳ nhà cung cấp
 * webhook
 */
@Getter
@Setter
public class WebhookPaymentRequest {

    private String transferContent; // Nội dung CK — chứa referenceCode
    private BigDecimal transferAmount;
    private String bankCode;
    private String transactionId; // Mã GD phía ngân hàng
}
