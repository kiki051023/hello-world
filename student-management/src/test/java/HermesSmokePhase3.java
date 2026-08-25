import java.sql.*;
import java.util.*;

/**
 * Phase-3 smoke test against live MySQL80, isolated in student_smoke3 (hermes-smoke-*).
 * Safety: URL pinned to scratch DB; script-level USE/CREATE/DROP DATABASE stripped;
 * hard guard refuses non-scratch targets. Mirrors HermesSmokePhase2 patterns.
 */
public class HermesSmokePhase3 {
    static int pass = 0, fail = 0;
    static List<String> failures = new ArrayList<>();

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (ok || detail.isEmpty() ? "" : "  [" + detail + "]"));
        if (ok) pass++; else { fail++; failures.add(name); }
    }

    static final char BSL = (char) 92, QUO = (char) 39;

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/student_smoke3?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        if (!url.contains("/student_smoke3")) {
            System.out.println("REFUSING TO RUN: target is not the scratch DB");
            System.exit(2);
        }
        String user = System.getProperty("db.user", "root");
        String pwd = System.getProperty("db.pass", "123456");
        Class.forName("com.mysql.cj.jdbc.Driver");

        // bootstrap scratch db
        try (Connection b = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true", user, pwd);
             Statement bs = b.createStatement()) {
            bs.execute("DROP DATABASE IF EXISTS student_smoke3");
            bs.execute("CREATE DATABASE student_smoke3 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        try (Connection c = DriverManager.getConnection(url, user, pwd)) {
            System.out.println("connected as " + user + ", target=student_smoke3");

            runScript(c, "src/main/resources/sql/init.sql");
            runScript(c, "src/main/resources/sql/migrations/phase1_foundation.sql");
            runScript(c, "src/main/resources/sql/migrations/phase2_education_scores.sql");
            try {
                runScript(c, "src/main/resources/sql/migrations/phase3_student_affairs.sql");
                check("phase3 migration executes end-to-end", true, "");
            } catch (SQLException e) {
                check("phase3 migration executes end-to-end", false, e.getMessage());
                done(); return;
            }
            // idempotent rerun
            try {
                runScript(c, "src/main/resources/sql/migrations/phase3_student_affairs.sql");
                check("phase3 migration is idempotent", true, "");
            } catch (SQLException e) {
                check("phase3 migration is idempotent", false, e.getMessage());
            }

            for (String t : new String[]{"student_status_history", "leave_request",
                    "reward_punishment", "hardship_funding", "student_warning"}) {
                check("table created: " + t, tableExists(c, t), "");
            }
            for (String[] t : new String[][]{{"hardship_funding", "uk_funding_once"},
                    {"student_warning", "uk_warning_active"}}) {
                try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() " +
                        "AND table_name='" + t[0] + "' AND index_name='" + t[1] + "'")) {
                    rs.next();
                    check("unique key present: " + t[1], rs.getInt(1) >= 1, "stat_rows=" + rs.getInt(1));
                }
            }

            // ---- seed a student ----
            exec(c, "INSERT INTO sys_user (username, password, role) VALUES ('smk_stu', 'x', 'STUDENT')");
            long uid = lastId(c);
            exec(c, "INSERT INTO student (student_no, name, user_id) VALUES ('SMK3001', '冒烟学生', " + uid + ")");
            long stuId = lastId(c);

            // ---- status history append ----
            exec(c, "INSERT INTO student_status_history (student_id, change_type, old_value, new_value, operator_id)" +
                    " VALUES (" + stuId + ", 'SUSPEND', 'READING', 'SUSPENDED', 1)");
            exec(c, "INSERT INTO student_status_history (student_id, change_type, old_value, new_value, operator_id)" +
                    " VALUES (" + stuId + ", 'RESUME', 'SUSPENDED', 'READING', 1)");
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM student_status_history WHERE student_id=" + stuId)) {
                rs.next();
                check("status history appends both changes", rs.getInt(1) == 2, "count=" + rs.getInt(1));
            }

            // ---- leave workflow states ----
            exec(c, "INSERT INTO leave_request (student_id, leave_type, start_time, end_time, status)" +
                    " VALUES (" + stuId + ", 'SICK', '2026-01-10 08:00:00', '2026-01-12 18:00:00', 'PENDING')");
            long leaveId = lastId(c);
            exec(c, "UPDATE leave_request SET status='APPROVED', approved_at=NOW() WHERE id=" + leaveId);
            exec(c, "UPDATE leave_request SET status='RETURNED', returned_at=NOW() WHERE id=" + leaveId);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT status FROM leave_request WHERE id=" + leaveId)) {
                rs.next();
                check("leave workflow PENDING->APPROVED->RETURNED", "RETURNED".equals(rs.getString(1)), rs.getString(1));
            }

            // ---- reward / punishment revoke ----
            exec(c, "INSERT INTO reward_punishment (student_id, rp_type, level_code, title, revoked)" +
                    " VALUES (" + stuId + ", 'PUNISHMENT', '警告', '测试处分', 0)");
            long rpId = lastId(c);
            exec(c, "INSERT INTO reward_punishment (student_id, rp_type, level_code, title, revoked)" +
                    " VALUES (" + stuId + ", 'REWARD', '校级', '三好学生', 0)");
            exec(c, "UPDATE reward_punishment SET revoked=1, revoke_date='2026-02-01' WHERE id=" + rpId);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM reward_punishment WHERE student_id=" + stuId + " AND rp_type='PUNISHMENT' AND revoked=1")) {
                rs.next();
                check("punishment revocable without deletion", rs.getInt(1) == 1, "");
            }

            // ---- funding uniqueness + sensitive column ----
            exec(c, "INSERT INTO hardship_funding (student_id, academic_year, funding_type, amount, status, sensitive_note)" +
                    " VALUES (" + stuId + ", '2025-2026', 'GRANT', 3000.00, 'PENDING', '家庭变故')");
            try {
                exec(c, "INSERT INTO hardship_funding (student_id, academic_year, funding_type, status)" +
                        " VALUES (" + stuId + ", '2025-2026', 'GRANT', 'PENDING')");
                check("uk_funding_once blocks duplicate year+type", false, "dup insert succeeded");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_funding_once blocks duplicate year+type", true, "");
            }
            exec(c, "UPDATE hardship_funding SET sensitive_note=NULL WHERE student_id=" + stuId);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT sensitive_note FROM hardship_funding WHERE student_id=" + stuId)) {
                rs.next();
                check("sensitive_note nullable (masking path)", rs.getString(1) == null, "");
            }

            // ---- warning idempotency via uk_warning_active ----
            exec(c, "INSERT INTO student_warning (student_id, warning_type, rule_key, rule_threshold, current_value, status)" +
                    " VALUES (" + stuId + ", 'SCORE', 'score_fail_3', 3, 4, 'ACTIVE')");
            try {
                exec(c, "INSERT INTO student_warning (student_id, warning_type, rule_key, rule_threshold, current_value, status)" +
                        " VALUES (" + stuId + ", 'SCORE', 'score_fail_3', 3, 4, 'ACTIVE')");
                check("uk_warning_active blocks duplicate ACTIVE", false, "dup insert succeeded");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_warning_active blocks duplicate ACTIVE", true, "");
            }
            // same key but HANDLED must coexist with ACTIVE
            exec(c, "INSERT INTO student_warning (student_id, warning_type, rule_key, rule_threshold, current_value, status)" +
                    " VALUES (" + stuId + ", 'SCORE', 'score_fail_3', 3, 4, 'HANDLED')");
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM student_warning WHERE student_id=" + stuId)) {
                rs.next();
                check("HANDLED coexists with ACTIVE", rs.getInt(1) == 2, "count=" + rs.getInt(1));
            }

            exec(c, "DROP DATABASE IF EXISTS student_smoke3");
            done();
        }
    }

    static void done() {
        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0) { System.out.println("FAILED: " + failures); System.exit(1); }
    }

    static boolean tableExists(Connection c, String t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
            ps.setString(1, t);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    static void exec(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }

    static long lastId(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    static void runScript(Connection c, String path) throws Exception {
        List<String> stmts = splitScript(new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(path)), java.nio.charset.StandardCharsets.UTF_8));
        try (Statement s = c.createStatement()) {
            for (int i = 0; i < stmts.size(); i++) {
                String t = stmts.get(i).trim();
                if (t.isEmpty()) continue;
                String head = t.toUpperCase();
                if (head.startsWith("USE ") || head.startsWith("CREATE DATABASE")
                        || head.startsWith("DROP DATABASE")) continue;
                try { s.execute(t); }
                catch (SQLException e) {
                    String h = t.substring(0, Math.min(160, t.length())).replace('\n', ' ');
                    throw new SQLException("script=" + path + " stmt#" + i + " head=[" + h + "] cause: " + e.getMessage(), e);
                }
            }
        }
    }

    static List<String> splitScript(String raw) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder(), procBody = new StringBuilder();
        boolean delimMode = false, inString = false;
        int parenDepth = 0;
        for (String line : raw.split("\r?\n")) {
            String l = line.trim();
            if (l.startsWith("--") || l.isEmpty()) continue;
            String u = l.toUpperCase();
            if (u.startsWith("DELIMITER $$")) { delimMode = true; continue; }
            if (u.equals("DELIMITER ;")) {
                delimMode = false;
                if (procBody.length() > 0) { out.add(procBody.toString()); procBody.setLength(0); }
                continue;
            }
            if (delimMode) {
                procBody.append(line).append('\n');
                if (l.endsWith("$$")) {
                    String body = procBody.toString();
                    body = body.substring(0, body.lastIndexOf("$$"));
                    out.add(body);
                    procBody.setLength(0);
                }
                continue;
            }
            boolean esc = false;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (esc) { esc = false; continue; }
                if (ch == BSL) { esc = true; continue; }
                if (ch == QUO) inString = !inString;
                else if (!inString && ch == '(') parenDepth++;
                else if (!inString && ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            }
            cur.append(line).append('\n');
            if (l.endsWith(";") && parenDepth == 0 && !inString) { out.add(cur.toString()); cur.setLength(0); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
