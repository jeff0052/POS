package com.developer.pos.v2.channel.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.channel.application.dto.ChannelDto;
import com.developer.pos.v2.channel.application.dto.ChannelSettlementBatchDto;
import com.developer.pos.v2.channel.application.dto.CommissionResultDto;
import com.developer.pos.v2.channel.application.service.ChannelAttributionService;
import com.developer.pos.v2.channel.application.service.ChannelService;
import com.developer.pos.v2.channel.application.service.ChannelSettlementService;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/merchants/{merchantId}/channels")
public class ChannelV2Controller implements V2Api {

    private final ChannelService channelService;
    private final ChannelAttributionService attributionService;
    private final ChannelSettlementService settlementService;

    public ChannelV2Controller(ChannelService channelService,
                               ChannelAttributionService attributionService,
                               ChannelSettlementService settlementService) {
        this.channelService = channelService;
        this.attributionService = attributionService;
        this.settlementService = settlementService;
    }

    @GetMapping
    public ApiResponse<List<ChannelDto>> listChannels(@PathVariable Long merchantId) {
        return ApiResponse.success(channelService.listChannels(merchantId));
    }

    @GetMapping("/{channelId}")
    public ApiResponse<ChannelDto> getChannel(@PathVariable Long merchantId,
                                              @PathVariable Long channelId) {
        return ApiResponse.success(channelService.getChannel(merchantId, channelId));
    }

    @PostMapping
    public ApiResponse<ChannelDto> createChannel(@PathVariable Long merchantId,
                                                 @RequestBody Map<String, String> body) {
        return ApiResponse.success(channelService.createChannel(
                merchantId,
                body.get("channelCode"),
                body.get("channelName"),
                body.get("channelType"),
                body.get("contactName"),
                body.get("contactPhone"),
                body.get("contactEmail")
        ));
    }

    @PutMapping("/{channelId}")
    public ApiResponse<ChannelDto> updateChannel(@PathVariable Long merchantId,
                                                 @PathVariable Long channelId,
                                                 @RequestBody Map<String, String> body) {
        return ApiResponse.success(channelService.updateChannel(
                merchantId, channelId,
                body.get("channelName"),
                body.get("channelType"),
                body.get("contactName"),
                body.get("contactPhone"),
                body.get("contactEmail")
        ));
    }

    @PostMapping("/{channelId}/deactivate")
    public ApiResponse<Void> deactivateChannel(@PathVariable Long merchantId,
                                               @PathVariable Long channelId) {
        channelService.deactivateChannel(merchantId, channelId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{channelId}/attribute-order")
    public ApiResponse<Void> attributeOrder(@PathVariable Long merchantId,
                                            @PathVariable Long channelId,
                                            @RequestBody Map<String, Object> body) {
        Long submittedOrderId = Long.valueOf(body.get("submittedOrderId").toString());
        String trackingValue = (String) body.get("trackingValue");
        attributionService.attributeOrder(submittedOrderId, channelId, trackingValue);
        return ApiResponse.success(null);
    }

    @PostMapping("/{channelId}/calculate-commission")
    public ApiResponse<CommissionResultDto> calculateCommission(@PathVariable Long merchantId,
                                                                @PathVariable Long channelId,
                                                                @RequestBody Map<String, Object> body) {
        Long submittedOrderId = Long.valueOf(body.get("submittedOrderId").toString());
        Long storeId = Long.valueOf(body.get("storeId").toString());
        long orderAmountCents = Long.parseLong(body.get("orderAmountCents").toString());
        return ApiResponse.success(attributionService.calculateCommission(
                channelId, submittedOrderId, storeId, orderAmountCents
        ));
    }

    @PostMapping("/{channelId}/settlement-batches")
    public ApiResponse<ChannelSettlementBatchDto> generateBatch(@PathVariable Long merchantId,
                                                                @PathVariable Long channelId,
                                                                @RequestBody Map<String, String> body) {
        LocalDate periodStart = LocalDate.parse(body.get("periodStart"));
        LocalDate periodEnd = LocalDate.parse(body.get("periodEnd"));
        return ApiResponse.success(settlementService.generateBatch(channelId, periodStart, periodEnd));
    }
}
