-- V103: Migrate legacy auth_users and staff to unified users table
-- Idempotent: uses INSERT IGNORE to skip already-migrated rows
-- Strategy:
--   1. auth_users → users (with password_hash, username)
--   2. staff → users (with pin_hash, staff_code as user_code)
--   3. Assign roles via user_roles
--   4. Set up user_store_access

-- ============================================================
-- 1. Migrate auth_users → users
-- ============================================================
-- user_code = CONCAT('AU-', auth_users.id) to guarantee uniqueness
-- merchant_id: auth_users allows NULL, users requires NOT NULL — use 0 as placeholder

INSERT IGNORE INTO users (user_code, merchant_id, display_name, username, password_hash, must_change_password, user_status, created_at, updated_at)
SELECT
    CONCAT('AU-', a.id),
    COALESCE(a.merchant_id, 0),
    COALESCE(a.display_name, a.username),
    a.username,
    a.password_hash,
    a.must_change_password,
    a.status,
    a.created_at,
    a.updated_at
FROM auth_users a;

-- ============================================================
-- 2. Migrate staff → users (only those not already migrated via auth_users)
-- ============================================================
-- Match by phone or staff_code to avoid duplicates
-- user_code = staff.staff_code

INSERT IGNORE INTO users (user_code, merchant_id, display_name, phone, pin_hash, must_change_password, user_status, created_at, updated_at)
SELECT
    s.staff_code,
    s.merchant_id,
    s.staff_name,
    s.phone,
    s.pin_hash,
    FALSE,
    s.staff_status,
    s.created_at,
    s.updated_at
FROM staff s
WHERE NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.username COLLATE utf8mb4_unicode_ci = s.staff_code COLLATE utf8mb4_unicode_ci
       OR (u.phone IS NOT NULL AND u.phone COLLATE utf8mb4_unicode_ci = s.phone COLLATE utf8mb4_unicode_ci AND u.merchant_id = s.merchant_id)
);

-- ============================================================
-- 3. Assign roles via user_roles
-- ============================================================

-- auth_users role mapping:
--   PLATFORM_ADMIN → SUPER_ADMIN
--   ADMIN → MERCHANT_OWNER
--   CASHIER (default) → CASHIER
-- Only bind roles for users actually migrated in step 1 (user_code starts with 'AU-')
INSERT IGNORE INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), NULL
FROM users u
JOIN auth_users a ON u.username COLLATE utf8mb4_unicode_ci = a.username COLLATE utf8mb4_unicode_ci
JOIN custom_roles r ON r.merchant_id IS NULL AND r.role_code = CASE
    WHEN a.role = 'PLATFORM_ADMIN' THEN 'SUPER_ADMIN'
    WHEN a.role = 'ADMIN' THEN 'MERCHANT_OWNER'
    ELSE a.role
END
WHERE u.user_code LIKE 'AU-%';

-- staff role mapping: staff.role_code → custom_roles.role_code
INSERT IGNORE INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), NULL
FROM users u
JOIN staff s ON u.user_code COLLATE utf8mb4_unicode_ci = s.staff_code COLLATE utf8mb4_unicode_ci
JOIN custom_roles r ON r.merchant_id IS NULL AND r.role_code COLLATE utf8mb4_unicode_ci = CASE
    WHEN s.role_code = 'MANAGER' THEN 'STORE_MANAGER'
    ELSE s.role_code
END COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- ============================================================
-- 4. Set up user_store_access
-- ============================================================

-- auth_users with store_id
INSERT IGNORE INTO user_store_access (user_id, store_id, access_level, granted_at, granted_by)
SELECT u.id, a.store_id, 'FULL', NOW(), NULL
FROM users u
JOIN auth_users a ON u.username COLLATE utf8mb4_unicode_ci = a.username COLLATE utf8mb4_unicode_ci
WHERE a.store_id IS NOT NULL
  AND u.user_code LIKE 'AU-%';

-- staff with store_id
INSERT IGNORE INTO user_store_access (user_id, store_id, access_level, granted_at, granted_by)
SELECT u.id, s.store_id, 'FULL', NOW(), NULL
FROM users u
JOIN staff s ON u.user_code COLLATE utf8mb4_unicode_ci = s.staff_code COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM user_store_access usa WHERE usa.user_id = u.id AND usa.store_id = s.store_id
);
