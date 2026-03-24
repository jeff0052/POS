package com.developer.pos.gto.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.gto.dto.GtoBatchDto;
import com.developer.pos.gto.service.GtoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gto")
public class GtoController {

    private final GtoService gtoService;

    public GtoController(GtoService gtoService) {
        this.gtoService = gtoService;
    }

    @GetMapping("/batches")
    public ApiResponse<List<GtoBatchDto>> listBatches() {
        return ApiResponse.success(gtoService.listBatches());
    }
}
