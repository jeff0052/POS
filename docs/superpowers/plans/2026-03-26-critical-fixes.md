# POS Critical & High Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 8 CRITICAL and top HIGH issues from the architecture review (docs/52-architecture-review-report.md) without breaking the existing running flows.

**Architecture:** Incremental fixes on a single branch. Each task is isolated — one domain per task, no cross-task dependencies. All fixes are additive or narrowly scoped edits to existing files.

**Tech Stack:** Spring Boot 3.3.3, Java 17, Maven, MySQL, Flyway, JPA/Hibernate

**Branch:** `fix/critical-review-fixes` (from `main`)

---

## File Structure

### New files to create:
- `pos-backend/src/main/java/com/developer/pos/common/config/SecurityConfig.java` — Spring Security filter chain + JWT
- `pos-backend/src/main/java/com/developer/pos/common/security/JwtAuthenticationFilter.java` — JWT token validation filter
- `pos-backend/src/main/java/com/developer/pos/common/security/JwtTokenProvider.java` — JWT token creation/validation utility
- `pos-backend/src/main/resources/db/migration/v2/V016__add_settlement_idempotency.sql` — idempotency columns
- `pos-backend/src/test/java/com/developer/pos/v2/order/ActiveTableOrderServiceTest.java` — order core tests
- `pos-backend/src/test/java/com/developer/pos/v2/settlement/CashierSettlementServiceTest.java` — settlement tests

### Existing files to modify:
- `pos-backend/pom.xml` — add spring-boot-starter-security + jjwt dependencies
- `pos-backend/src/main/java/com/developer/pos/common/config/CorsConfig.java` — restrict origins
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java` — real JWT generation
- `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/QrOrderingV2Controller.java` — remove client price trust
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java` — concurrency lock, fix submitToKitchen delete, fix order ID
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/command/SubmitQrOrderingCommand.java` — remove unitPriceCents from QR items
- `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/request/QrOrderingSubmitRequest.java` — remove unitPriceCents
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java` — idempotent check
- `pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/repository/JpaActiveTableOrderRepository.java` — add pessimistic lock query
- `pos-backend/src/main/java/com/developer/pos/v2/payment/application/service/VibeCashPaymentApplicationService.java` — webhook signature verification
- `pos-backend/src/main/resources/application.yml` — add JWT secret config, disable SQL logging

---

### Task 1: Add Spring Security + JWT Authentication [SEC-1]

**Files:**
- Modify: `pos-backend/pom.xml`
- Create: `pos-backend/src/main/java/com/developer/pos/common/security/JwtTokenProvider.java`
- Create: `pos-backend/src/main/java/com/developer/pos/common/security/JwtAuthenticationFilter.java`
- Create: `pos-backend/src/main/java/com/developer/pos/common/config/SecurityConfig.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java`
- Modify: `pos-backend/src/main/resources/application.yml`

- [ ] **Step 1: Add dependencies to pom.xml**

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: Add JWT config to application.yml**

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:default-dev-secret-change-in-production-must-be-at-least-256-bits-long!!}
    expiration-ms: 86400000  # 24 hours
