package com.netcoffee.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Request từ SePay webhook. Docs: https://sepay.vn/lap-trinh-webhook.html
 *
 * <p>SePay gửi JSON dạng: { "id": 1, "gateway": "MBBank", "transactionDate": "2024-01-01 12:00:00",
 * "accountNumber": "0123456789", "code": null, "content": "NCABC123 chuyen tien", "transferType":
 * "in", "transferAmount": 50000, "accumulated": 50000, "subAccount": null, "referenceCode":
 * "FT24001234567", "description": "NCABC123 chuyen tien" }
 */
@Getter
@Setter
public class SePayWebhookRequest {

    private Long id;

    private String gateway; // "MBBank"

    private String transactionDate; // "2024-01-01 12:00:00"

    private String accountNumber; // Số TK nhận tiền

    private String code; // Thường null

    private String content; // "NCABC123 chuyen tien" ← chứa referenceCode

    private String transferType; // "in" = tiền vào, "out" = tiền ra

    private BigDecimal transferAmount;

    private BigDecimal accumulated;

    private String subAccount;

    private String referenceCode; // Mã GD ngân hàng "FT24001234567"

    private String description;
}
