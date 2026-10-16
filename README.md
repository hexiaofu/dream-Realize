# 圆梦社区

uni-app 前端 + Spring Boot 后端的梦想互助社区。作者：**沙里飞**，邮箱：**1499058156@qq.com**，公司：**青海老铁软件有限公司**。

## 目录与技术栈

```text
frontend/   uni-app + Vue 3 + Wot Design Uni，支持 H5 / 微信小程序构建
  admin/    独立 Vue 3 + Element Plus 管理后台
backend/    Spring Boot 2.7.18 + Java 8 + Spring JDBC + MySQL 5.7
```

UI 采用 [Wot Design Uni](https://netlify.wot-design-uni.cn/guide/quick-use) 的导航、标签栏、卡片、表单、选择器、弹窗、提示、按钮、进度条等组件。自定义样式集中于 `frontend/src/App.vue` 的页面布局与排版，颜色通过 `AppShell.vue` 的 `wd-config-provider` 主题变量设置。没有另写一套 UI 控件。空状态图片来自 `wot-design-uni-assets`，随项目本地打包，许可保留于 `frontend/src/static/status/LICENSE`。

产品主线是自由表达梦想、发现可以提供的帮助。现金仅为可选帮助类型之一，金额字段仅在主动选择该类型时出现；首页与默认故事围绕生活、学习、创作和陪伴展开。

[Spring Boot 2.7.18 支持 Java 8](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html)。构建推荐 Maven 3.9.x、Node.js 20 与 npm。uni-app 依赖按官方模板的同一版本固定，`package-lock.json` 用于重复安装。

## 已实现的流程

- 8 道情境入门题：服务端评分、逐题反馈、全部理解后注册，凭证有效期 30 分钟且仅能使用一次。
- 昵称注册、BCrypt 密码、随机 Bearer 会话（7 天）、退出会话失效。
- 梦想分类、关键词和帮助类型筛选、分页、公开圆梦故事。
- 发布梦想，故事选填；支持现金、物品、技能、经验、陪伴、资源及其他帮助。
- 发布梦想和记录进展时上传图片、视频；图片放大预览、视频播放与拖动、上传进度、失败重试、移除附件、发布草稿恢复。
- 每条梦想独立选择昵称或匿名；匿名身份贯穿广场、详情、帮助会话与消息，公开个人名片排除匿名梦想。
- 提倡无偿帮助，支持双方协商条件；支持线上和线下交流，资金交付由双方自行安排。
- 帮助意愿、接受 / 婉拒 / 撤回、双方私信（页面可见时每 5 秒刷新）、历史消息分页。
- 发布者记录进展、更新自行填报的现金金额、标记进行中 / 已圆梦 / 暂停，也可恢复。
- 我的梦想、公开名片、技能与资源介绍；统一默认头像，可编辑头像、昵称、城市、简介和能提供的帮助。
- 独立后管：梦想与进展查询、原文和媒体复核、下架 / 恢复、账号限发 / 封禁 / 解除、限时自动恢复、操作日志。

私信目前支持文本；消息页展示最近 100 个会话。当前内容处理由管理员人工操作；自动识别、举报受理、推送通知、支付托管和原生安装包尚未实现。

## 管理后台

界面使用 [Element Plus](https://element-plus.org/en-US/component/table.html) 的表格、筛选、分页、抽屉、对话框和统计组件，源码位于 `frontend/admin/`，不混入用户端登录。

首次部署时，在启动后端前设置独立管理员账号：

```powershell
$env:ADMIN_USERNAME = '你的管理员账号'
$env:ADMIN_PASSWORD = '你设置的管理员密码'
# 生产环境使用 HTTPS 时设置为 true
$env:ADMIN_COOKIE_SECURE = 'true'
```

管理员账号需为 4–32 位字母、数字或下划线；密码为 12–64 位，UTF-8 不超过 72 字节。仅当 `administrators` 表为空时初始化，不会把已有普通用户提升为管理员，也不会在重启时重置密码。没有配置时不创建默认管理员。初始化成功后可移除密码环境变量。HTTP 本地开发应保持 `ADMIN_COOKIE_SECURE=false`。

```powershell
cd frontend/admin
npm ci
npm run dev
# 构建后台
npm run build
```

访问 `http://127.0.0.1:18174`，生产产物位于 `frontend/admin/dist/`。管理后台与 API 应通过同一站点提供，将 `/api` 代理到后端；用户端和后台可以部署为两个站点。后台使用限定到 `/api/admin` 的独立 HttpOnly、SameSite=Strict Cookie 会话，8 小时有效；普通用户 Bearer 会话不能访问后台接口。写操作还要求 `X-Admin-Request: 1` 请求头，管理员密码及会话令牌不保存在浏览器 localStorage。

当前本地预览的管理员凭证保存在 `.runtime/admin-access.json`（已被 Git 忽略），不是源码中的默认账号。

处理规则：

- **内容下架**：梦想或单条进展可以独立下架、恢复。梦想下架后从公开列表、搜索、圆梦故事、名片、相关帮助交流入口移除，详情与媒体旧链接返回 404；发布者在“我的”中能看到下架原因。恢复梦想不会恢复之前单独下架的进展。
- **限制发布**：仍可登录浏览；禁止发布梦想、更新进展、上传、帮助操作、发消息和修改资料。
- **封禁登录**：禁止登录，并立即注销已有用户会话。解除后需要重新登录；不会自动下架既有内容，违规内容需另行处理。
- **限制时长**：界面提供 1 天、7 天、30 天或长期；到期在服务端请求校验时自动失效，无需定时任务。也可手动解除。
- **记录与复核**：每次下架、恢复、限制或解除都必须填写原因，记录管理员、对象、时间与期限；不提供删除审计日志的接口。后台可以查看匿名内容对应的账号，普通用户接口继续隐藏匿名作者身份。

下架媒体对后续请求生效，响应使用 `Cache-Control: no-store`；已经被保存或下载的文件无法远程撤回。后台的受保护媒体接口要求管理员 Cookie，支持审核图片和视频。

## 个人资料与头像

在“我的 → 编辑资料”修改头像、昵称、城市、简介和技能，点击“保存”后生效；直接关闭编辑不会修改已保存资料。头像支持相册或拍摄选择图片、上传进度、失败重试和恢复默认。未设置头像、匿名身份或头像加载失败时，使用 Wot 内置圆形用户图标，不依赖外部图片服务。

头像与昵称同步到公开名片、梦想列表和详情、帮助列表及对话。匿名作者在这些接口中始终不返回真实头像。头像仅允许使用本人上传的未发布图片，沿用 10 MB 图片限制；正在使用的头像不能被删除或绑定到梦想附件。限制发布的账号不能修改资料或上传头像。

头像关联保存在新增的 `member_avatars` 表；服务重启自动创建缺失的表，现有用户自然使用默认头像，无需清库或改动原有成员数据。

## 图片与视频

上传使用 [Wot Upload](https://netlify.wot-design-uni.cn/component/upload) 和 [uni.uploadFile](https://uniapp.dcloud.net.cn/api/request/network-file)，需要先登录。

- 每条梦想、每次进展可附 9 张图片和 1 个视频。图片支持 JPG / PNG / GIF / WebP，每张最多 10 MB；视频支持 MP4 / WebM，每个最多 100 MB。
- 上传失败可重试或移除；上传未完成或有失败项时不能提交。发布页草稿保留已上传附件，刷新后可以继续。
- 后端校验文件类型、大小及附件所有者，附件只能绑定一次；匿名响应不包含上传者身份或原始文件名。图片与视频的内容本身由发布者提供。
- 文件保存在 `backend/uploads/`，可用环境变量 `MEDIA_DIR` 指定绝对路径；元数据保存到 MySQL 的 `media_files` 表。部署时需要持久化此目录，并与数据库一起备份。
- 文件通过不可预测的随机地址访问，持有地址即可读取，包括尚未发布的上传。移除未发布附件会删除文件；已经发布的附件不能从上传区删除。中途离开、清空草稿或取消头像编辑后遗留的未绑定文件暂不自动清理；制定保留策略时应同时满足 `dream_id IS NULL` 且未被 `member_avatars.media_id` 引用，避免清理正在使用的头像。
- 视频按原文件存储，不做转码；跨设备优先使用 MP4（H.264 / AAC），WebM 能否播放取决于设备。后端支持 HTTP Range，供播放器分段加载、拖动进度。

反向代理需放行至少 101 MB 请求体并配置足够的上传超时。微信小程序还需配置 uploadFile / downloadFile 合法域名，以及相册、拍摄相关隐私接口；构建成功不代表已完成真机权限验证。

## 启动后端：MySQL 5.7

本机已创建 `127.0.0.1:3306/dream_realize`，使用 `utf8mb4` 编码。现有预览账号、梦想和管理员数据已迁入，当前后端连接此库。提供的 MySQL 5.7.26 客户端位于 `D:/phpstudy_pro/Extensions/MySQL5.7.26/bin/mysql.exe`；3306 上实际运行的服务端版本为 5.7.9。

本机连接信息保存在 `backend/config/application-local.yml`（已排除 Git）。在 `backend` 目录下使用 `local` profile 启动即可加载：

```powershell
cd backend
java -jar target/dream-realize-1.0.0.jar --spring.profiles.active=local
# 或通过 Maven 启动源码
mvn spring-boot:run '-Dspring-boot.run.profiles=local'
```

IDE 启动时也可指定 `--spring.profiles.active=local`，工作目录设为 `backend`。迁移前的数据快照保存在本机忽略目录 `.runtime/mysql-before-3306-migration.json`；原 3307 演示库仍保留，业务后端已不再连接它。

其他环境部署时：

先在自己的 MySQL 实例中新建 UTF-8 数据库：

```sql
CREATE DATABASE dream_realize CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

设置环境变量，进入 `backend` 启动（以下为 PowerShell）：

```powershell
$env:JAVA_HOME = '你的 JDK 8 目录'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/dream_realize?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
$env:DB_USERNAME = '你的数据库用户名'
$env:DB_PASSWORD = '你的数据库密码'
cd backend
mvn spring-boot:run
```

默认端口 `18080`，健康接口 `GET /api/health`。启动时 `schema.sql` 自动创建缺失的表，不清空数据。所有表名与 SQL 兼容 MySQL 5.7，无 MySQL 8 专属语法。按需要设置 `SERVER_PORT` 和 `CORS_ORIGINS`（逗号分隔的前端来源）。部署应使用独立数据库账号与环境变量，密码不写进源码。

## 无需 MySQL 的本地演示

```powershell
cd backend
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

此模式使用本地 H2 文件数据库 `backend/data/`，空库首次启动写入示例内容。演示账号：

| 账号 | 密码 | 用途 |
| --- | --- | --- |
| dreamer | Dream12345! | 查看自己的梦想、接收帮助 |
| helper | Dream12345! | 提供帮助、双方交流 |

演示数据仅在 `demo` profile 或显式设置 `app.demo-data=true` 时启用。默认 MySQL 模式不创建演示账号。

## 启动前端

```powershell
cd frontend
npm ci
npm run dev:h5
```

浏览器打开 `http://127.0.0.1:18173`。开发服务器将 `/api` 转发到 `127.0.0.1:18080`。

HBuilderX 可导入 `frontend` 目录。微信小程序需在 `src/manifest.json` 填入自己的 AppID。真机 / 小程序调用后端时，在 `frontend/.env.local` 设置 `VITE_API_BASE_URL=https://你的接口域名/api`；正式微信环境还需配置 request 合法域名。手机不能用电脑的 `127.0.0.1` 地址。

```powershell
npm run build:h5
npm run build:mp-weixin
```

产物分别位于 `frontend/dist/build/h5` 和 `frontend/dist/build/mp-weixin`。H5 使用 hash 路由；生产环境将 `/api` 反向代理到后端或在构建前指定 API 地址。微信构建通过不代表已完成真机、登录平台或发布审核。

## 测试

后端接口测试默认使用隔离的 H2 内存数据库：

```powershell
cd backend
mvn test
```

也可在独立的 MySQL 5.7 测试库执行相同测试，测试操作在事务结束时回滚：

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/dream_realize_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
$env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
$env:TEST_DB_USERNAME = '你的测试库用户'
$env:TEST_DB_PASSWORD = '你的测试库密码'
mvn test
```

前端浏览器测试需先启动后端和 H5 开发服务，会创建带随机名称的测试成员和梦想，请连接独立演示库：

```powershell
cd frontend
npx playwright install chromium
npm run test:e2e
```

可通过 `PLAYWRIGHT_BASE_URL`、`API_TEST_URL` 指定测试服务，`PW_EXECUTABLE_PATH` 指定已有 Chromium 可执行文件。后端测试覆盖考试凭证、匿名身份、公开名片、越权访问、帮助状态转换、金额校验、搜索、退出失效、媒体上传、附件归属与视频分段读取；媒体测试文件写入独立临时目录并在结束后清理。浏览器测试覆盖注册、发布、帮助交流、完成梦想，以及图片视频的重试、移除、草稿恢复与实际播放。

## 接口约定

返回格式：`{ "code": 0, "message": "ok", "data": ... }`；失败使用对应 HTTP 状态码和可读提示。登录后请求头使用 `Authorization: Bearer <token>`。

| 接口 | 用途 |
| --- | --- |
| `GET /api/admission/questions` | 获取入门题（不下发标准答案） |
| `POST /api/admission/check` | 提交 `answers`，返回反馈与注册凭证 |
| `POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout` | 账户会话 |
| `GET/PUT /api/me` | 当前用户名片 |
| `GET /api/members/{id}` | 公开名片，仅包含非匿名梦想 |
| `GET /api/stats` | 梦想与帮助数量 |
| `GET/POST /api/dreams` | 检索 / 发布梦想 |
| `GET /api/dreams/{id}` | 梦想详情与足迹 |
| `POST /api/dreams/{id}/updates` | 发布者更新进展与状态 |
| `POST /api/media` | multipart 上传，字段 `file` 和 `kind`（`IMAGE` / `VIDEO`），需要登录 |
| `GET /api/media/{随机文件名}` | 读取图片 / 视频，支持 Range |
| `DELETE /api/media/{id}` | 上传者移除尚未发布的附件 |
| `POST /api/dreams/{id}/helps` | 提交帮助意愿 |
| `GET /api/helps`、`GET/PATCH /api/helps/{id}` | 双方申请与状态管理 |
| `GET/POST /api/helps/{id}/messages` | 双方消息，`before` 查询更早记录 |
| `POST /api/admin/login`、`POST /api/admin/logout`、`GET /api/admin/me` | 独立管理员会话 |
| `GET /api/admin/overview` | 后台统计 |
| `GET /api/admin/contents`、`GET/PATCH /api/admin/contents/{type}/{id}` | `DREAM` / `UPDATE` 查询、详情、下架与恢复 |
| `GET /api/admin/members`、`PATCH /api/admin/members/{id}/restriction` | 账号查询、限制与解除 |
| `GET /api/admin/logs` | 分页查看操作日志 |
| `GET /api/admin/media/{随机文件名}` | 管理员访问已下架媒体 |

`GET /api/dreams` 支持 `keyword/category/helpType/status/page/size/mine`，`size` 为 1–30。匿名字段在后端投影时移除，不依赖前端隐藏。所有内部账户主键、密码哈希和会话摘要均不会作为匿名作者信息下发。

上传成功返回 `{ id, type, url, contentType, size }`，其中 `url` 为 `/api/media/` 下的相对路径。发布梦想或进展时，将这些 `id` 放入 `mediaIds` 数组；列表、详情与进展响应中的 `media` 数组用于渲染附件。

`PUT /api/me` 接收 `{ nickname, city, bio, skills, avatarId }`。`avatarId` 使用图片上传返回的 ID；空字符串恢复默认，省略或 `null` 保留现有头像。`GET /api/me` 和登录响应的 `user` 返回 `avatarId`、`avatarUrl`，公开名片只返回 `avatarUrl`。默认头像对应空 URL，由前端统一呈现；梦想使用 `authorAvatar`，帮助会话使用 `partnerAvatar`，消息使用 `senderAvatar`，匿名作者均返回空 URL。

后台内容处理提交 `{ hidden, expectedHidden, reason }`；账号限制提交 `{ mode, durationHours, expectedRevision, reason }`，`durationHours=null` 表示长期。旧状态或版本过期返回 409，避免覆盖其他管理员的操作。`/api/me` 中的 `restriction` 为当前有效限制；用户端直接调用写接口也会受到同样限制。

Java 的顶层类、嵌套入参类、测试类均包含作者、邮箱和公司注释；Vue/JS 源码也带有同样的署名。

## 本次验证（2026-10-16）

- Java 8（1.8.0_291）编译与运行，生成的 class 主版本为 52。
- H2 与独立 MySQL 5.7.9 实例分别通过 21 组后端集成测试，覆盖后管权限、内容下架、账号限制，以及头像持久化、归属校验、默认恢复、昵称同步和匿名头像保护。
- 浏览器测试覆盖 5 组用户端流程（包含头像上传重试、资料保存、刷新与重新登录、取消编辑、断图回退、恢复默认），以及 1 组桌面后管下架恢复、限发封禁、解除、媒体复核与日志流程。后管测试需启动后台服务，提供 `ADMIN_TEST_USERNAME`、`ADMIN_TEST_PASSWORD`，或使用本地 `.runtime/admin-access.json`；可用 `ADMIN_TEST_URL` 指定后台地址。未配置账号时该用例跳过。
- H5、微信小程序、独立后台构建成功；后端可执行 JAR 打包成功。
- Java 类（含嵌套类与测试类）保留作者、邮箱、公司注释。
- 微信真机与原生安装包尚未验证。
