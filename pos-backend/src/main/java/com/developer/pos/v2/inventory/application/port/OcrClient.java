package com.developer.pos.v2.inventory.application.port;

import com.developer.pos.v2.inventory.application.dto.OcrRawResult;

public interface OcrClient {
    OcrRawResult recognize(String imageUrl);
}
