package com.developer.pos.v2.kitchen.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.kitchen.application.dto.KitchenStationDto;
import com.developer.pos.v2.kitchen.application.dto.KitchenTicketDto;
import com.developer.pos.v2.kitchen.application.dto.StationHeartbeatDto;
import com.developer.pos.v2.kitchen.application.service.KdsHeartbeatService;
import com.developer.pos.v2.kitchen.application.service.KitchenStationService;
import com.developer.pos.v2.kitchen.application.service.KitchenTicketQueryService;
import com.developer.pos.v2.kitchen.application.service.TicketStatusService;
import com.developer.pos.v2.kitchen.interfaces.rest.request.CreateStationRequest;
import com.developer.pos.v2.kitchen.interfaces.rest.request.UpdateTicketStatusRequest;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class KitchenV2Controller implements V2Api {

    private final KdsHeartbeatService heartbeatService;
    private final TicketStatusService ticketStatusService;
    private final KitchenTicketQueryService ticketQueryService;
    private final KitchenStationService stationService;

    public KitchenV2Controller(KdsHeartbeatService heartbeatService,
                                TicketStatusService ticketStatusService,
                                KitchenTicketQueryService ticketQueryService,
                                KitchenStationService stationService) {
        this.heartbeatService = heartbeatService;
        this.ticketStatusService = ticketStatusService;
        this.ticketQueryService = ticketQueryService;
        this.stationService = stationService;
    }

    /** KDS device heartbeat. storeId derived from station row, not required in path. */
    @PostMapping("/stations/{stationId}/heartbeat")
    public ApiResponse<StationHeartbeatDto> heartbeat(@PathVariable Long stationId) {
        return ApiResponse.success(heartbeatService.heartbeat(stationId));
    }

    /** Advance or cancel a kitchen ticket. */
    @PutMapping("/kitchen-tickets/{ticketId}/status")
    public ApiResponse<KitchenTicketDto> updateTicketStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ApiResponse.success(ticketStatusService.updateStatus(ticketId, request.newStatus()));
    }

    /** List kitchen tickets for a store, optionally filtered by station and status. */
    @GetMapping("/stores/{storeId}/kitchen-tickets")
    public ApiResponse<List<KitchenTicketDto>> listTickets(
            @PathVariable Long storeId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(ticketQueryService.listTickets(storeId, stationId, status));
    }

    /** Create a new kitchen station (admin). */
    @PostMapping("/stores/{storeId}/kitchen-stations")
    public ApiResponse<KitchenStationDto> createStation(
            @PathVariable Long storeId,
            @Valid @RequestBody CreateStationRequest request) {
        return ApiResponse.success(stationService.createStation(storeId, request));
    }

    /** List active kitchen stations for a store. */
    @GetMapping("/stores/{storeId}/kitchen-stations")
    public ApiResponse<List<KitchenStationDto>> listStations(@PathVariable Long storeId) {
        return ApiResponse.success(stationService.listStations(storeId));
    }
}
