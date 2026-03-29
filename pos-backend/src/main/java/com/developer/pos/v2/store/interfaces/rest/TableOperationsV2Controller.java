package com.developer.pos.v2.store.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.store.application.dto.QrTokenResultDto;
import com.developer.pos.v2.store.application.dto.ReservationDto;
import com.developer.pos.v2.store.application.dto.TableCleanResultDto;
import com.developer.pos.v2.store.application.dto.TableMergeResultDto;
import com.developer.pos.v2.store.application.dto.TableTransferResultDto;
import com.developer.pos.v2.store.application.dto.TableUnmergeResultDto;
import com.developer.pos.v2.store.application.service.QrTokenService;
import com.developer.pos.v2.store.application.service.ReservationApplicationService;
import com.developer.pos.v2.store.application.service.TableCleanService;
import com.developer.pos.v2.store.application.service.TableMergeApplicationService;
import com.developer.pos.v2.store.application.service.TableTransferApplicationService;
import com.developer.pos.v2.store.interfaces.rest.request.SeatReservationRequest;
import com.developer.pos.v2.store.interfaces.rest.request.TableMergeRequest;
import com.developer.pos.v2.store.interfaces.rest.request.TableTransferRequest;
import com.developer.pos.v2.store.interfaces.rest.request.TableUnmergeRequest;
import com.developer.pos.v2.store.interfaces.rest.request.UpsertReservationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/stores/{storeId}")
public class TableOperationsV2Controller implements V2Api {

    private final ReservationApplicationService reservationApplicationService;
    private final TableTransferApplicationService tableTransferApplicationService;
    private final TableMergeApplicationService tableMergeApplicationService;
    private final TableCleanService tableCleanService;
    private final QrTokenService qrTokenService;

    public TableOperationsV2Controller(
            ReservationApplicationService reservationApplicationService,
            TableTransferApplicationService tableTransferApplicationService,
            TableMergeApplicationService tableMergeApplicationService,
            TableCleanService tableCleanService,
            QrTokenService qrTokenService
    ) {
        this.reservationApplicationService = reservationApplicationService;
        this.tableTransferApplicationService = tableTransferApplicationService;
        this.tableMergeApplicationService = tableMergeApplicationService;
        this.tableCleanService = tableCleanService;
        this.qrTokenService = qrTokenService;
    }

    @GetMapping("/reservations")
    public ApiResponse<List<ReservationDto>> listReservations(@PathVariable Long storeId) {
        return ApiResponse.success(reservationApplicationService.listByStore(storeId));
    }

    @PostMapping("/reservations")
    public ApiResponse<ReservationDto> createReservation(
            @PathVariable Long storeId,
            @Valid @RequestBody UpsertReservationRequest request
    ) {
        return ApiResponse.success(
                reservationApplicationService.create(
                        storeId,
                        request.guestName(),
                        request.contactPhone(),
                        request.reservationTime(),
                        request.partySize(),
                        request.reservationStatus(),
                        request.area(),
                        request.tableId()
                )
        );
    }

    @PutMapping("/reservations/{reservationId}")
    public ApiResponse<ReservationDto> updateReservation(
            @PathVariable Long storeId,
            @PathVariable Long reservationId,
            @Valid @RequestBody UpsertReservationRequest request
    ) {
        return ApiResponse.success(
                reservationApplicationService.update(
                        storeId,
                        reservationId,
                        request.guestName(),
                        request.contactPhone(),
                        request.reservationTime(),
                        request.partySize(),
                        request.reservationStatus(),
                        request.area()
                )
        );
    }

    @PostMapping("/reservations/{reservationId}/seat")
    public ApiResponse<ReservationDto> seatReservation(
            @PathVariable Long storeId,
            @PathVariable Long reservationId,
            @RequestBody(required = false) SeatReservationRequest request
    ) {
        return ApiResponse.success(reservationApplicationService.seat(storeId, reservationId, request != null ? request.tableId() : null));
    }

    @PostMapping("/tables/{tableId}/transfer")
    public ApiResponse<TableTransferResultDto> transferTable(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @Valid @RequestBody TableTransferRequest request
    ) {
        return ApiResponse.success(tableTransferApplicationService.transfer(storeId, tableId, request.destinationTableId()));
    }

    @PostMapping("/tables/merge")
    public ApiResponse<TableMergeResultDto> mergeTables(
            @PathVariable Long storeId,
            @Valid @RequestBody TableMergeRequest request
    ) {
        return ApiResponse.success(tableMergeApplicationService.merge(storeId, request.masterTableId(), request.mergedTableId()));
    }

    @PostMapping("/tables/unmerge")
    public ApiResponse<TableUnmergeResultDto> unmergeTables(
            @PathVariable Long storeId,
            @Valid @RequestBody TableUnmergeRequest request
    ) {
        return ApiResponse.success(tableMergeApplicationService.unmerge(storeId, request.mergeRecordId()));
    }

    @PostMapping("/tables/{tableId}/mark-clean")
    public ApiResponse<TableCleanResultDto> markTableClean(
            @PathVariable Long storeId,
            @PathVariable Long tableId
    ) {
        return ApiResponse.success(tableCleanService.markClean(storeId, tableId));
    }

    @PostMapping("/tables/{tableId}/qr/refresh")
    public ApiResponse<QrTokenResultDto> refreshQr(
            @PathVariable Long storeId,
            @PathVariable Long tableId
    ) {
        return ApiResponse.success(qrTokenService.refreshQr(storeId, tableId));
    }
}
