package com.developer.pos.v2.member.infrastructure.persistence.entity;

import com.developer.pos.v2.common.entity.BaseAuditableEntity;
import com.developer.pos.v2.mcp.ActionContextAuditListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2MemberEntity")
@Table(name = "members")
@EntityListeners(ActionContextAuditListener.class)
public class MemberEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "member_no", nullable = false)
    private String memberNo;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "tier_code", nullable = false)
    private String tierCode;

    @Column(name = "member_status", nullable = false)
    private String memberStatus;

    public MemberEntity() {
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getTierCode() {
        return tierCode;
    }

    public String getMemberStatus() {
        return memberStatus;
    }
}
