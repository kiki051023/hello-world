import java.sql.*;
import java.util.*;

/**
 * Phase-2 smoke test against the live MySQL80 instance (hermes-smoke-*).
 * Steps: connect -> baseline -> phase1+phase2 migrations on scratch DB
 *        -> seed minimal data -> exercise lifecycle SQL paths.
 * Read-only toward student_db; all DDL/DML happens in student_smoke2.
 */
public class HermesSmokePhase2 {
    static int pass = 0, fail = 0;
    static List<String> failures = new ArrayList<>();

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (ok || detail.isEmpty() ? "" : "  [" + detail + "]"));
        if (ok) pass++; else { fail++; failures.add(name); }
    }

    public static void main(String[] args) throws Exception {
        // SAFETY: pin every connection to the scratch DB. Scripts' own USE/CREATE DATABASE
        // statements are filtered out in runScript(), so student_db is unreachable.
        String url = "jdbc:mysql://localhost:3306/student_smoke2?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        if (!url.contains("/student_smoke2")) {
            System.out.println("REFUSING TO RUN: target database is not the scratch DB");
            System.exit(2);
        }
        String user = System.getProperty("db.user", "root");
        String pass1 = System.getProperty("db.pass", "123456");
        Class.forName("com.mysql.cj.jdbc.Driver");

        // bootstrap: create the scratch DB first (URL has no database yet)
        try (Connection boot = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=false", user, pass1);
             Statement bs = boot.createStatement()) {
            bs.execute("DROP DATABASE IF EXISTS student_smoke2");
            bs.execute("CREATE DATABASE student_smoke2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        try (Connection c = DriverManager.getConnection(url, user, pass1)) {
            System.out.println("connected as " + user + ", server " + c.getMetaData().getDatabaseProductVersion());

            // ---- baseline: production DB untouched ----
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='student_db'")) {
                rs.next();
                check("baseline: student_db exists", rs.getInt(1) == 1, "");
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='student_db' AND table_name='score'")) {
                rs.next();
                check("baseline: student_db.score exists", rs.getInt(1) == 1, "");
            }

            // ---- scratch database (already recreated during bootstrap) ----
            runScript(c, "src/main/resources/sql/init.sql", null);
            runScript(c, "src/main/resources/sql/migrations/phase1_foundation.sql", null);
            try {
                runScript(c, "src/main/resources/sql/migrations/phase2_education_scores.sql", null);
                check("phase2 migration executes end-to-end", true, "");
            } catch (SQLException e) {
                check("phase2 migration executes end-to-end", false, e.getMessage());
                printResult(e); return;
            }

            // idempotency: run phase2 again
            try {
                runScript(c, "src/main/resources/sql/migrations/phase2_education_scores.sql", null);
                check("phase2 migration is idempotent", true, "");
            } catch (SQLException e) {
                check("phase2 migration is idempotent", false, e.getMessage());
            }

            use(c);

            // ---- verify schema objects ----
            for (String t : new String[]{"course_term","teaching_task","training_plan",
                    "training_plan_course","course_selection","score_change_log"}) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?");
                     ) {
                    ps.setString(1, t);
                    ResultSet rs = ps.executeQuery(); rs.next();
                    check("table created: " + t, rs.getInt(1) == 1, "");
                }
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
                    "AND table_name='score' AND column_name IN ('course_term_id','score_status','score_type','active','original_score_id','version_no','version','active_uk')")) {
                rs.next();
                check("score has all 8 lifecycle columns", rs.getInt(1) == 8, "found=" + rs.getInt(1));
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema=DATABASE() " +
                    "AND table_name='score' AND index_name IN ('uk_score_active')")) {
                rs.next();
                check("uk_score_active unique key present", rs.getInt(1) == 1, "");
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() " +
                    "AND table_name='score' AND index_name='uk_student_course'")) {
                rs.next();
                check("legacy uk_student_course dropped", rs.getInt(1) == 0, "");
            }

            // ---- legacy rows default to APPROVED/active=1 ----
            exec(c, "INSERT INTO clazz (clazz_name, grade) VALUES ('冒烟一班', '2023')");
            long clazzId = lastId(c);
            exec(c, "INSERT INTO course (course_no, course_name, credit) VALUES ('SMK-C001', '冒烟数学', 4)");
            long courseId = lastId(c);
            exec(c, "INSERT INTO sys_user (username, password, role) VALUES ('smoke_stu1', 'x', 'STUDENT')");
            long userId = lastId(c);
            exec(c, "INSERT INTO student (student_no, name, clazz_id, user_id) VALUES ('SMK2023001', '冒烟张三', " + clazzId + ", " + userId + ")");
            long stuId = lastId(c);
            exec(c, "INSERT INTO score (student_id, course_id, score, exam_time) VALUES (" + stuId + ", " + courseId + ", 88.5, '2024-06-20')");
            long legacyScoreId = lastId(c);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT score_status, score_type, active FROM score WHERE student_id=" + stuId)) {
                rs.next();
                check("legacy row defaults APPROVED/REGULAR/active=1",
                        "APPROVED".equals(rs.getString(1)) && "REGULAR".equals(rs.getString(2)) && rs.getInt(3) == 1,
                        rs.getString(1) + "/" + rs.getString(2) + "/" + rs.getInt(3));
            }

            // ---- lifecycle transitions via real SQL ----
            exec(c, "INSERT INTO academic_term (academic_year, term_no, term_name, status) VALUES ('2024-2025', 1, '2024-2025第一学期', 1)");
            long termId = lastId(c);
            exec(c, "INSERT INTO course_term (term_id, course_id, capacity, status) VALUES (" + termId + ", " + courseId + ", 30, 1)");
            long ctId = lastId(c);
            // draft
            exec(c, "INSERT INTO score (student_id, course_id, score, exam_time, course_term_id, score_status, score_type, active, version_no, version)" +
                    " VALUES (" + stuId + ", " + courseId + ", 55, '2024-06-20', " + ctId + ", 'DRAFT', 'REGULAR', 0, 1, 0)");
            long draftId = lastId(c);
            // submit -> approve（镜像 ScoreLifecycleServiceImpl.approve：先切换旧 active 再上线新记录）
            exec(c, "UPDATE score SET score_status='SUBMITTED', version=version+1 WHERE id=" + draftId);
            exec(c, "UPDATE score SET active=0 WHERE student_id=" + stuId + " AND course_id=" + courseId
                    + " AND score_type='REGULAR' AND active=1 AND id<>" + draftId);
            exec(c, "UPDATE score SET score_status='APPROVED', active=1, version=version+1 WHERE id=" + draftId);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM score WHERE student_id=" + stuId + " AND course_id=" + courseId +
                    " AND score_type='REGULAR' AND active=1")) {
                rs.next();
                check("active uniqueness allows single APPROVED", rs.getInt(1) == 1, "");
            }
            // second APPROVED same slot must violate uk_score_active
            try (Statement s = c.createStatement()) {
                s.execute("INSERT INTO score (student_id, course_id, score, course_term_id, score_status, score_type, active)" +
                        " VALUES (" + stuId + ", " + courseId + ", 90, " + ctId + ", 'APPROVED', 'REGULAR', 1)");
                check("uk_score_active blocks duplicate active record", false, "insert unexpectedly succeeded");
            } catch (SQLIntegrityConstraintViolationException e) {
                check("uk_score_active blocks duplicate active record", true, "");
            }
            // makeup as independent record
            exec(c, "INSERT INTO score (student_id, course_id, score, exam_time, course_term_id, original_score_id, score_status, score_type, active)" +
                    " VALUES (" + stuId + ", " + courseId + ", 75, '2024-07-01', " + ctId + ", " + draftId + ", 'SUBMITTED', 'MAKEUP', 0)");
            long makeupId = lastId(c);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM score WHERE original_score_id=" + draftId + " AND score_type='MAKEUP'")) {
                rs.next();
                check("makeup stored as independent record linked to original", rs.getInt(1) == 1, "");
            }
            // approve makeup -> two actives across DIFFERENT types must coexist
            exec(c, "UPDATE score SET score_status='APPROVED', active=1 WHERE id=" + makeupId);
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM score WHERE student_id=" + stuId + " AND active=1")) {
                rs.next();
                check("REGULAR and MAKEUP can both be active", rs.getInt(1) == 2, "count=" + rs.getInt(1));
            }
            // change log append
            exec(c, "INSERT INTO score_change_log (score_id, change_type, old_score, new_score, operator_name)" +
                    " VALUES (" + draftId + ", 'CHANGE', 55, 60, 'smoke')");
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM score_change_log WHERE score_id=" + draftId)) {
                rs.next();
                check("change log appended", rs.getInt(1) >= 1, "");
            }
            // credit aggregation query (same shape as selectStudentCreditSummary)
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COALESCE(SUM(CASE WHEN t.best_score >= 60 THEN cr.credit ELSE 0 END),0) AS earned " +
                    "FROM (SELECT sc.course_id, MAX(sc.score) AS best_score FROM score sc WHERE sc.deleted=0 " +
                    "AND sc.active=1 AND sc.score_status='APPROVED' AND sc.student_id=" + stuId + " GROUP BY sc.course_id) t " +
                    "JOIN course cr ON cr.id=t.course_id AND cr.deleted=0")) {
                rs.next();
                check("credit aggregation returns 4 (makeup 75 passed)", rs.getBigDecimal(1).intValue() == 4,
                        "earned=" + rs.getBigDecimal(1));
            }

            // cleanup scratch db only; student_db never touched
            exec(c, "DROP DATABASE IF EXISTS student_smoke2");
            System.out.println();
            System.out.println(pass + " passed, " + fail + " failed");
            if (fail > 0) { System.out.println("FAILED: " + failures); System.exit(1); }
        }
    }

    static void use(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute("USE student_smoke2"); }
    }

    static void exec(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS); }
    }

    static long lastId(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next(); return rs.getLong(1);
        }
    }

    /** Executes a semicolon-separated SQL script with naive DELIMITER handling. */
    static void runScript(Connection c, String path, String switchDb) throws Exception {
        List<String> stmts = splitScript(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)),
                java.nio.charset.StandardCharsets.UTF_8));
        if (switchDb != null) {
            try (Statement s = c.createStatement()) { s.execute("USE " + switchDb); }
        }
        try (Statement s = c.createStatement()) {
            for (int i = 0; i < stmts.size(); i++) {
                String t = stmts.get(i).trim();
                if (t.isEmpty()) continue;
                // SAFETY: never let a script switch databases or create foreign schemas
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
        StringBuilder cur = new StringBuilder();
        StringBuilder procBody = new StringBuilder();
        boolean delimMode = false;
        int parenDepth = 0;
        boolean inString = false;
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
                    // strip the custom delimiter before handing to JDBC
                    String body = procBody.toString();
                    body = body.substring(0, body.lastIndexOf("$$"));
                    out.add(body);
                    procBody.setLength(0);
                }
                continue;
            }
            // track parens and single-quote strings so multi-line VALUES tuples stay together
            boolean escaped = false;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (escaped) { escaped = false; continue; }
                if (ch == '\\') { escaped = true; continue; }
                if (ch == '\'') inString = !inString;
                else if (!inString && ch == '(') parenDepth++;
                else if (!inString && ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            }
            cur.append(line).append('\n');
            boolean endsStatement = l.endsWith(";") && parenDepth == 0 && !inString;
            if (endsStatement) { out.add(cur.toString()); cur.setLength(0); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    static void printResult(SQLException e) {
        System.out.println("SQLException: " + e.getMessage());
    }
}
