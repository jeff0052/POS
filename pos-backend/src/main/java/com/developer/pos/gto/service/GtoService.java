package com.developer.pos.gto.service;

import com.developer.pos.gto.dto.GtoBatchDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GtoService {

    public List<GtoBatchDto> listBatches() {
        return List.of(
            new GtoBatchDto(1L, "GTO2026032001", "2026-03-20", "Riverside Branch", 128, 1268000L, 86000L, "SUCCESS", "2026-03-20 23:59"),
            new GtoBatchDto(2L, "GTO2026032101", "2026-03-21", "Riverside Branch", 132, 1324000L, 91000L, "PENDING", "2026-03-21 23:59")
        );
    }
}
