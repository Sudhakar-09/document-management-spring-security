SELECT '✅ STARTED USERS INSERT';

INSERT INTO users (
    id, user_id,
    is_account_non_expired, is_account_non_locked, is_enabled, is_mfa_enabled,
    created_at, created_by, updated_at, updated_by
)
VALUES (
    100, 'manual-user',
    true, true, true, false,
    '2025-06-23 06:00:00', 1, '2025-06-23 06:00:00', 1
);

SELECT COUNT(*) FROM users;
