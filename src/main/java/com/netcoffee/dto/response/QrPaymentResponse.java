package com.netcoffee.dto.response;

import com.netcoffee.enumtype.QrPaymentStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class QrPaymentResponse {

    private Long id;
    private BigDecimal amountExpected;
    private String referenceCode;
    private QrPaymentStatusEnum status;
    private LocalDateTime expiredAt;

    /**
     * URL ảnh QR từ VietQR API — FE render trực tiếp bằng <img src=...>
     * Format: https://img.vietqr.io/image/{bankBin}-{accountNumber}-compact2.png?...
     */
    private String qrImageUrl;

    /**
     * Nội dung chuyển khoản gợi ý: "BankName | AccountNumber | ReferenceCode"
     */
    private String qrContent;
}