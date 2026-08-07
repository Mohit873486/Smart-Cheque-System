-- ============================================================
-- ChequePro Performance Indexes v5.0
-- Adds 15+ strategic indexes for dashboard & query optimization
-- Run: mysql -u root -p chequeprint_db < V5__add_performance_indexes.sql
-- ============================================================

USE chequeprint_db;

-- Drop existing duplicate indexes (if any) to avoid clutter
-- MySQL automatically creates indexes on PK, UK, FK - we skip those

-- ============================================================
-- 1. USERS TABLE - Login & Dashboard User Stats
-- ============================================================

-- Dashboard: Count users by role + status (Admin/Manager/Active counts)
CREATE INDEX idx_users_role_status ON users(role, status);

-- Dashboard: User registration trends (last 30 days)
CREATE INDEX idx_users_created_at ON users(created_at);

-- Security: Failed login monitoring, account lock checks
CREATE INDEX idx_users_security ON users(failed_login_attempts, locked_until, status);

-- Login search: Fast lookup by username OR email (existing idx_users_identity covers this partially)
-- But adding a covering index for active users:
CREATE INDEX idx_users_login_lookup ON users(username, email, status, password);

-- ============================================================
-- 2. CHEQUES TABLE - Main Dashboard & Reports (MOST CRITICAL)
-- ============================================================

-- Dashboard: Status breakdown pie chart (Pending, Approved, Printed, etc.)
-- Composite index: status first (high cardinality filter), then date for sorting
CREATE INDEX idx_cheques_status_date ON cheques(status, issue_date);

-- Dashboard: Bank-wise cheque distribution + status filtering
CREATE INDEX idx_cheques_bank_status ON cheques(bank_id, status, issue_date);

-- Dashboard: Recent cheques list + amount trends
CREATE INDEX idx_cheques_created_sort ON cheques(created_at DESC);

-- Search: Payee name lookup (case-insensitive search support)
CREATE INDEX idx_cheques_payee ON cheques(payee_name);

-- Dashboard: Monthly/weekly amount aggregation for charts
CREATE INDEX idx_cheques_date_amount ON cheques(issue_date, amount);

-- Print workflow: Find cheques ready to print (Approved status, not printed)
CREATE INDEX idx_cheques_print_queue ON cheques(status, issue_date, payee_name, amount);

-- Audit: Cheque lifecycle tracking by account
CREATE INDEX idx_cheques_account_status ON cheques(account_id, status, updated_at);

-- ============================================================
-- 3. INVOICES TABLE - Financial Dashboard
-- ============================================================

-- Dashboard: Invoice status summary (Unpaid/Paid/Partial)
CREATE INDEX idx_invoices_status_date ON invoices(status, issue_date);

-- Dashboard: Overdue invoice alerts (due_date < today AND status != 'Paid')
CREATE INDEX idx_invoices_overdue ON invoices(due_date, status);

-- Search: Client name lookup
CREATE INDEX idx_invoices_client ON invoices(client_name);

-- Dashboard: Monthly revenue trends
CREATE INDEX idx_invoices_date_amount ON invoices(issue_date, amount, status);

-- ============================================================
-- 4. AUDIT_LOG TABLE - Activity Feed & Compliance (HEAVILY QUERIED)
-- ============================================================

-- Dashboard: Recent activity feed (latest 50 actions)
CREATE INDEX idx_audit_recent ON audit_log(created_at DESC, action);

-- Dashboard: Action-type filtering (e.g., show only PRINT, APPROVE)
CREATE INDEX idx_audit_action_date ON audit_log(action, created_at DESC);

-- Record history: Lookup all actions for a specific record
CREATE INDEX idx_audit_record ON audit_log(table_name, record_id, created_at DESC);

-- User profile: Activity timeline for specific user
CREATE INDEX idx_audit_user_timeline ON audit_log(user_id, created_at DESC);

-- Dashboard: Daily activity count for charts
CREATE INDEX idx_audit_daily_stats ON audit_log(created_at, action);

-- ============================================================
-- 5. NOTIFICATIONS TABLE - Real-time Bell Icon Count
-- ============================================================

-- CRITICAL: Unread notification count (shows in header bell icon)
CREATE INDEX idx_notifications_unread ON notifications(user_id, status, created_at DESC);

-- Notification feed: All notifications sorted by time
CREATE INDEX idx_notifications_feed ON notifications(user_id, created_at DESC);

-- Type filtering: Show only APPROVAL or REMINDER
CREATE INDEX idx_notifications_type ON notifications(user_id, type, status);

-- ============================================================
-- 6. BANK_ACCOUNT TABLE - Dropdown & Search
-- ============================================================

-- Bank dropdown population + search
CREATE INDEX idx_bank_account_name ON bank_account(bank_name, account_holder_name);

-- IFSC search for validation
CREATE INDEX idx_bank_account_ifsc ON bank_account(ifsc);

-- ============================================================
-- 7. CHEQUE_TEMPLATE & TEMPLATE_FIELD - Template Designer
-- ============================================================

-- Template lookup by bank
CREATE INDEX idx_template_bank ON cheque_template(bank_id, template_name);

-- Field lookup for rendering
CREATE INDEX idx_template_field_lookup ON template_field(template_id, field_name);

-- ============================================================
-- 8. REMINDERS TABLE - Upcoming Tasks Panel
-- ============================================================

-- Dashboard: Upcoming reminders (Pending + remind_at >= now)
CREATE INDEX idx_reminders_upcoming ON reminders(user_id, status, remind_at);

-- ============================================================
-- 9. PASSWORD_RESET_OTPS - Security Cleanup
-- ============================================================

-- Cleanup job: Find expired/unused OTPs
CREATE INDEX idx_otp_cleanup ON password_reset_otps(expires_at, used_at);

-- ============================================================
-- 10. FULLTEXT INDEXES - Search Optimization (MySQL 5.6+)
-- ============================================================

-- Fulltext search on cheque payee names
CREATE FULLTEXT INDEX idx_cheques_payee_ft ON cheques(payee_name);

-- Fulltext search on audit log details
CREATE FULLTEXT INDEX idx_audit_details_ft ON audit_log(details);

-- Fulltext search on invoice client names + notes
CREATE FULLTEXT INDEX idx_invoices_search_ft ON invoices(client_name, notes);

-- ============================================================
-- INDEX VERIFICATION & STATS
-- ============================================================

-- Verify all indexes created
SELECT 
    table_name,
    index_name,
    column_name,
    cardinality
FROM information_schema.statistics
WHERE table_schema = 'chequeprint_db'
ORDER BY table_name, index_name, seq_in_index;

-- Show index usage stats (run after 24 hours of usage)
-- SELECT * FROM performance_schema.table_io_waits_summary_by_index_usage 
-- WHERE OBJECT_SCHEMA = 'chequeprint_db';