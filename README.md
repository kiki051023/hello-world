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

学生管理系统是一个全栈 Web 应用，实现了学生信息、班级、课程、成绩、教师等核心业务的全生命周期管理。系统采用前后端分离架构，后端基于 Spring Boot 3 + MyBatis-Plus，前端基于 Vue 3 + Element Plus，支持 JWT 身份认证和多角色权限控制。

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
- 用户登录 / 登出
- JWT Token 认证
- 角色权限（管理员 / 教师 / 学生）

### 👨‍🎓 学生管理
- 学生信息增删改查
- 按姓名、学号、班级筛选
- 分页查询
- 班级关联显示

### 🏫 班级管理
- 班级信息增删改查
- 班级名称、年级、班主任管理
- 分页查询

### 📚 课程管理
- 课程信息增删改查
- 课程编号、名称、学分、授课教师管理
- 分页查询

### 📊 成绩管理
- 成绩录入、修改、删除
- 按学生、课程、班级筛选
- 成绩统计（平均分、最高分、最低分、及格率）
- 分数等级颜色标签
- 分页查询

### 👨‍🏫 教师管理
- 教师信息增删改查
- 工号、姓名、性别、电话管理

### 🏠 首页看板
- 学生/班级/课程/教师总数统计
- 欢迎信息与功能导航

## 项目结构

```
student-management-system/
├── student-management/          # 后端（Spring Boot）
│   ├── src/main/java/com/example/student/
│   │   ├── StudentApplication.java   # 启动类
│   │   ├── common/                   # 公共组件 (Result, PageResult)
│   │   ├── config/                   # 配置类
│   │   ├── controller/               # 控制层 (7个Controller)
│   │   ├── entity/                   # 实体类 (6个实体)
│   │   ├── mapper/                   # 持久层 (6个Mapper)
│   │   ├── service/                  # 业务层
│   │   │   └── impl/                 # 业务实现类
│   │   ├── security/                 # 安全层 (JWT过滤器)
│   │   ├── exception/                # 异常处理
│   │   └── utils/                    # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml           # 配置文件
│   │   ├── mapper/                   # MyBatis XML
│   │   └── sql/init.sql              # 数据库初始化脚本
│   └── pom.xml
│
└── student-frontend/              # 前端（Vue 3）
    ├── src/
    │   ├── api/                      # API 接口 (6个模块)
    │   ├── layout/                   # 布局组件
    │   ├── router/                   # 路由配置
    │   ├── store/                    # 状态管理 (Pinia)
    │   ├── utils/                    # 工具类 (Axios封装)
    │   ├── views/                    # 页面 (7个页面)
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

编辑 `student-management/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: "你的MySQL密码"
```

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

共 **6 张核心表**：

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| sys_user | 用户表 | username, password, role, status |
| student | 学生表 | student_no, name, gender, clazz_id |
| teacher | 教师表 | teacher_no, name, gender, phone |
| clazz | 班级表 | clazz_name, grade, head_teacher_id |
| course | 课程表 | course_no, course_name, credit, teacher_id |
| score | 成绩表 | student_id, course_id, score, exam_time |

### ER 关系图

```
sys_user ─── 1:1 ─── student (student.user_id)
sys_user ─── 1:1 ─── teacher (teacher.user_id)

clazz ─── 1:N ─── student (student.clazz_id)
teacher ─── 1:N ─── clazz (clazz.head_teacher_id)
teacher ─── 1:N ─── course (course.teacher_id)

student ─── N:M ─── course (通过 score 表)
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

### 主要 API 列表

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 认证 | /api/auth/login | POST | 登录 |
| 认证 | /api/auth/logout | POST | 登出 |
| 学生 | /api/student/page | GET | 分页列表 |
| 学生 | /api/student/{id} | GET | 详情 |
| 学生 | /api/student | POST | 新增 |
| 学生 | /api/student | PUT | 修改 |
| 学生 | /api/student/{id} | DELETE | 删除 |
| 班级 | /api/clazz/list | GET | 全部列表 |
| 课程 | /api/course/list | GET | 全部列表 |
| 成绩 | /api/score/page | GET | 分页列表 |
| 成绩 | /api/score/stats | GET | 成绩统计 |
| 教师 | /api/teacher/list | GET | 全部列表 |

> 共 31 个接口，详情请访问 Knife4j 文档。

## 部署说明

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
