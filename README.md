# wblog-backend

一个基于 **Spring Boot 2.5.14 + MyBatis-Plus + MySQL + Redis** 的个人博客后端，提供文章、分类、标签、评论、用户、文件上传等 REST 接口，供前后端分离的前端（门户 / 管理后台）调用。

> 本文档基于当前 `main` 分支源码逐文件分析编写（分析日期：2026-11-07），文中「实测」结论均在本机 Windows + Maven 3.9.6 环境验证过。项目代码主要写于 2022 年，含大量中文教学注释与外链，属于学习型 / 教学型博客后端，并非开箱即用的生产系统。

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 技术栈与版本](#2-技术栈与版本)
- [3. 仓库结构](#3-仓库结构)
- [4. 模块说明](#4-模块说明)
- [5. 环境要求与外部依赖](#5-环境要求与外部依赖)
- [6. 配置说明](#6-配置说明)
- [7. 构建与启动](#7-构建与启动)
- [8. 数据库设计](#8-数据库设计)
- [9. REST API 一览](#9-rest-api-一览)
- [10. 统一响应与错误码](#10-统一响应与错误码)
- [11. 核心实现与架构](#11-核心实现与架构)
- [12. 认证与权限模型](#12-认证与权限模型)
- [13. 日志、缓存与异步](#13-日志缓存与异步)
- [14. 已知问题与改进建议](#14-已知问题与改进建议)
- [15. Git 忽略规则](#15-git-忽略规则)
- [16. FAQ](#16-faq)

---

## 1. 项目概览

| 项目 | 内容 |
| --- | --- |
| 组织 / 产物 | `com.weixf.blog:wblog-backend:1.0-SNAPSHOT`，父工程 `packaging=pom` |
| 架构 | Maven 多模块聚合（2 个子模块）+ 单体 Spring Boot 应用 |
| 实际可运行服务 | **仅 `wblog-portal`**，监听 `8088`，无 `context-path` |
| 入口类 | `wblog-portal/src/main/java/com/weixf/blog/WBlogApp.java`（全仓库唯一 `@SpringBootApplication`） |
| 根包 | `com.weixf.blog` |
| 代码规模 | 75 个 `.java`（portal 74 个、manage 1 个）、2 个 Mapper XML、2 个 `application.yml`、2 个 `logback-spring.xml` |
| 业务能力 | 文章列表/详情/发布/归档、热门与最新文章、分类、标签、两级评论树、注册/登录/登出/当前用户、OSS 文件上传与删除 |
| 数据层 | 7 张 `ms_` 前缀业务表，`BaseMapper` + 2 个自定义 XML，分页插件 |
| 接口风格 | `@RestController` + `Result` 统一包装，路径无 `/api` 前缀，列表类接口也多用 `POST` |
| 测试 / 部署 | **无测试代码**（`src/test/java` 为空）、**无 SQL 建表脚本**、**无 Dockerfile / CI / 部署脚本**、**无 LICENSE** |

---

## 2. 技术栈与版本

父 `pom.xml` 继承 `spring-boot-starter-parent:2.5.14`，编译目标 Java 8（`maven.compiler.source/target=8`、UTF-8 编码）。未在仓库显式声明版本的依赖由 Spring Boot BOM 管理。

| 类别 | 组件 | 版本 | 说明 |
| --- | --- | --- | --- |
| 基础框架 | Spring Boot | 2.5.14 | parent |
| 语言 / 构建 | Java 8 / Maven | source=8, target=8 | 聚合父工程 |
| Web | `spring-boot-starter-web` | BOM | Spring MVC，内嵌 Tomcat |
| AOP | `spring-boot-starter-aop` | BOM | 缓存切面、日志切面 |
| 持久层 | `mybatis-plus-boot-starter` | 3.5.2 | `BaseMapper`、分页插件 |
| 代码生成 | `mybatis-plus-generator` | 3.5.3 | 引入但无生成器代码 |
| 连接池 | `druid-spring-boot-starter` | 1.2.11 | 数据源 |
| 数据库 | `mysql-connector-java` | BOM（`com.mysql.cj.jdbc.Driver`） | MySQL |
| 缓存 / 会话 | `spring-boot-starter-data-redis` | BOM（Lettuce） | `TOKEN_` 会话与热点缓存 |
| 认证 | `jjwt` | 0.9.1 | HS256 JWT |
| JSON | `fastjson` | 2.0.10 | 序列化、缓存值、拦截器响应 |
| 对象存储 | `aliyun-sdk-oss` | 3.15.1 | 图片上传/删除 |
| 工具 | `commons-lang3` / `commons-codec` / `commons-collections4` 4.4 / `commons-pool2` / `joda-time` / `lombok` | 混合 | MD5、字符串、日期、简化 POJO |
| 配置加密 | `jasypt-spring-boot-starter` | 3.0.4 | 引入但配置中无 `ENC(...)` |
| API 文档 | `springfox-swagger2` / `springfox-swagger-ui` | 3.0.0 | 引入但未启用 `Docket` / `@EnableOpenApi` |
| 验证码 | `kaptcha` | 0.0.9 | 引入但无验证码代码 |
| 其他 starter | `amqp`、`mail`、`test`、`configuration-processor` | BOM | 均无对应业务代码/配置 |
| 安全框架 | `spring-boot-starter-security` | BOM | **仅 manage 模块引入**，portal 未引入 |
| 日志 | `logback-spring.xml` | — | 自定义彩色控制台输出，文件 appender 全部注释 |

> 依赖冗余较多：`amqp`、`mail`、`kaptcha`、`springfox-*`、`jasypt`、`mybatis-plus-generator` 均无实际使用；portal 未引入 Security，却留有一个空的 `WebSecurityConfig.java`。

---

## 3. 仓库结构

```text
wblog-backend/
├── pom.xml                          # 聚合父 POM：parent / modules / dependencyManagement / 插件
├── .gitignore                       # IDE、构建产物、临时文件忽略规则
├── README.md                        # 本文档
├── wblog-manage/                    # 后台管理模块（占位，见 §4）
│   ├── pom.xml
│   └── src/main/
│       ├── Java/com/weixf/blog/Test.java   # 注意：Git 中目录名为大写 Java
│       └── resources/
│           ├── application.yml            # 端口 8089
│           └── logback-spring.xml
└── wblog-portal/                    # 门户服务（唯一可运行模块）
    ├── pom.xml
    └── src/
        ├── main/java/com/weixf/blog/
        │   ├── WBlogApp.java                 # 启动类
        │   ├── config/                       # 6 个配置类
        │   │   ├── CorsConfig.java           # WebFlux 风格 CORS（Servlet 环境不生效）
        │   │   ├── MybatisPlusConfig.java    # @MapperScan + 分页插件
        │   │   ├── OSSProperties.java        # OSS 配置转静态常量
        │   │   ├── ThreadPoolConfig.java     # @EnableAsync 线程池
        │   │   ├── WebMVCConfig.java         # CORS + 登录拦截器注册
        │   │   └── WebSecurityConfig.java    # 空类
        │   ├── controller/                  # 10 个 Controller，22 个端点
        │   ├── service/ + service/impl/     # 8 个接口 / 7 个实现 + ThreadService
        │   ├── dao/
        │   │   ├── mapper/                  # 7 个 BaseMapper 接口
        │   │   ├── pojo/                   # 7 个实体
        │   │   └── dos/Archives.java        # 归档统计 DO
        │   ├── vo/ + vo/params/             # 9 个 VO/枚举 + 5 个入参对象
        │   ├── handler/                     # 全局异常 + 登录拦截器
        │   ├── common/                      # aop 日志注解/切面、cache 注解/切面、自定义异常
        │   └── utils/                       # JWT、IP、ThreadLocal、七牛、异常工具等
        ├── main/resources/
        │   ├── application.yml
        │   ├── logback-spring.xml
        │   └── mapper/{ArticleMapper.xml,TagMapper.xml}
        └── test/java/                       # 空目录
```

包数量统计（portal）：`controller` 10、`service`+`impl` 15、`dao` 15、`vo`+`params` 14、`config` 6、`utils` 6、`handler` 2、`common` 5、根 1。

---

## 4. 模块说明

### `wblog-portal`（门户服务，实际主体）

- 默认 `jar`，端口 **8088**，`spring.application.name=wblog-backend`。
- 24 个依赖，包含 Web、AOP、Redis、MyBatis-Plus、Druid、MySQL、JWT、OSS、Swagger、Jasypt、Lombok 等。
- 拥有全部业务代码、Mapper XML 与唯一启动类。
- **构建 / 运行的唯一目标模块**。

### `wblog-manage`（后台管理，占位模块）

- 默认 `jar`，`application.yml` 端口 **8089**，16 个依赖（含 `spring-boot-starter-security`）。
- 源码仅 `Test.java` 一个空类，**没有启动类、没有 Controller、没有业务代码**。
- Git 中该源码路径记录为 `src/main/Java/...`（大写 `J`）；Windows 不区分大小写所以本地正常，但在 Linux/macOS 检出后 Maven 默认只编译 `src/main/java`，该类会被完全忽略。
- 因父 POM 的 `spring-boot-maven-plugin` 被所有子模块继承，该模块执行 `package` 时 `repackage` 会因「Unable to find main class」失败（已实测）。因此**不要直接对整个 reactor 执行 `mvn package`**，见 §7。

> 两个模块之间没有任何 `<dependency>` 依赖，也没有 common 共享模块；`wblog-manage` 目前只是预留壳。

---

## 5. 环境要求与外部依赖

| 依赖 | 要求 | 用途 / 备注 |
| --- | --- | --- |
| JDK | **Java 8**（推荐 `1.8.0_311` 实测通过） | JDK 11/17 通常可用；**JDK 21 编译失败**，见 §7 |
| Maven | 3.6+（本机 3.9.6 实测） | 聚合构建 |
| MySQL | 5.7 / 8.x，库名 `wblog`，`localhost:3306` | 需自行建库建表，仓库无 DDL |
| Redis | `127.0.0.1:6379`，无密码 | 登录会话 `TOKEN_*` 与 `@Cache` 缓存；启动即会连接 |
| 阿里云 OSS | 可选 | 仅 `/oss/file*` 接口使用，需有效 AK/SK 与 bucket |
| 七牛云 | 可选（当前不可用） | `/upload` 走七牛，但实现被注释，见 §14 |

端口：`8088`（portal）、`8089`（manage 配置值）；两者都**没有** `server.servlet.context-path`，接口路径即 `/articles`、`/login` 等。

---

## 6. 配置说明

配置文件共 4 个：`wblog-portal` / `wblog-manage` 各自的 `application.yml` 与 `logback-spring.xml`。**没有** `application-dev.yml`、`.properties`、`bootstrap.yml`。

### `application.yml`（portal，43 行）

| 配置键 | 值 | 说明 |
| --- | --- | --- |
| `server.port` | `8088` | manage 为 `8089` |
| `spring.application.name` | `wblog-backend` | 两模块同名 |
| `spring.profiles.active` | `dev` | 仅影响 logback 的 `<springProfile>`；无对应 profile 配置文件 |
| `spring.datasource.druid.url` | `jdbc:mysql://localhost:3306/wblog?useUnicode=true&characterEncoding=UTF-8&serverTimeZone=UTC` | 库名 `wblog` |
| `spring.datasource.druid.driver-class-name` | `com.mysql.cj.jdbc.Driver` | MySQL 8 驱动 |
| `spring.datasource.druid.username` / `password` | `root` / 明文口令 | **第 13 行为明文密码，必须改为环境变量** |
| `spring.redis.*` | `127.0.0.1:6379`，timeout `6s`，Lettuce 池 `max-active=1000 / max-wait=-1ms / max-idle=10 / min-idle=5` | 会话与缓存 |
| `spring.servlet.multipart` | `max-file-size=2MB`、`max-request-size=20MB` | 上传体积限制 |
| `mybatis-plus.configuration.log-impl` | `StdOutImpl` | SQL 打印到控制台 |
| `mybatis-plus.global-config.db-config.table-prefix` | `ms_` | 全局表前缀 |
| `mybatis-plus.mapper-locations` | `classpath:mapper/*.xml` | 自定义 SQL |
| `aliyun.oss.*` | endpoint `oss-cn-hangzhou.aliyuncs.com`、bucket `pictures01`、key-id / key-secret = `123` | 占位值，非真实密钥 |
| `qiniu.accessKey` / `accessSecretKey` | `123`（manage 中为空） | 占位值，当前未使用 |

两份 yml 的差异仅：端口、OSS/七牛密钥是否为空。配置中**没有** `${ENV}` 占位符、`ENC(...)` 密文、JWT 配置项、邮件/RabbitMQ 配置、文件上传本地路径。

### 其它配置来源（Java Config）

- `WebMVCConfig`：`addMapping("/**")` + `allowedOriginPatterns("*")` + `allowCredentials(true)` + 允许 GET/HEAD/POST/PUT/DELETE/OPTIONS + `maxAge=3600`；并注册登录拦截器到 3 条路径。
- `CorsConfig`：使用 `org.springframework.web.cors.reactive.CorsWebFilter`（WebFlux 组件），在 Servlet MVC 工程中通常不生效，实际生效的是 `WebMVCConfig`。
- `MybatisPlusConfig`：`@MapperScan("com.weixf.blog.dao.mapper")` + `PaginationInnerInterceptor`。
- `ThreadPoolConfig`：`@EnableAsync`，Bean 名 `taskExcutor`，core 5 / max 20 / queue `Integer.MAX_VALUE` / keepAlive 60s / 前缀「云间散月博客」。
- `OSSProperties`：`@Value` 读取配置后写入公开静态常量，供 `OssServiceImpl` 使用。

### 日志

`logback-spring.xml`：`scan=true`（10 秒）、彩色控制台 appender + `ThresholdFilter=DEBUG`；**INFO/WARN/ERROR 文件 appender 整段被注释**，`log.path` 也被注释，因此日志只输出到控制台、不落盘。`<springProfile name="dev">` 下 `com.weixf.blog=DEBUG`、`root=INFO`；`prod` 下 `root=INFO`。

---

## 7. 构建与启动

### 环境准备

1. 安装 **JDK 8** 并设置 `JAVA_HOME`；确认 `mvn -v` 显示的是 Java 8。
2. 启动 MySQL，创建数据库 `wblog`，并**自行准备 7 张 `ms_` 前缀表**（仓库无 DDL，字段见 §8）。
3. 启动 Redis（默认 `127.0.0.1:6379`）。
4. 按实际环境修改 `wblog-portal/src/main/resources/application.yml` 的数据库账号、Redis 地址、OSS 密钥。

### 构建命令（推荐：只构建 portal）

```bash
# 在仓库根目录执行
mvn -pl wblog-portal -DskipTests clean package
# 产物：wblog-portal/target/wblog-portal-1.0-SNAPSHOT.jar
```

也可进入模块目录构建：

```bash
cd wblog-portal
mvn -DskipTests package
```

启动：

```bash
java -jar wblog-portal/target/wblog-portal-1.0-SNAPSHOT.jar
# 或直接在 IDE 中运行 com.weixf.blog.WBlogApp#main
```

验证：

```bash
curl http://localhost:8088/tags
curl -X POST http://localhost:8088/articles -H "Content-Type: application/json" -d "{\"page\":1,\"pageSize\":10}"
```

### 实测记录（本机 Windows / Maven 3.9.6）

| 命令 | JDK | 结果 |
| --- | --- | --- |
| `mvn -o -pl wblog-portal -DskipTests package` | `jdk1.8.0_311` | **成功**，生成约 70 MB 可执行 jar |
| `mvn -o -pl wblog-portal -DskipTests package` | `jdk-21.0.5` | **失败**：`NoSuchFieldError: JCTree$JCImport.qualid`，Spring Boot 2.5.14 BOM 管理的 Lombok 版本与 JDK 21 的 javac 内部 API 不兼容 |
| `mvn -o -DskipTests package`（全 reactor） | `jdk-21.0.5` | **失败**于 `wblog-manage` 的 `spring-boot-maven-plugin:repackage`：「Unable to find main class」 |

结论与规避：

- 使用 JDK 8/11；若必须用 JDK 17/21，需显式升级 Lombok（1.18.30+）并保证与编译器兼容。
- 不要直接整仓 `mvn package`；只构建 `wblog-portal`，或为 `wblog-manage` 关闭 `repackage` / 补全启动类。
- 父 POM 的 `<resources>` 规则会把 `src/main/java/**/*.xml` 一起打包，但当前 java 目录下没有 XML，属于历史遗留配置。

---

## 8. 数据库设计

**仓库没有任何 `.sql` 文件、迁移脚本或数据初始化脚本**。以下表名与字段由实体类、`ArticleMapper.xml` 的 `resultMap`、以及 SQL 语句反推；`ms_` 前缀来自 `table-prefix` 配置，实体类**未使用** `@TableName` / `@TableId` / `@TableField`，完全依赖 MyBatis-Plus 驼峰转下划线规则。

| 表名 | 主要字段 | 说明 |
| --- | --- | --- |
| `ms_article` | `id`, `title`, `summary`, `comment_counts`, `view_counts`, `author_id`, `body_id`, `category_id`, `weight`, `create_date` | 文章主表；`weight=1` 置顶，`create_date` 为毫秒时间戳 |
| `ms_article_body` | `id`, `content`, `content_html`, `article_id` | 文章正文（Markdown 与 HTML） |
| `ms_article_tag` | `id`, `article_id`, `tag_id` | 文章-标签关联；热门标签按 `tag_id` 分组计数 |
| `ms_tag` | `id`, `avatar`, `tag_name` | 标签 |
| `ms_category` | `id`, `avatar`, `category_name`, `description` | 分类 |
| `ms_comment` | `id`, `content`, `create_date`, `article_id`, `author_id`, `parent_id`, `to_uid`, `level` | 两级评论（`level=1` 主评论 / `2` 子评论） |
| `ms_sys_user` | `id`, `account`, `admin`, `avatar`, `create_date`, `deleted`, `email`, `last_login`, `mobile_phone_number`, `nickname`, `password`, `salt`, `status` | 用户；密码为 `md5Hex(明文 + 固定盐)` |

> 注意：`ArticleMapper.xml` 中一段**已注释**的旧 SQL 使用 `ms_article_tags`（复数），生效 SQL 用的是 `ms_article_tag`。新建表时以单数为准。

### 自定义 SQL（`resources/mapper/`）

| 文件 | 语句 | 作用 |
| --- | --- | --- |
| `ArticleMapper.xml` | `listArchives` | `year/month(FROM_UNIXTIME(create_date/1000))` 分组统计归档数量 |
| `ArticleMapper.xml` | `listArticle` | 动态 `<where>` 按 `category_id`、`tag_id`（子查询 `ms_article_tag`）、年月筛选，`order by weight, create_date desc`，配合分页插件 |
| `TagMapper.xml` | `findTagsByArticleId` | 按文章 ID 查标签 |
| `TagMapper.xml` | `findHotsTagIds` | `GROUP BY tag_id ORDER BY count(*) DESC LIMIT` 取热门标签 ID |
| `TagMapper.xml` | `findTagsByTagIds` | `<foreach>` 批量查标签 |

其余 CRUD 全部由 MyBatis-Plus `BaseMapper` / `LambdaQueryWrapper` 完成（7 个 Mapper 均 `extends BaseMapper<T>` + `@Repository`）。

---

## 9. REST API 一览

共 **10 个 Controller、22 个端点**。除标注外均无需登录；「需登录」= 被 `LoginInterceptor` 拦截的路径。

### 文章 `/articles`

| 方法 | 路径 | 入参 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/articles` | `PageParams`（page/pageSize/categoryId/tagId/year/month） | 首页文章分页列表（按分类/标签/年月筛选），带 `@LogAnnotation` |
| `POST` | `/articles/hot` | — | 最热 5 篇（按 `view_counts` 倒序），`@Cache` 5 分钟 |
| `POST` | `/articles/new` | — | 最新 5 篇（按 `create_date` 倒序），`@Cache` 5 分钟 |
| `POST` | `/articles/listArchives` | — | 年月归档统计 |
| `POST` | `/articles/view/{id}` | 路径 ID | 文章详情（正文/标签/作者/分类聚合），并异步自增浏览量 |
| `POST` | `/articles/publish` | `ArticleParam`（title/summary/body/category/tags） | **需登录**：发布文章 |

### 分类与标签

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/categorys` | 全部分类（仅 id/name） |
| `GET` | `/categorys/detail` | 全部分类详情 |
| `GET` | `/categorys/detail/{id}` | 单个分类详情 |
| `GET` | `/tags/hot` | 热门 6 个标签 |
| `GET` | `/tags` | 全部标签 |
| `GET` | `/tags/detail` | 全部标签详情 |
| `GET` | `/tags/detail/{id}` | 单个标签详情 |

### 评论 `/comments`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/comments/article/{id}` | 文章评论树（一级评论 + 子评论） |
| `POST` | `/comments/create/change` | **需登录**：发表评论（`CommentParam`） |

### 用户与认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/login` | 登录，返回 JWT token（`LoginParam`：account/password/nickname） |
| `POST` | `/register` | 注册，返回 JWT token |
| `GET` | `/logout` | 登出，请求头 `Authorization`，删除 Redis 会话 |
| `GET` | `/users/currentUser` | 由 token 解析当前用户，请求头 `Authorization` |

### 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/oss/file` | 阿里云 OSS 上传，表单字段 `image`，返回可访问 URL |
| `POST` | `/oss/file/remove` | 删除 OSS 文件，参数 `cover`（完整 URL） |
| `POST` | `/upload` | 七牛云上传，表单字段 `image`；**当前恒返回「上传失败」**，见 §14 |

调用示例：

```bash
# 登录
curl -X POST http://localhost:8088/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"..."}'

# 携带 token 访问当前用户
curl http://localhost:8088/users/currentUser -H "Authorization: <token>"
```

---

## 10. 统一响应与错误码

`vo/Result.java` 统一包装所有接口：

```json
{ "success": true, "code": 200, "msg": "success", "data": {} }
```

- `Result.success(data)` → `code=200, msg="success"`；
- `Result.fail(code, msg)` → `success=false, data=null`；
- 全局异常 `AllExceptionHandler`：`Exception → -999 系统异常`，`GlobalException` 保留自带 code/msg。

`vo/ErrorCode.java` 错误码枚举：

| code | 常量 | 含义 |
| --- | --- | --- |
| 10001 | `PARAMS_ERROR` | 参数有误 |
| 10002 | `ACCOUNT_PWD_NOT_EXIST` | 用户名或密码不存在 |
| 10003 | `TOKEN_ERROR` | token 不合法 |
| 10004 | `ACCOUNT_EXIST` | 账号已存在 |
| 70001 | `NO_PERMISSION` | 无访问权限（仅定义，无引用） |
| 90001 | `SESSION_TIME_OUT` | 会话超时（仅定义，无引用） |
| 90002 | `NO_LOGIN` | 未登录（拦截器使用） |
| 20001 | — | 上传失败（`/upload` 硬编码） |
| -999 | — | 系统异常 / OSS 上传删除失败 |

---

## 11. 核心实现与架构

分层调用链：`Controller → Service(Impl) → Mapper(BaseMapper / XML) → MySQL`，横切为 `handler`（异常/拦截器）与 `common`（AOP 切面）。对象转换使用 `BeanUtils.copyProperties` 从 `pojo` 生成 `vo`。

| 能力 | 实现 | 要点 |
| --- | --- | --- |
| 文章列表 | `ArticleServiceImpl.listArticle` → `ArticleMapper.xml#listArticle` | `Page<Article>` + 分页插件；`PageParams` 默认 `page=1, pageSize=10`，`getMonth()` 会把 `6` 补零为 `06` |
| 热门 / 最新 | `ArticleServiceImpl.hotArticle / newArticles` | `LambdaQueryWrapper` + `last("limit 5")`，只 select `id,title` |
| 文章详情 | `ArticleServiceImpl.findArticleById` | 聚合正文、标签、作者昵称、分类；`joda-time DateTime` 格式化毫秒时间戳；触发异步浏览量更新 |
| 文章发布 | `ArticleServiceImpl.publish` | 从 `UserThreadLocal` 取登录用户，依次写 `ms_article` → `ms_article_tag` → `ms_article_body` → 回写 `body_id`；**未加 `@Transactional`** |
| 归档 | `ArticleMapper.xml#listArchives` | `FROM_UNIXTIME(create_date/1000)` 按年月 `GROUP BY` |
| 分类 / 标签 | `CategoryServiceImpl` / `TagServiceImpl` | 热门标签先查关联表 top ID，再批量查标签 |
| 评论 | `CommentsServiceImpl` | 两级树：先查 `level=1`，再按 `parent_id` 查 `level=2`；`toUid` 指向被回复人；插入后重算并回写文章 `comment_counts` |
| 用户 | `SysUserServiceImpl` | 按账号查用户、按 ID 查作者；用户不存在时兜底昵称「码神之路」与默认头像路径 |
| OSS 上传 | `OssServiceImpl` + `OSSProperties` | UUID + `yyyyMMdd` 目录，`putObject`，拼接 `https://{bucket}.{endpoint}/{fileName}`；删除用 `DeleteObjectsRequest` |
| 参数对象 | `vo/params/*` | `PageParams`、`ArticleParam`、`ArticleBodyParam`、`CommentParam`、`LoginParam` |

未实现的能力：**全文搜索、友链、独立统计接口、后台管理接口、验证码、邮件、消息队列**。统计能力仅体现为 `view_counts` / `comment_counts` / 归档 count / 热门文章与标签。

---

## 12. 认证与权限模型

**认证链路**：

1. `POST /login`：`LoginServiceImpl.login` 校验参数 → `DigestUtils.md5Hex(password + 固定盐)` → `SysUserServiceImpl.findUser` 按 account+password 查 `ms_sys_user` → `JWTUtils.createToken(userId)` → `redisTemplate.opsForValue().set("TOKEN_" + token, JSON(sysUser), 100, DAYS)` → 返回 token。
2. 后续请求在 `Authorization` 头携带 token；`LoginInterceptor.preHandle` 读取 → `LoginService.checkToken`（JWT 解析 + Redis `TOKEN_` 键查询 + 反序列化 `SysUser`）→ 成功写入 `UserThreadLocal`，`afterCompletion` 中 `remove()` 防内存泄漏。
3. `GET /logout`：删除 Redis `TOKEN_` 键。
4. `POST /register`：账号查重 → MD5 加盐落库（默认 `admin=1`、`deleted=0`、默认头像）→ 同样签发 token 并写 Redis。

**拦截器覆盖范围**（`WebMVCConfig.addInterceptors`）：仅 `/test`、`/comments/create/change`、`/articles/publish` 三条路径；其中 `/test` 并无对应 Controller，是示例占位。**登录、注册、OSS 上传/删除、文章列表等均不拦截**。

**权限**：全仓库无 `@PreAuthorize` / `@Secured` / `SecurityFilterChain` / `WebSecurityConfigurerAdapter`；`SysUser.admin` 字段虽被查询，但未参与任何鉴权判断；`WebSecurityConfig` 是空类。**没有角色 / 权限体系**。

**JWT 细节**：`JWTUtils` 使用 jjwt 0.9.1 + HS256，claims 仅 `userId`，密钥硬编码在类常量中；有效期表达式注释为「六百天」，与 Redis 会话 **100 天不一致**；解析失败仅 `printStackTrace` 返回 `null`。

---

## 13. 日志、缓存与异步

| 机制 | 实现 | 说明 |
| --- | --- | --- |
| Redis 缓存切面 | `@Cache(expire, name)` + `CacheAspect` | 环绕通知，key = `name::类名::方法名::md5(参数)`，`Duration.ofMillis(expire)`；仅 `/articles/hot`、`/articles/new` 启用（列表缓存被注释）；异常兜底 `Result.fail(-999,"系统错误")` |
| 操作日志切面 | `@LogAnnotation(module, operation)` + `LogAspect` | 环绕通知记录模块、操作、类方法、首个参数 JSON、IP（`IpUtils`）、耗时；仅 `/articles` 列表使用；输出到控制台 |
| 异步浏览量 | `@Async("taskExcutor") ThreadService.updateArticleViewCount` | 条件更新 `WHERE id=? AND view_counts=旧值` 模拟 CAS；更新后 `Thread.sleep(5000)`（无实际意义，占用线程 5 秒） |
| 线程池 | `ThreadPoolConfig` | `@EnableAsync`，core 5 / max 20 / 队列 `Integer.MAX_VALUE` / 优雅关闭 |
| 全局异常 | `AllExceptionHandler` | `@ControllerAdvice`，见 §10 |

---

## 14. 已知问题与改进建议

### 14.1 构建 / 工程结构

| 问题 | 影响 | 建议 |
| --- | --- | --- |
| 整仓 `mvn package` 在 `wblog-manage` 的 `repackage` 阶段失败（Unable to find main class） | 无法一键构建 | 只构建 `-pl wblog-portal`，或给 manage 补启动类 / 关闭其 `repackage` |
| JDK 21 下 Lombok 与 javac 内部 API 不兼容 | 编译失败 | 使用 JDK 8/11，或升级 Lombok ≥1.18.30 并验证 |
| `wblog-manage` 无启动类、仅 1 个空类；Git 中源码目录为大写 `Java` | 模块不可用，Linux 下该类不会被编译 | 删除占位模块或补齐实现，并把目录改为小写 `java` |
| 无 DDL / 数据初始化脚本、无测试、无 Docker / CI / LICENSE | 无法开箱运行、无法回归验证 | 补充 Flyway/Liquibase 或 `schema.sql`、单元测试与容器化配置 |
| `spring.profiles.active=dev` 但无 `application-dev.yml` | profile 形同虚设 | 拆分 dev/prod 配置，敏感值外置 |
| 大量引入未使用依赖 | 包体积大（fat jar 约 70 MB）、启动慢 | 清理 `amqp`/`mail`/`kaptcha`/`springfox`/`jasypt`/`generator` 等 |

### 14.2 功能缺陷

1. **`/upload` 恒失败**：`QiniuUtils.upload()` 的上传逻辑整段被注释且 `return false`，`UploadController` 固定返回 `Result.fail(20001,"上传失败")`；此外 `pom.xml` 也**没有引入七牛 SDK**，直接取消注释仍无法编译。可用的是 `/oss/file`。
2. **OSS 密钥为占位值 `123`**：`/oss/file*` 需要填入真实 AK/SK 与 bucket 才能工作；`OssServiceImpl.removeFile` 用 `cover.lastIndexOf("/", 50)` 截取 key，对短 URL 可能越界。
3. **发布文章无事务**：`ArticleServiceImpl.publish` 连续写 4 张表但无 `@Transactional`，中途失败会留下脏数据。
4. **会话时长不一致**：JWT 有效期约 600 天，Redis `TOKEN_` 仅 100 天，过期判断实际以 Redis 为准。
5. **拦截器保护范围过窄**：仅 3 条路径，OSS 上传/删除、评论外接口均无鉴权；且 `/test` 是无效路径。
6. **Swagger 未启用**：引入了 springfox 依赖却没有 `Docket` / `@EnableOpenApi`，访问不到 API 文档。
7. **热门缓存与实时数据不一致**：`@Cache` 缓存 5 分钟，期间发布/浏览不会刷新。
8. **默认头像路径不存在**：`/static/img/logo.b3a48c0.png` 只是字符串，仓库无 `static` 目录与图片资源。
9. **跨域过宽**：`allowedOriginPatterns("*")` + `allowCredentials(true)`；另有 WebFlux `CorsWebFilter` 冗余配置。
10. **`@Async("taskExcutor")`** Bean 名与拼写均非 `Executor`（自洽但不规范）；队列容量 `Integer.MAX_VALUE` 有 OOM 风险；异步任务无条件 sleep 5 秒。

### 14.3 安全清单（上线前必须处理）

| 风险 | 位置 |
| --- | --- |
| 数据库口令明文写在配置文件 | 两份 `application.yml` 第 13 行 |
| JWT 签名密钥硬编码 | `utils/JWTUtils.java:14` |
| 密码盐硬编码、使用 MD5 + 固定盐（非 BCrypt / 随机盐） | `service/impl/LoginServiceImpl.java:30,61,126` |
| OSS / 七牛 AK/SK 写死占位值 `123`，CDN 域名硬编码 | portal `application.yml:38-43`、`utils/QiniuUtils.java:10` |
| 跨域全放行 + 允许凭据 | `config/WebMVCConfig.java:19-26`、`config/CorsConfig.java` |
| 上传/删除接口无鉴权 | `controller/OssController.java`、`UploadController.java` |
| 生产开启 SQL 控制台打印、dev profile 的 DEBUG 日志 | `application.yml:28-30`、`logback-spring.xml` |
| 全局异常直接 `printStackTrace`，可能泄露堆栈 | `AllExceptionHandler`、`JWTUtils`、`OssServiceImpl` 等 |

建议：把口令/密钥迁移到环境变量或配置中心（项目已引入 jasypt 可直接用 `ENC(...)`），升级密码哈希为 BCrypt，收敛 CORS 白名单，为写操作统一加鉴权与参数校验，并补充 HTTPS、限流与操作审计。

---

## 15. Git 忽略规则

根目录 `.gitignore` 覆盖以下类别，避免把 IDE 配置、构建产物和本地临时文件提交进仓库：

| 类别 | 规则示例 |
| --- | --- |
| IntelliJ IDEA | `.idea/`、`*.iml`、`*.ipr`、`*.iws`、`out/` |
| Maven / 构建产物 | `target/`（匹配任意层级）、`*.class`、`pom.xml.*Backup`、`release.properties`、`dependency-reduced-pom.xml`、`.flattened-pom.xml` |
| Eclipse / STS | `.classpath`、`.project`、`.settings/`、`.springBeans`、`.factorypath`、`bin/` |
| VS Code / 系统 | `.vscode/`、`*.code-workspace`、`.DS_Store`、`Thumbs.db` |
| 日志与临时文件 | `*.log`、`logs/`、`*.tmp`、`*.bak`、`*.swp`、`*~`、`hs_err_pid*` |
| 本地私有配置 | `.env`、`application-local.yml`、`application-*-local.yml` |

`target/` 不带前导斜杠，因此根目录与两个子模块的构建输出都会被忽略；`.idea/` 末尾斜杠只匹配目录。Maven Wrapper 的 `.mvn/wrapper/maven-wrapper.jar` 未被忽略，便于将来提交 Wrapper。

验证忽略是否生效：

```bash
git status --short --ignored
git check-ignore -v .idea/ target/ wblog-portal/target/ wblog-portal/wblog-portal.iml
```

> 当前仓库已跟踪文件共 85 个，全部为源码、POM、配置与 XML；`.idea/`、`target/`、`*.iml` 均只存在于本地磁盘，未被跟踪。

---

## 16. FAQ

**Q：直接 `mvn package` 为什么失败？**
A：`wblog-manage` 无启动类，却被父 POM 的 `spring-boot-maven-plugin` 执行 `repackage`。使用 `mvn -pl wblog-portal -DskipTests clean package`。

**Q：JDK 21 编译报 `JCTree$JCImport.qualid` 怎么办？**
A：切到 JDK 8/11；或升级 Lombok 到兼容 JDK 21 的版本并重新验证。

**Q：启动后接口报数据库连接 / Redis 连接错误？**
A：确认 MySQL 的 `wblog` 库与 7 张 `ms_` 表已创建、Redis 已启动，并核对 `application.yml` 的账号与地址。仓库不含建表脚本。

**Q：为什么 `/upload` 总是返回「上传失败」？**
A：七牛上传实现被注释且恒返回 `false`；请使用 `/oss/file` 并配置有效 OSS 密钥。

**Q：Swagger 地址是什么？**
A：当前未启用，没有 `Docket` Bean / `@EnableOpenApi`，需自行补充配置。

**Q：登录后哪些接口需要 token？**
A：目前仅 `/comments/create/change` 与 `/articles/publish` 被拦截器保护；`/users/currentUser` 与 `/logout` 则要求请求头必须携带 `Authorization`。

**Q：如何参与开发？**
A：建议优先补齐 DDL、单元测试、profile 拆分与密钥外置，再按 §14 的问题清单逐项整改；提交前执行 `mvn -pl wblog-portal -DskipTests package` 验证可构建。

---

## 附注

- 源码中保留了大量 CSDN / 简书 / 博客园参考链接与中文讲解注释，可辅助理解实现思路。
- 项目未声明开源许可证；如需对外分发，请先补充 `LICENSE`。
- 本文档仅描述仓库当前状态，不构成对生产可用性的承诺。
