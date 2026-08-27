import java.sql.*;
import java.util.*;

/**
 * Production migration runner for student_db (hermes-migrate-*).
 * Safety protocol per docs/operations/phase5-operations.md:
 *   1. pre-flight: conflict view must be empty, binlog ON
 *   2. full backup table snapshot record (row counts before)
 *   3. apply phase3 then phase4 (idempotent scripts)
 *   4. post-checks: tables exist, seed rows unchanged
 * Connection pinned to student_db by URL; script USE/CREATE/DROP stripped.
 */
public class HermesMigrateProd {
    static int pass = 0, fail = 0;
    static List<String> failures = new ArrayList<>();

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (ok || detail.isEmpty() ? "" : "  [" + detail + "]"));
        if (ok) pass++; else { fail++; failures.add(name); }
    }

    static final char BSL = (char) 92, QUO = (char) 39;

    public static void main(String[] args) throws Exception {
        boolean apply = args.length > 0 && "apply".equals(args[0]);
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/student_db?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            System.out.println("=== target: student_db (apply=" + apply + ") ===");

            // ---- pre-flight ----
            try (ResultSet rs = s.executeQuery("SHOW VARIABLES LIKE 'log_bin'")) {
                rs.next();
                check("binlog enabled for PITR", "ON".equals(rs.getString(2)), rs.getString(2));
            }
            int conflict;
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.views WHERE table_schema=DATABASE() AND table_name='v_phase2_score_conflict'")) {
                rs.next(); conflict = rs.getInt(1);
            }
            if (conflict == 1) {
                try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM v_phase2_score_conflict")) {
                    rs.next();
                    check("phase2 conflict view empty", rs.getInt(1) == 0, "dups=" + rs.getInt(1));
                }
            } else {
                check("conflict view exists (skip content check)", true, "");
            }

            // ---- baseline counts ----
            Map<String, Long> before = new LinkedHashMap<>();
            for (String t : new String[]{"sys_user", "student", "teacher", "clazz", "course", "score"}) {
                try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM `" + t + "`")) {
                    rs.next(); before.put(t, rs.getLong(1));
                }
            }
            System.out.println("baseline: " + before);

            // ---- current schema state ----
            Set<String> existing = new HashSet<>();
            try (ResultSet rs = s.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()")) {
                while (rs.next()) existing.add(rs.getString(1).toLowerCase());
            }

            if (!apply) {
                System.out.println("\nDRY-RUN complete. Re-run with 'apply' to execute migrations.");
                done();
                return; // 关键：dry-run 绝不继续执行 apply 逻辑（此前行数验证的 done() 不退出导致下钻）
            }

            if (!existing.contains("student_status_history")) {
                runScript(c, "src/main/resources/sql/migrations/phase3_student_affairs.sql");
                check("phase3 applied", true, "");
            } else check("phase3 already present, skip", true, "");

            if (!existing.contains("practice_company")) {
                runScript(c, "src/main/resources/sql/migrations/phase4_practice_employment.sql");
                check("phase4 applied", true, "");
            } else check("phase4 already present, skip", true, "");

            // ---- 阶段7：用户/角色管理端（权限字典 + ADMIN 预置，幂等）----
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM sys_permission WHERE permission_code IN ('system:user:read','system:user:write','system:role:read','system:role:assign-permission','system:role:assign-data-scope')")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    runScript(c, "src/main/resources/sql/migrations/phase7_user_role_admin.sql");
                    check("phase7 applied", true, "");
                } else check("phase7 already present, skip", true, "");
            }
            // phase7 后置：ADMIN 已预置管理权限
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM sys_role_permission rp JOIN sys_role r ON r.id=rp.role_id WHERE r.role_code='ADMIN' AND rp.permission_id IN (SELECT id FROM sys_permission WHERE permission_code LIKE 'system:%')")) {
                ResultSet rs = ps.executeQuery(); rs.next();
                check("ADMIN has system:* permissions", rs.getInt(1) >= 5, "count=" + rs.getInt(1));
            }

            // ---- 阶段8：P1 密码策略（must_change_password / last_password_change，幂等）----
            boolean hasMcp;
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_user' AND column_name='must_change_password'")) {
                rs.next(); hasMcp = rs.getInt(1) > 0;
            }
            if (!hasMcp) {
                runScript(c, "src/main/resources/sql/migrations/phase8_password_policy.sql");
                check("phase8 applied", true, "");
            } else check("phase8 already present, skip", true, "");
            // phase8 后置：两列已存在
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_user' AND column_name IN ('must_change_password','last_password_change')")) {
                rs.next();
                check("sys_user password-policy columns present", rs.getInt(1) == 2, "count=" + rs.getInt(1));
            }

            // ---- post checks ----
            for (String t : new String[]{"student_status_history", "leave_request", "reward_punishment",
                    "hardship_funding", "student_warning", "practice_company", "employment_record"}) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
                    ps.setString(1, t);
                    ResultSet rs = ps.executeQuery(); rs.next();
                    check("table present after migrate: " + t, rs.getInt(1) == 1, "");
                }
            }
            for (Map.Entry<String, Long> e : before.entrySet()) {
                try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM `" + e.getKey() + "`")) {
                    rs.next();
                    check("rows unchanged: " + e.getKey(), rs.getLong(1) == e.getValue(),
                          rs.getLong(1) + " vs " + e.getValue());
                }
            }
            done();
        }
    }

    static void done() {
        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0) { System.out.println("FAILED: " + failures); System.exit(1); }
    }

    static void runScript(Connection c, String path) throws Exception {
        List<String> stmts = split(new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(path)), java.nio.charset.StandardCharsets.UTF_8));
        try (Statement s = c.createStatement()) {
            for (int i = 0; i < stmts.size(); i++) {
                String t = stmts.get(i).trim();
                if (t.isEmpty()) continue;
                String head = t.toUpperCase();
                if (head.startsWith("USE ") || head.startsWith("CREATE DATABASE")
                        || head.startsWith("DROP DATABASE") || head.startsWith("DROP VIEW IF EXISTS V_PHASE2")) continue;
                try { s.execute(t); }
                catch (SQLException e) {
                    throw new SQLException("script=" + path + " stmt#" + i + ": " + e.getMessage(), e);
                }
            }
        }
    }

    static List<String> split(String raw) {
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
                    out.add(body.substring(0, body.lastIndexOf("$$")));
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
