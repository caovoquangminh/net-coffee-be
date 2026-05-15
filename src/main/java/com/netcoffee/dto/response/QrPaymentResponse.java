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

    private String qrImageUrl;
    private String qrContent;
}