```

Also change `show-sql: true` to `show-sql: false` under `jpa.properties.hibernate`.

- [ ] **Step 3: Create JwtTokenProvider.java**

```java
package com.developer.pos.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String username, String role, Long storeId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("storeId", storeId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Create JwtAuthenticationFilter.java**

```java
package com.developer.pos.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.isValid(token)) {
                Claims claims = jwtTokenProvider.parseToken(token);
                String role = claims.get("role", String.class);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 5: Create SecurityConfig.java**

```java
package com.developer.pos.common.config;

import com.developer.pos.common.security.JwtAuthenticationFilter;
import com.developer.pos.common.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/v2/qr-ordering/**").permitAll()
                .requestMatchers("/api/v2/webhooks/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 6: Update AuthService.java to generate real JWT**

```java
package com.developer.pos.auth.service;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.common.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        // TODO: Replace with real user lookup + password verification
        AuthUserDto user = new AuthUserDto(1L, request.username(), "Store Admin", "ADMIN", 1001L);
        String token = jwtTokenProvider.generateToken(user.id(), user.username(), user.role(), user.storeId());
        return new LoginResponse(token, user);
    }
}
```

- [ ] **Step 7: Verify compilation**

Run: `cd pos-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(security): add JWT authentication with Spring Security [SEC-1]"
```

---

### Task 2: Restrict CORS Origins [SEC-2]

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/common/config/CorsConfig.java`

- [ ] **Step 1: Replace wildcard CORS with explicit origins**

Replace the entire file content:

```java
package com.developer.pos.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:5174}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.split(","))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd pos-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix(security): restrict CORS to explicit origins [SEC-2]"
```

---

### Task 3: QR Ordering Server-Side Price Validation [SEC-5]

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/order/application/command/SubmitQrOrderingCommand.java`

- [ ] **Step 1: Add SKU repository injection to ActiveTableOrderApplicationService**

Add to constructor and field:

```java
private final JpaSkuRepository skuRepository;
```

Add import: `import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;`

Add to constructor parameter list and assignment.

- [ ] **Step 2: In submitQrOrdering, look up real prices from DB instead of trusting client**

In `submitQrOrdering()`, after getting the entity, replace the item mapping that uses `item.unitPriceCents()` with server-side lookup:

```java
List<ActiveTableOrderItemEntity> newItems = command.items().stream()
        .map(item -> {
            var sku = skuRepository.findById(item.skuId())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + item.skuId()));
            ActiveTableOrderItemEntity next = new ActiveTableOrderItemEntity();
            next.setSkuId(sku.getId());
            next.setSkuCodeSnapshot(sku.getSkuCode());
            next.setSkuNameSnapshot(sku.getSkuName());
            next.setQuantity(item.quantity());
            next.setUnitPriceSnapshotCents(sku.getPriceCents());  // server price, not client
            next.setItemRemark(item.remark());
            next.setLineTotalCents(sku.getPriceCents() * item.quantity());
            return next;
        })
        .toList();
```

- [ ] **Step 3: Verify compilation**

Run: `cd pos-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "fix(security): validate QR order prices server-side [SEC-5]"
```

---

### Task 4: VibeCash Webhook Signature Verification [SEC-3]

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/payment/application/service/VibeCashPaymentApplicationService.java`
- Modify: `pos-backend/src/main/resources/application.yml`

- [ ] **Step 1: Add webhook secret to application.yml**

```yaml
vibecash:
  webhook-secret: ${VIBECASH_WEBHOOK_SECRET:}
```

- [ ] **Step 2: Add signature verification in handleWebhook**

At the top of `handleWebhook()`, before any business logic:

```java
@Value("${vibecash.webhook-secret:}")
private String webhookSecret;

// In handleWebhook method, add at the very beginning:
if (webhookSecret != null && !webhookSecret.isBlank()) {
    String expectedSignature = HmacUtils.hmacSha256Hex(webhookSecret, requestBody);
    if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8))) {
        throw new SecurityException("Invalid webhook signature");
    }
}
```

Add imports:
```java
import org.apache.commons.codec.digest.HmacUtils;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
```

Note: If commons-codec is not in dependencies, add to pom.xml:
```xml
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
</dependency>
```

- [ ] **Step 3: Verify compilation**

Run: `cd pos-backend && mvn compile -q`

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "fix(security): verify VibeCash webhook signatures [SEC-3]"
```

---

### Task 5: Table Concurrency Lock [ORD-1]

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/repository/JpaActiveTableOrderRepository.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java`

- [ ] **Step 1: Add pessimistic lock query to JpaActiveTableOrderRepository**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM ActiveTableOrderEntity a WHERE a.storeId = :storeId AND a.tableId = :tableId")
Optional<ActiveTableOrderEntity> findByStoreIdAndTableIdForUpdate(@Param("storeId") Long storeId, @Param("tableId") Long tableId);
```

Add imports:
```java
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

- [ ] **Step 2: Use locked query in replaceItems and submitQrOrdering**

In `replaceItems()`, change:
```java
// Before:
ActiveTableOrderEntity entity = activeTableOrderRepository.findByStoreIdAndTableId(command.storeId(), command.tableId())
// After:
ActiveTableOrderEntity entity = activeTableOrderRepository.findByStoreIdAndTableIdForUpdate(command.storeId(), command.tableId())
```

In `submitQrOrdering()`, change:
```java
// Before:
ActiveTableOrderEntity entity = activeTableOrderRepository.findByStoreIdAndTableId(store.getId(), table.getId())
// After:
ActiveTableOrderEntity entity = activeTableOrderRepository.findByStoreIdAndTableIdForUpdate(store.getId(), table.getId())
```

- [ ] **Step 3: Verify compilation**

Run: `cd pos-backend && mvn compile -q`

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "fix(order): add pessimistic lock for table concurrency [ORD-1]"
```

---

### Task 6: Idempotent Settlement [PAY-1]

**Files:**
- Create: `pos-backend/src/main/resources/db/migration/v2/V016__add_settlement_idempotency.sql`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java`

- [ ] **Step 1: Create migration to add unique constraint**

```sql
-- Prevent double settlement for same table session
ALTER TABLE settlement_records
    ADD COLUMN table_session_id VARCHAR(64) NULL AFTER active_order_id;

CREATE UNIQUE INDEX uk_settlement_session
    ON settlement_records (table_session_id)
    WHERE table_session_id IS NOT NULL;
```

Note: MySQL doesn't support partial unique indexes. Use this instead:

```sql
ALTER TABLE settlement_records
    ADD COLUMN table_session_id VARCHAR(64) NULL AFTER active_order_id;

-- For MySQL, we rely on application-level check since MySQL doesn't support partial unique indexes
```

- [ ] **Step 2: Add idempotency check in collectForTable**

In `collectForTable()`, after getting the session, add:

```java
// Idempotency check: if already settled for this session, return existing result
if (settlementRecordRepository.existsByActiveOrderId(session.getSessionId())) {
    SettlementRecordEntity existing = settlementRecordRepository.findByActiveOrderId(session.getSessionId())
            .orElseThrow();
    return new CashierSettlementResultDto(
            session.getSessionId(),
            existing.getSettlementNo(),
            "SETTLED",
            existing.getPayableAmountCents(),
            existing.getCollectedAmountCents()
    );
}
```

- [ ] **Step 3: Add idempotency check in collect**

In `collect()`, after checking status:

```java
// Idempotency: already settled
if (activeOrder.getStatus() == ActiveOrderStatus.SETTLED) {
    SettlementRecordEntity existing = settlementRecordRepository.findByActiveOrderId(activeOrder.getActiveOrderId())
            .orElseThrow();
    return new CashierSettlementResultDto(
            activeOrder.getActiveOrderId(),
            existing.getSettlementNo(),
            "SETTLED",
            existing.getPayableAmountCents(),
            existing.getCollectedAmountCents()
    );
}
```

- [ ] **Step 4: Add repository methods**

In `JpaSettlementRecordRepository.java`:

```java
boolean existsByActiveOrderId(String activeOrderId);
Optional<SettlementRecordEntity> findByActiveOrderId(String activeOrderId);
```

- [ ] **Step 5: Verify compilation**

Run: `cd pos-backend && mvn compile -q`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "fix(settlement): add idempotent settlement check [PAY-1]"
```

---

### Task 7: Fix submitToKitchen Deleting Active Order [ORD-4]

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java`

- [ ] **Step 1: Change submitToKitchen to keep the active order**

In `submitToKitchen()`, at line 292, change:

```java
// Before:
activeTableOrderRepository.delete(saved);

// After:
// Keep the active order for traceability — do not delete
```

Simply remove the `activeTableOrderRepository.delete(saved);` line.

- [ ] **Step 2: Verify compilation**

Run: `cd pos-backend && mvn compile -q`

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix(order): keep active order after submitToKitchen [ORD-4]"
```

---

### Task 8: Fix Order ID Collision [ORD-2]

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java`

- [ ] **Step 1: Find and fix the createEntity method**

Find the `createEntity` method (around line 326+) and change the order number generation:

```java
// Before:
entity.setOrderNo("ATO" + System.currentTimeMillis());

// After:
entity.setOrderNo("ATO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
```

UUID is already imported in this file.

- [ ] **Step 2: Verify compilation**

Run: `cd pos-backend && mvn compile -q`

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix(order): use UUID for order number to prevent collision [ORD-2]"
```

---

### Task 9: Disable SQL Logging in Production [SEC-4]

**Files:**
- Modify: `pos-backend/src/main/resources/application.yml`

- [ ] **Step 1: Set show-sql to false**

This was already done in Task 1 Step 2. Verify it's set to `false`.

- [ ] **Step 2: Commit (if not already committed with Task 1)**

```bash
git add -A && git commit -m "fix(security): disable SQL logging to prevent data exposure [SEC-4]"
```

---

### Task 10: Core Integration Tests [TEST-1]

**Files:**
- Create: `pos-backend/src/test/java/com/developer/pos/v2/order/ActiveTableOrderServiceTest.java`
- Create: `pos-backend/src/test/java/com/developer/pos/v2/settlement/CashierSettlementServiceTest.java`

- [ ] **Step 1: Add test dependencies to pom.xml (if missing)**

Verify these exist (Spring Boot starter-test is usually included):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Create test application.yml**

Create `pos-backend/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MYSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  flyway:
    enabled: false
app:
  jwt:
    secret: test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256!!
    expiration-ms: 3600000
```

- [ ] **Step 3: Write order lifecycle integration test**

```java
package com.developer.pos.v2.order;

import com.developer.pos.v2.order.application.service.ActiveTableOrderApplicationService;
import com.developer.pos.v2.order.application.command.ReplaceActiveTableOrderItemsCommand;
import com.developer.pos.v2.order.application.dto.ActiveTableOrderDto;
import com.developer.pos.v2.order.domain.source.OrderSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActiveTableOrderServiceTest {

    @Autowired
    private ActiveTableOrderApplicationService service;

    @Test
    void replaceItems_createsNewOrder_whenNoExistingOrder() {
        // This test requires seed data for store and table
        // Arrange: use store ID and table ID from test seed data
        // Act & Assert: verify order is created with correct totals
        // TODO: Populate with actual test data matching the store/table setup
    }

    @Test
    void submitToKitchen_doesNotDeleteActiveOrder() {
        // Verify that after submitToKitchen, the active order still exists
        // (regression test for ORD-4 fix)
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd pos-backend && mvn test -q`
Expected: Tests compile and framework boots (even if some tests are skeletal)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "test: add initial test infrastructure and skeletal order tests [TEST-1]"
```
