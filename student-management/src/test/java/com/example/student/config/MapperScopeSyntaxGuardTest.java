package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防回归守卫：MyBatis 只在 &lt;if test&gt; / &lt;foreach&gt; / ${} 属性里做 OGNL 求值，
 * SQL 正文里"裸写"的 scope.xxx 会被原样拼进 SQL（对非 admin 角色必然 500）。
 * 这条规则在 2026-08-27 安全矩阵中真实击穿（学生/教师/辅导员查列表全 500），
 * 此测试确保任何人为改 mapper 后不再引入裸 scope.xxx。
 */
class MapperScopeSyntaxGuardTest {

    /** SQL 正文里的裸 scope.selfOnly / scope.departmentId / scope.hasClassIds 等 */
    private static final Pattern RAW_SCOPE = Pattern.compile(
            "\\bscope\\.(selfOnly|userId|departmentId|hasClassIds|hasCourseIds|teacherId|classIds|courseIds)\\b");

    /** 抹掉 test="..." / collection="..." 属性值 与 #{...} / ${...} 占位符，剩下的 scope. 才是裸引用 */
    private static String cleanSqlText(String line) {
        String cleaned = line
                .replaceAll("(test|collection)=\"[^\"]*\"", "")
                .replaceAll("#\\{[^}]*\\}", "")
                .replaceAll("\\$\\{[^}]*\\}", "");
        return cleaned;
    }

    @Test
    void mapperXmlMustNotContainRawScopeTextInSqlBody() throws IOException {
        Path mapperDir = Paths.get("src/main/resources/mapper");
        assertTrue(Files.isDirectory(mapperDir), "mapper 目录应存在: " + mapperDir.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.list(mapperDir)) {
            for (Path xml : paths.filter(p -> p.toString().endsWith(".xml")).toList()) {
                List<String> lines = Files.readAllLines(xml);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    // 跳过纯属性行（<if test=...> / <foreach collection=...> / <include refid=...>）
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("<if ") || trimmed.startsWith("<foreach ")
                            || trimmed.startsWith("<include ") || trimmed.startsWith("</")) {
                        continue;
                    }
                    // 抹掉 test="..." / collection="..." + #{...} / ${...} 后不应再出现裸 scope.
                    String cleaned = cleanSqlText(line);
                    Matcher m = RAW_SCOPE.matcher(cleaned);
                    if (m.find()) {
                        violations.add(xml.getFileName() + ":" + (i + 1) + " -> " + trimmed);
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "mapper XML 正文存在裸 scope.xxx（会原样拼入 SQL 致非 admin 角色 500），应改为 <if test=\"scope.xxx...\"> 动态分支：\n"
                        + String.join("\n", violations));
    }

    /** Java 注解版 mapper（@Select("""...""") 里的 <script> 同样受该规则约束） */
    @Test
    void javaAnnotationMappersMustNotContainRawScopeText() throws IOException {
        Path mapperPackage = Paths.get("src/main/java/com/example/student/mapper");
        assertTrue(Files.isDirectory(mapperPackage), "mapper 包应存在: " + mapperPackage.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.list(mapperPackage)) {
            for (Path java : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(java);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    // 抹掉 test="..." / collection="..." + #{...} / ${...} 后不应再出现裸 scope.
                    String cleaned = cleanSqlText(line);
                    Matcher m = RAW_SCOPE.matcher(cleaned);
                    if (m.find()) {
                        violations.add(java.getFileName() + ":" + (i + 1) + " -> " + line.trim());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Java 注解 mapper 存在裸 scope.xxx（会原样拼入 SQL 致非 admin 角色 500）：\n"
                        + String.join("\n", violations));
    }
}