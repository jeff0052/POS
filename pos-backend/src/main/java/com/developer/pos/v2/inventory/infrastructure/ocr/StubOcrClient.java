package com.developer.pos.v2.inventory.infrastructure.ocr;

import com.developer.pos.v2.inventory.application.dto.OcrLineItem;
import com.developer.pos.v2.inventory.application.dto.OcrRawResult;
import com.developer.pos.v2.inventory.application.port.OcrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class StubOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(StubOcrClient.class);

    @Override
    public OcrRawResult recognize(String imageUrl) {
        log.info("StubOcrClient: simulating OCR for image {}", imageUrl);
        return new OcrRawResult(
            "示例供应商",
            LocalDate.now().toString(),
            80000L,
            new BigDecimal("0.85"),
            List.of(
                new OcrLineItem("牛腩 5kg", new BigDecimal("5.0"), "kg", 8000L, 40000L),
                new OcrLineItem("大米 10kg", new BigDecimal("10.0"), "kg", 3000L, 30000L),
                new OcrLineItem("辣椒 2kg", new BigDecimal("2.0"), "kg", 5000L, 10000L)
            )
        );
    }
}
