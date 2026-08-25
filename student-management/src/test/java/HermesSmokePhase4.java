import java.sql.*;
import java.util.*;

/** Phase-4 smoke in disposable student_smoke4 (hermes-smoke-*). Same safety model. */
public class HermesSmokePhase4 {
    static int pass = 0, fail = 0;
    static List<String> failures = new ArrayList<>();

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (ok || detail.isEmpty() ? "" : "  [" + detail + "]"));
        if (ok) pass++; else { fail++; failures.add(name); }
    }

    static final char BSL = (char) 92, QUO = (char) 39;

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/student_smoke4?useSSL=false&allowPublicKeyRetrieval=true";
        if (!url.contains("/student_smoke4")) { System.out.println("REFUSING"); System.exit(2); }
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection b = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement bs = b.createStatement()) {
            bs.execute("DROP DATABASE IF EXISTS student_smoke4");
            bs.execute("CREATE DATABASE student_smoke4 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection c = DriverManager.getConnection(url, "root", "123456")) {
            runScript(c, "src/main/resources/sql/init.sql");
            runScript(c, "src/main/resources/sql/migrations/phase1_foundation.sql");
            runScript(c, "src/main/resources/sql/migrations/phase2_education_scores.sql");
            runScript(c, "src/main/resources/sql/migrations/phase3_student_affairs.sql");
            try {
                runScript(c, "src/main/resources/sql/migrations/phase4_practice_employment.sql");
                check("phase4 migration executes end-to-end", true, "");
            } catch (SQLException e) {
                check("phase4 migration executes end-to-end", false, e.getMessage());
                done(); return;
            }
            try {
                runScript(c, "src/main/resources/sql/migrations/phase4_practice_employment.sql");
                check("phase4 migration is idempotent", true, "");
            } catch (SQLException e) {
                check("phase4 migration is idempotent", false, e.getMessage());
            }

            for (String t : new String[]{"practice_company", "practice_position", "practice_plan",
                    "practice_assignment", "practice_log", "practice_evaluation",
                    "attachment_object", "employment_record", "student_certificate"}) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
                    ps.setString(1, t);
                    ResultSet rs = ps.executeQuery(); rs.next();
                    check("table created: " + t, rs.getInt(1) == 1, "");
                }
            }

            // assignment uniqueness per plan+student
            exec(c, "INSERT INTO practice_plan (academic_year, plan_name) VALUES ('2025-2026', '冒烟计划')");
            long planId = lastId(c);
            exec(c, "INSERT INTO sys_user (username, password, role) VALUES ('smk4_stu', 'x', 'STUDENT')");
            exec(c, "INSERT INTO student (student_no, name, user_id) SELECT 'SMK4001', '冒烟生', id FROM sys_user WHERE username='smk4_stu'");
            long stuId = lastId(c);
            exec(c, "INSERT INTO practice_company (company_name) VALUES ('冒烟公司')");
            long companyId = lastId(c);

            exec(c, "INSERT INTO practice_assignment (plan_id, student_id, company_id, instructor_teacher_id, status)" +
                    " VALUES (" + planId + ", " + stuId + ", " + companyId + ", 1, 'ACTIVE')");
            long asgId = lastId(c);
            try {
                exec(c, "INSERT INTO practice_assignment (plan_id, student_id, company_id, instructor_teacher_id)" +
                        " VALUES (" + planId + ", " + stuId + ", " + companyId + ", 2)");
                check("uk_practice_assign blocks duplicate plan+student", false, "dup ok");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_practice_assign blocks duplicate plan+student", true, "");
            }

            // weekly log uniqueness + workflow
            exec(c, "INSERT INTO practice_log (assignment_id, week_no, content, status)" +
                    " VALUES (" + asgId + ", 1, '第一周', 'SUBMITTED')");
            try {
                exec(c, "INSERT INTO practice_log (assignment_id, week_no, content) VALUES (" + asgId + ", 1, '重复')");
                check("uk_plog_week blocks duplicate week", false, "dup ok");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_plog_week blocks duplicate week", true, "");
            }
            exec(c, "UPDATE practice_log SET status='COMMENTED', teacher_comment='不错' WHERE assignment_id=" + asgId + " AND week_no=1");

            // evaluation once per type
            exec(c, "INSERT INTO practice_evaluation (assignment_id, evaluator_type, score)" +
                    " VALUES (" + asgId + ", 'TEACHER', 88)");
            try {
                exec(c, "INSERT INTO practice_evaluation (assignment_id, evaluator_type, score)" +
                        " VALUES (" + asgId + ", 'TEACHER', 90)");
                check("uk_peval_once blocks duplicate evaluator", false, "dup ok");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_peval_once blocks duplicate evaluator", true, "");
            }

            // attachment object key unique
            exec(c, "INSERT INTO attachment_object (object_key, original_name, business_type, business_id)" +
                    " VALUES ('smk-key-001', 'a.pdf', 'PRACTICE_LOG', " + asgId + ")");
            try {
                exec(c, "INSERT INTO attachment_object (object_key, original_name, business_type)" +
                        " VALUES ('smk-key-001', 'b.pdf', 'PRACTICE_LOG')");
                check("uk_attachment_key blocks duplicate key", false, "dup ok");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_attachment_key blocks duplicate key", true, "");
            }
            // no physical path column
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
                    "AND table_name='attachment_object' AND column_name LIKE '%path%'")) {
                ResultSet rs = ps.executeQuery(); rs.next();
                check("attachment stores no path-like column", rs.getInt(1) == 0, "");
            }

            // employment year uniqueness + stats SQL shape
            exec(c, "INSERT INTO employment_record (student_id, graduation_year, destination_type, employer_name)" +
                    " VALUES (" + stuId + ", 2026, 'EMPLOYED', '冒烟科技')");
            try {
                exec(c, "INSERT INTO employment_record (student_id, graduation_year, destination_type)" +
                        " VALUES (" + stuId + ", 2026, 'UNEMPLOYED')");
                check("uk_employ_year blocks duplicate graduation record", false, "dup ok");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_employ_year blocks duplicate graduation record", true, "");
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT destination_type, COUNT(*) FROM employment_record WHERE deleted=0 " +
                    "AND graduation_year=2026 GROUP BY destination_type")) {
                rs.next();
                check("destination stats aggregation works", "EMPLOYED".equals(rs.getString(1)) && rs.getInt(2) == 1, "");
            }

            exec(c, "DROP DATABASE IF EXISTS student_smoke4");
            done();
        }
    }

    static void done() {
        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0) { System.out.println("FAILED: " + failures); System.exit(1); }
    }

    static void exec(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }

    static long lastId(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next(); return rs.getLong(1);
        }
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
                        || head.startsWith("DROP DATABASE")) continue;
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
