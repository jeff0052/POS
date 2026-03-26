package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.application.command.RegisterMemberCommand;
import com.developer.pos.v2.member.application.dto.MemberDto;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberApplicationServiceTest {

    @Mock private JpaMemberRepository memberRepository;
    @Mock private JpaMemberAccountRepository memberAccountRepository;
    @Mock private JpaMemberPointsLedgerRepository memberPointsLedgerRepository;
    @Mock private JpaActiveTableOrderRepository activeTableOrderRepository;

    @InjectMocks
    private MemberApplicationService service;

    @Nested
    @DisplayName("registerMember")
    class RegisterMember {

        @Test
        @DisplayName("creates member and account for new phone number")
        void createsNewMember() {
            when(memberRepository.findByPhoneNumber("91234567"))
                    .thenReturn(Optional.empty());
            when(memberRepository.save(any())).thenAnswer(inv -> {
                MemberEntity e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(memberAccountRepository.save(any())).thenAnswer(inv -> {
                MemberAccountEntity e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });

            RegisterMemberCommand command = new RegisterMemberCommand(
                    1L, "91234567", "Test User", null
            );

            MemberDto result = service.registerMember(command);

            assertNotNull(result);
            verify(memberRepository).save(any());
            verify(memberAccountRepository).save(any());
        }

        @Test
        @DisplayName("throws when phone number already registered")
        void throwsOnDuplicate() {
            MemberEntity existing = new MemberEntity();
            existing.setId(1L);
            existing.setPhoneNumber("91234567");
            when(memberRepository.findByPhoneNumber("91234567"))
                    .thenReturn(Optional.of(existing));

            RegisterMemberCommand command = new RegisterMemberCommand(
                    1L, "91234567", "Test User", null
            );

            assertThrows(IllegalStateException.class,
                    () -> service.registerMember(command));
        }
    }

    @Nested
    @DisplayName("getMember")
    class GetMember {

        @Test
        @DisplayName("returns member with account when found")
        void returnsMember() {
            MemberEntity member = new MemberEntity();
            member.setId(1L);
            member.setMerchantId(1L);
            member.setPhoneNumber("91234567");
            member.setDisplayName("Test User");
            member.setTierCode("STANDARD");
            member.setCreatedAt(OffsetDateTime.now());
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            MemberAccountEntity account = new MemberAccountEntity();
            account.setId(1L);
            account.setMemberId(1L);
            account.setTotalPointsBalance(100L);
            account.setCashBalanceCents(5000L);
            account.setLifetimeSpendCents(50000L);
            when(memberAccountRepository.findByMemberId(1L))
                    .thenReturn(Optional.of(account));

            MemberDto result = service.getMember(1L);

            assertNotNull(result);
            assertEquals("91234567", result.phoneNumber());
            assertEquals(100L, result.totalPointsBalance());
            assertEquals(5000L, result.cashBalanceCents());
        }

        @Test
        @DisplayName("throws when member not found")
        void throwsWhenNotFound() {
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.getMember(999L));
        }
    }
}
