package com.developer.pos.member.service;

import com.developer.pos.member.dto.MemberDto;
import com.developer.pos.member.dto.MemberTierDto;
import com.developer.pos.member.dto.PointsRecordDto;
import com.developer.pos.member.dto.RechargeRecordDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    public List<MemberDto> listMembers() {
        return List.of(
            new MemberDto(1L, "Lina Chen", "13800000001", "Gold", 2860, 32000L, 862000L, 200000L, "ACTIVE"),
            new MemberDto(2L, "Eric Wang", "13800000002", "Silver", 940, 8800L, 248000L, 50000L, "ACTIVE")
        );
    }

    public List<MemberTierDto> listTiers() {
        return List.of(
            new MemberTierDto(1L, "Silver", "Spend over CNY 2,000", List.of("95折", "基础积分")),
            new MemberTierDto(2L, "Gold", "Spend over CNY 8,000", List.of("9折", "会员价", "充值赠送")),
            new MemberTierDto(3L, "Diamond", "Spend over CNY 20,000", List.of("88折", "专属套餐", "双倍积分"))
        );
    }

    public List<RechargeRecordDto> listRecharges() {
        return List.of(
            new RechargeRecordDto(1L, "Lina Chen", "13800000001", 50000L, 8000L, "SUCCESS", "2026-03-20 13:10"),
            new RechargeRecordDto(2L, "Eric Wang", "13800000002", 20000L, 2000L, "SUCCESS", "2026-03-19 19:20")
        );
    }

    public List<PointsRecordDto> listPointsLedger() {
        return List.of(
            new PointsRecordDto(1L, "Lina Chen", "EARN", 120, "POS202603200001", "2026-03-20 09:22"),
            new PointsRecordDto(2L, "Lina Chen", "REDEEM", -200, "Manual order settlement", "2026-03-20 12:11")
        );
    }
}
