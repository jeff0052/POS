package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2MemberEntity")
@Table(name = "members")
public class MemberEntity {

    @Id
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

    protected MemberEntity() {
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
