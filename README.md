# 学生管理系统 (Student Management System)

基于 Spring Boot + Vue 3 的前后端分离学生管理系统，适合课程作业、学习项目和小型培训机构使用。

## 📋 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [数据库设计](#数据库设计)
- [默认账号](#默认账号)
- [接口文档](#接口文档)
- [许可证](#许可证)

---

## 项目简介

学生管理系统是一个全栈 Web 应用，实现了学生信息、班级、课程、成绩、教师等核心业务的全生命周期管理，并扩展至教务、学工、实习就业、组织架构等院校全场景。系统采用前后端分离架构，后端基于 Spring Boot 3 + MyBatis-Plus，前端基于 Vue 3 + Element Plus，支持 JWT 认证（访问令牌 + 刷新令牌 + 黑名单 + 在线会话）、RBAC 多角色权限与数据权限（按学校/系部/班级隔离）。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 后端框架 |
| Spring Security | 6.x | 安全框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| JWT (jjwt) | 0.11.5 | Token 认证 |
| Knife4j | 4.4.0 | API 文档 |
| Lombok | - | 代码简化 |
| BCrypt | - | 密码加密 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| Vue Router | 4.3.x | 路由管理 |
| Pinia | 2.1.x | 状态管理 |
| Element Plus | 2.6.x | UI 组件库 |
| Axios | 1.6.x | HTTP 客户端 |
| Vite | 5.1.x | 构建工具 |
| dayjs | 1.11.x | 日期处理 |

## 功能模块

### 🔐 认证与权限
- 登录 / 登出 / 刷新令牌（JWT 访问+刷新双令牌，黑名单即时吊销）
- 验证码（图片验证码）+ 登录失败锁定 + 密码强度策略 + 首次登录强制改密
- RBAC 多角色（管理员 / 教师 / 学生 / 教务管理员 / 辅导员等）+ 用户与角色管理
- 数据权限：按学校 / 系部 / 班级隔离数据，学生端仅见本人
- 在线用户管理：在线会话列表 + 强制下线

### 👨‍🎓 基础数据管理
- 学生信息增删改查（按姓名、学号、班级筛选，分页，Excel 导入导出）
- 班级 / 课程 / 教师管理
- 组织架构三级树：学校 → 系部 → 专业（删除保护）

### 🎓 教务中心
- 学期管理：新增/编辑/设为当前学期
- 课程开设（学期维度）+ 授课任务分配 + 学生选课
- 培养方案管理

### 📊 成绩管理
- 成绩录入、审核（草稿→提交→审核/退回）、作废（含变更日志全链路）
- 按学生、课程、班级、学期筛选；统计（平均/最高/最低/及格率）
- 成绩分析：班级对比、个人趋势、下钻、四 Tab 视图（ECharts）

### 🏫 学工中心
- 请假申请与审批、资助申请与审批（敏感信息脱敏）
- 奖惩管理、学籍异动管理
- 学业预警：按规则引擎生成（挂科数等），预警处理与通知

### 💼 实习就业
- 实习计划 / 岗位 / 分配 / 日志 / 评价 / 证书
- 就业记录管理

### 🔔 通知中心
- 站内通知 + 待办红点（30s 轮询）
- 业务触发点：请假审批 / 成绩审核 / 资助审批 / 学业预警 / 实习分配

### 🏠 首页看板
- 学生/班级/课程/教师总数统计 + 成绩分布 / 班级均分图 + 快捷入口

### ⚙️ 系统管理
- 用户管理、角色管理、数据权限分配、密码重置
- 操作审计日志（traceId 全链路贯通）、异步导入任务、健康检查
- 平台能力：定时任务调度（预警扫描/学期切换/通知清理/导入回收）、接口限流、OpenAPI v1 外部接口

## 项目结构

```
student-management-system/
├── student-management/          # 后端（Spring Boot）
│   ├── src/main/java/com/example/student/
│   │   ├── StudentApplication.java   # 启动类
│   │   ├── common/                   # 公共组件 (Result, PageResult, DistributedCacheService)
│   │   ├── config/                   # 配置类（含 ScheduledTaskScheduler 动态定时任务）
│   │   ├── controller/               # 控制层 (27个Controller)
│   │   ├── entity/                   # 实体类 (37个实体)
│   │   ├── mapper/                   # 持久层
│   │   ├── service/                  # 业务层
│   │   │   └── impl/                 # 业务实现类
│   │   ├── security/                 # 安全层 (JWT过滤器/Redis令牌/数据权限/验证码)
│   │   ├── audit/                    # 操作审计 + 接口限流切面
│   │   ├── exception/                # 异常处理
│   │   └── utils/                    # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml           # 配置文件
│   │   ├── mapper/                   # MyBatis XML
│   │   └── sql/
│   │       ├── init.sql              # 数据库初始化脚本
│   │       └── migrations/           # phase1~phase10 版本化迁移（各配套结构测试）
│   └── pom.xml
│
└── student-frontend/              # 前端（Vue 3）
    ├── src/
    │   ├── api/                      # API 接口 (16个模块)
    │   ├── layout/                   # 布局组件（暗色古典学院风）
    │   ├── router/                   # 路由配置（懒加载）
    │   ├── store/                    # 状态管理 (Pinia)
    │   ├── styles/                   # 全局主题 token（theme.css）
    │   ├── utils/                    # 工具类 (Axios封装)
    │   ├── views/                    # 页面（约40个视图：基础CRUD/教务/学工/实习/分析/系统）
    │   ├── App.vue
    │   └── main.js
    ├── index.html
    ├── vite.config.js
    └── package.json
```

## 环境要求

- **JDK**: 17 或以上
- **Node.js**: 16.x 或以上
- **MySQL**: 8.0 或以上
- **Redis**: 6.x 或以上（令牌黑名单与刷新令牌依赖，需先启动）
- **Maven**: 可选。项目内置 Maven Wrapper，会在首次运行时独立下载 Maven 3.9.6；也可使用 Maven 3.6.x 或以上。

## 快速开始

### 1. 克隆/下载项目

```bash
# 将项目下载到本地
```

### 2. 初始化数据库

> ⚠️ 启动后端前请先启动 Redis（默认 `localhost:6379`），否则登录/鉴权相关功能会因无法连接 Redis 而报错。可用 `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` 环境变量覆盖连接配置。

**方式一：命令行导入**

```bash
mysql -u root -p < student-management/src/main/resources/sql/init.sql
```

**方式二：MySQL Workbench / Navicat**

直接打开 `student-management/src/main/resources/sql/init.sql` 执行即可。

### 3. 修改后端配置

后端启动依赖以下环境变量（**已移除 `application.yml` 中的默认回退值，未注入即拒绝启动**）：

| 环境变量 | 必填 | 默认 | 说明 |
|---|---|---|---|
| `JWT_SECRET` | ✅ | 无（缺失/过短即拒绝启动） | JWT 签名密钥，≥32 字符强随机值 |
| `DB_PASSWORD` | ✅ | 无（缺失即拒绝启动） | 数据库密码；本地开发可用 `123456` |

本地开发推荐直接用 `start-all.bat`（自动生成 JWT_SECRET 并注入本地 DB_PASSWORD）。手动启动时：

```bash
cd student-management
set JWT_SECRET=<你的强随机密钥>
set DB_PASSWORD=123456        # 替换为你的 MySQL 密码
mvnw.cmd spring-boot:run
```

数据库连接 URL 位于 `student-management/src/main/resources/application.yml`，用户名默认 `root`（可用 `DB_USERNAME` 覆盖）。

### 4. 启动后端

**方式一：IDEA（推荐）**

1. 用 IntelliJ IDEA 打开 `student-management` 目录
2. 等待 Maven 依赖自动下载
3. 运行 `StudentApplication.java`

**方式二：命令行**

```bash
cd student-management
mvnw.cmd spring-boot:run
```

Windows 用户无需预先安装 Maven；`mvnw.cmd` 会自动下载 Wrapper 所需的 Maven 发行版并复用本地缓存。也可以直接运行 `mvnw.bat`。

后端启动后访问：`http://localhost:8080/doc.html` 查看接口文档

### 5. 启动前端

```bash
cd student-frontend
npm install
npm run dev
```

前端启动后访问：`http://localhost:5173`

## 数据库设计

核心业务表 + 教务 / 学工 / 实习 / 系统共 **37 张表**（实体 37 个），主要分组：

| 分组 | 表 |
|------|-----|
| 基础 | sys_user, student, teacher, clazz, course, score |
| 教务 | academic_term, course_term, teaching_task, course_selection, training_plan, training_plan_course |
| 学工 | leave_request, hardship_funding, reward_punishment, student_status_history, student_warning |
| 实习就业 | practice_plan, practice_position, practice_company, practice_assignment, practice_log, practice_evaluation, practice_certificate, employment_record, student_certificate |
| 组织 | school, department, major |
| 系统 | sys_role, sys_permission, sys_user_role, sys_role_permission, sys_data_scope, operation_log, notification, import_task, export_task, scheduled_task, scheduled_task_log 等 |

### ER 关系图（核心）

```
sys_user ─── 1:1 ─── student (student.user_id)
sys_user ─── 1:1 ─── teacher (teacher.user_id)

clazz ─── 1:N ─── student (student.clazz_id)
teacher ─── 1:N ─── clazz (clazz.head_teacher_id)
teacher ─── 1:N ─── course (course.teacher_id)

school ─── 1:N ─── department ─── 1:N ─── major ─── 1:N ─── clazz

student ─── N:M ─── course (通过 score 表)
term ─── 1:N ─── course_term ─── 1:N ─── teaching_task / course_selection
```

## 默认账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | 123456 | 管理员 | 系统管理员，全部权限 |
| teacher1 | 123456 | 教师 | 张老师 |
| student1 | 123456 | 学生 | 张三 |
| student2 | 123456 | 学生 | 李四 |

## 接口文档

启动后端后访问 Knife4j 文档：

- **文档地址**：`http://localhost:8080/doc.html`
- **OpenAPI JSON**：`http://localhost:8080/v3/api-docs`

### 主要 API 列表（按模块，完整见 Knife4j）

| 模块 | 前缀 | 说明 |
|------|------|------|
| 认证 | /api/auth | 登录、登出、刷新令牌、验证码、改密 |
| 学生/班级/课程/教师 | /api/student, /api/clazz, /api/course, /api/teacher | CRUD + 分页 + Excel 导入导出 |
| 成绩 | /api/score, /api/score-lifecycle | CRUD、统计、审核、作废、变更日志、导入导出 |
| 教务 | /api/academic-term, /api/course-term, /api/teaching-task, /api/course-selection, /api/training-plan | 学期、课程开设、授课任务、选课、培养方案 |
| 学工 | /api/leave, /api/funding, /api/reward-punishment, /api/student-status, /api/warning | 请假、资助、奖惩、学籍异动、预警 |
| 实习就业 | /api/practice-assignment, /api/practice-log, /api/employment | 实习分配、日志、就业 |
| 分析 | /api/statistics | 概览、成绩分布、班级均分、趋势 |
| 组织架构 | /api/org | 学校/系部/专业三级树 |
| 通知 | /api/notification | 分页、未读数、已读 |
| 系统 | /api/system/* | 用户管理、角色、在线用户、健康检查、操作日志 |
| 开放接口 | /api/v1/* | OpenAPI v1（X-Client-Id + 幂等键 + 限流） |

> 共 100+ 个接口，完整列表请访问 Knife4j 文档。

## 部署说明

### 部署环境变量（生产必配）

| 环境变量 | 必填 | 生产建议 |
|---|---|---|
| `JWT_SECRET` | ✅ | 强随机 ≥32 字符，与开发环境不同 |
| `DB_PASSWORD` | ✅ | 强密码，禁止 123456 之类弱口令 |
| `CORS_ALLOWED_ORIGINS` | ✅ | 仅填真实前端域名，逗号分隔 |
| `KNIFE4J_ENABLE` | ❌ | 生产设为 `false` 关闭接口文档 |

### 后端打包

```bash
cd student-management
mvnw.cmd clean package
java -jar target/student-management-1.0.0.jar
```

### 前端打包

```bash
cd student-frontend
npm run build
```

打包产物在 `student-frontend/dist` 目录，可部署到 Nginx 等静态服务器。

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /path/to/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 开发规范

- 后端代码分层：Controller → Service → Mapper
- 统一返回格式：`{ code, msg, data }`
- 分页返回格式：`{ total, records, current, size }`
- 数据库表使用逻辑删除（`deleted` 字段）
- 前端页面使用 Vue 3 Composition API + `<script setup>`

## 常见问题

**Q: 登录失败？**
A: 请检查数据库中 `sys_user` 表的密码是否为正确的 BCrypt 加密值（123456 对应 `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`）。

**Q: 前端跨域问题？**
A: Vite 已配置代理（`/api` → `http://localhost:8080`），开发环境直接访问即可。

**Q: Maven 依赖下载慢？**
A: 可在 Maven 的 `settings.xml` 中配置阿里云镜像：
```xml
<mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

## 许可证

MIT License

---

⭐ 如果这个项目对你有帮助，欢迎 Star 支持！
