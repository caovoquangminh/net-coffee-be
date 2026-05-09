package com.netcoffee.dto.response;

import com.netcoffee.enumtype.QrPaymentStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class QrPaymentResponse
{

    private Long id;
    private BigDecimal amountExpected;
    private String referenceCode;
    private QrPaymentStatusEnum status;
    private LocalDateTime expiredAt;

    /**
     * Base64 QR image hoặc URL để client render
     */
    private String qrImageBase64;
    private String qrContent; // Nội dung chuyển khoản gợi ý
}
