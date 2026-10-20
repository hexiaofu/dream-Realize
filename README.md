# 圆梦社区

uni-app 前端 + Spring Boot 后端的梦想互助社区。作者：**沙里飞**，邮箱：**1499058156@qq.com**，公司：**青海老铁软件有限公司**。

## 目录与技术栈

```text
frontend/   uni-app + Vue 3 + Wot Design Uni，支持 H5 / 微信小程序构建
admin/      独立 Vue 3 + Element Plus 管理后台
backend/    Spring Boot 2.7.18 + Java 8 + Spring JDBC + MySQL 5.7
```

UI 采用 [Wot Design Uni](https://netlify.wot-design-uni.cn/guide/quick-use) 的导航、标签栏、卡片、表单、选择器、弹窗、提示、按钮、进度条等组件。自定义样式集中于 `frontend/src/App.vue` 的页面布局与排版，颜色通过 `AppShell.vue` 的 `wd-config-provider` 主题变量设置。没有另写一套 UI 控件。空状态图片来自 `wot-design-uni-assets`，随项目本地打包，许可保留于 `frontend/src/static/status/LICENSE`。

产品主线是自由表达梦想、发现可以提供的帮助。现金仅为可选帮助类型之一，金额字段仅在主动选择该类型时出现；首页与默认故事围绕生活、学习、创作和陪伴展开。

[Spring Boot 2.7.18 支持 Java 8](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html)。构建推荐 Maven 3.9.x、Node.js 20 与 npm。uni-app 依赖按官方模板的同一版本固定，`package-lock.json` 用于重复安装。

## 已实现的流程

- 注册无需答题；登录后通过 10 道评论资格问答，每题 10 分，达到 90 分后获得持续有效的评论资格。
- 昵称注册、BCrypt 密码、随机 Bearer 会话（7 天）、退出会话失效。
- 注册时必须打开免责声明全文并点击“阅读完成”，再主动勾选同意后注册；服务端校验同意信息并保存所同意的文本版本和时间。
- 梦想分类、关键词和帮助类型筛选、分页、公开圆梦故事。
- 发布梦想，故事选填；支持现金、物品、技能、经验、陪伴、资源及其他帮助。
- 发布梦想和记录进展时上传图片、视频；图片放大预览、视频播放与拖动、上传进度、失败重试、移除附件、发布草稿恢复。
- 每条梦想独立选择昵称或匿名；匿名身份贯穿广场、详情、帮助会话与消息，公开个人名片排除匿名梦想。
- 提倡无偿帮助，支持双方协商条件；支持线上和线下交流，资金交付由双方自行安排。
- 帮助意愿、接受 / 婉拒 / 撤回、双方帮助交流（页面可见时每 5 秒刷新）、历史消息分页。
- 成员私信：在他人的公开名片点“发私信”，一对成员共用一个会话；“消息 → 私信”展示最近会话、最后一条消息和未读数，聊天页每 5 秒刷新。匿名梦想没有名片入口，不会因私信暴露身份；限制发布的账号可以阅读但不能发送或发起新会话。
- 发布者记录进展、更新自行填报的现金金额、标记进行中 / 已圆梦 / 暂停，也可恢复。
- 我的梦想、公开名片、技能与资源介绍；统一默认头像，可编辑头像、昵称、城市、简介和能提供的帮助。
- 梦想可点击“精神支持”或“不同意”，一人一票、互斥切换和取消；先表态才能评论，评论显示当前态度，支持公开/匿名、分页和删除自己的评论，后台可下架或恢复评论。
- 意见反馈：“我的 → 意见反馈”选择类型（功能建议 / 问题反馈 / 内容与体验 / 其他）、填写内容和可选联系方式；同页查看自己最近 20 条反馈的处理状态与官方回复。需要登录，限制发布的账号也可以反馈，每个账号每小时最多 5 条。
- 独立后管：梦想与进展查询、原文和媒体复核、下架 / 恢复、账号限发 / 封禁 / 解除、限时自动恢复、意见反馈处理、操作日志。

帮助交流与私信目前支持文本；消息页各展示最近 100 个会话。私信暂未接入后台审核与举报。当前内容处理由管理员人工操作；自动识别、举报受理、推送通知、支付托管和原生安装包尚未实现。

## 管理后台

界面使用 [Element Plus](https://element-plus.org/en-US/component/table.html) 的表格、筛选、分页、抽屉、对话框和统计组件，源码位于 `admin/`，不混入用户端登录。

首次部署时，在启动后端前设置独立管理员账号：

```powershell
$env:ADMIN_USERNAME = '你的管理员账号'
$env:ADMIN_PASSWORD = '你设置的管理员密码'
# 生产环境使用 HTTPS 时设置为 true
$env:ADMIN_COOKIE_SECURE = 'true'
```

管理员账号需为 4–32 位字母、数字或下划线；密码为 12–64 位，UTF-8 不超过 72 字节。仅当 `administrators` 表为空时初始化，不会把已有普通用户提升为管理员，也不会在重启时重置密码。没有配置时不创建默认管理员。初始化成功后可移除密码环境变量。HTTP 本地开发应保持 `ADMIN_COOKIE_SECURE=false`。

```powershell
cd admin
npm ci
npm run dev
# 构建后台
npm run build
```

访问 `http://127.0.0.1:18174`，生产产物位于 `admin/dist/`。管理后台与 API 应通过同一站点提供，将 `/api` 代理到后端；用户端和后台可以部署为两个站点。后台使用限定到 `/api/admin` 的独立 HttpOnly、SameSite=Strict Cookie 会话，8 小时有效；普通用户 Bearer 会话不能访问后台接口。写操作还要求 `X-Admin-Request: 1` 请求头，管理员密码及会话令牌不保存在浏览器 localStorage。

当前本地预览的管理员凭证保存在 `.runtime/admin-access.json`（已被 Git 忽略），不是源码中的默认账号。

意见反馈：后台“意见反馈”菜单按状态、类型和关键词筛选，待处理的排在最前，菜单与概览显示待处理数量。点“处理”可查看全文、提交账号和联系方式，设置“待处理 / 处理中 / 已处理”并填写回复；回复会展示在用户的“我的 → 意见反馈”中。每次处理都会写入操作日志，并发修改以状态校验避免覆盖（返回 409）。

处理规则：

- **内容下架**：梦想、单条进展和评论可以独立下架、恢复。梦想下架后从公开列表、搜索、圆梦故事、名片、相关帮助交流入口移除，详情与媒体旧链接返回 404；评论读取、发表评论和表态也会被拒绝。发布者在“我的”中能看到下架原因。恢复梦想不会恢复之前单独下架的进展或评论。
- **限制发布**：仍可登录浏览；禁止发布梦想、更新进展、上传、帮助操作、表态、评论、发消息和修改资料。
- **封禁登录**：禁止登录，并立即注销已有用户会话。解除后需要重新登录；不会自动下架既有内容，违规内容需另行处理。
- **限制时长**：界面提供 1 天、7 天、30 天或长期；到期在服务端请求校验时自动失效，无需定时任务。也可手动解除。
- **记录与复核**：每次下架、恢复、限制或解除都必须填写原因，记录管理员、对象、时间与期限；不提供删除审计日志的接口。后台可以查看匿名内容对应的账号，普通用户接口继续隐藏匿名作者身份。

下架媒体对后续请求生效，响应使用 `Cache-Control: no-store`；已经被保存或下载的文件无法远程撤回。后台的受保护媒体接口要求管理员 Cookie，支持审核图片和视频。

## 个人资料与头像

在“我的 → 编辑资料”修改头像、昵称、城市、简介和技能，点击“保存”后生效；直接关闭编辑不会修改已保存资料。头像支持相册或拍摄选择图片、上传进度、失败重试和恢复默认。未设置头像、匿名身份或头像加载失败时，使用 Wot 内置圆形用户图标，不依赖外部图片服务。

头像与昵称同步到公开名片、梦想列表和详情、帮助列表及对话。匿名作者在这些接口中始终不返回真实头像。头像仅允许使用本人上传的未发布图片，沿用 10 MB 图片限制；正在使用的头像不能被删除或绑定到梦想附件。限制发布的账号不能修改资料或上传头像。

头像关联保存在新增的 `member_avatars` 表；服务重启自动创建缺失的表，现有用户自然使用默认头像，无需清库或改动原有成员数据。

## 精神支持、不同意和评论

梦想卡片和详情提供“精神支持”“不同意”按钮。每个登录用户对同一梦想仅能保留一种态度，再点已选按钮会取消，选择另一种会切换。访客可以看数量，点击表态或发表评论时进入登录。服务端以成员、梦想联合主键保证一人一票，接口提交目标状态，重复或并发请求不会多计票。

发表评论须同时满足：已登录、通过评论资格问答（至少 90 分）、已对当前梦想选择“精神支持”或“不同意”、账号未受限且梦想未下架，前后端均校验。评论区提供“去答题”入口，问答完成后可返回原梦想继续评论。“我的 → 评论资格”也可进入问答或查看通过状态。

问答共 10 题，每题 10 分，答对 9 题即可通过；80 分及以下可根据反馈重新作答。服务端只接收答案和试卷版本，自行计分，不接受客户端指定成绩或资格。首次通过的成绩、试卷版本及时间保存在 `comment_qualifications` 表并绑定登录账号，重复提交不重复发放资格；后续答题失误不撤销已有资格。通过一次后跨梦想、刷新页面、重新登录均有效，账号限制仍优先执行。现有账号没有可验证的评论资格记录时需完成新问答；旧 `admission_passes` 表保留历史数据，但不再签发或接受注册凭证。

评论旁展示作者对该梦想的当前态度，切换表态会同步更新，取消后显示“未表态”并禁止继续发表评论；已有评论保留，即使尚未取得新评论资格，仍可删除自己的评论。

评论默认使用昵称，可切换匿名，最多 1000 字。匿名梦想的作者发表评论时始终匿名，服务端同时移除真实昵称、头像和成员编号。评论按最新优先分页，作者可以删除自己的评论。删除后从公共列表和计数移除，原文仅供管理员复核，不能通过后台“恢复”重新公开。

评论在底部弹层展示，输入区提供“表情、图片、收藏”。每条可带 9 张图片（每张 10 MB，JPG/PNG/GIF/WebP），支持纯图片评论；GIF 上传原图并保留动画。字数按 Unicode 字符统计，上限 1000，避免截断表情。附件上传失败可重试，全部上传成功后才能发送，关闭弹层保留本次草稿。

图片下方或上传面板可点“收藏”，在“收藏”面板选择后再次发送，也能放大预览和移除收藏。收藏绑定个人账号、跨登录保留，上限 500 项。收藏和多条评论引用同一文件，移出草稿或取消收藏不会误删已发布的图片。首次发布图片的来源评论被删除，或任一引用该图片的评论/梦想被审核下架时，旧链接、收藏预览与再次发送均不可用；仅删除转发评论不影响原图。后台评论详情可查看图片/GIF，包括已下架的原图。新增 `comment_attachments`、`media_favorites` 两张表，随启动自动创建，保留已有数据。

后台“内容管理 → 评论”可按关键词和可见状态查找、查看原文、下架/恢复，并记录原因与操作日志。被下架或已删除的评论不计入评论总数。新增 `dream_reactions`、`dream_comments`、`comment_moderation` 三张表，启动时自动创建，兼容已有 MySQL 5.7 数据库。

## 注册声明与同意记录

注册页使用 Wot 卡片、弹窗和复选框展示《免责声明与线下交流风险告知》：重点提示默认可见，复选框默认不勾选且不可选，打开全文并点击底部“阅读完成”后才可勾选。查看全文不会自动勾选同意。声明加载失败时可重试，但不能继续注册；页面打开期间声明更新时，阅读状态及原勾选会清除，需重新阅读确认，账号输入保留。“我的”提供当前声明的长期查看入口，访客也可阅读。

声明覆盖身份与履约判断、线下见面、私下转账、有偿帮助、未成年人、个人信息、投诉联系和责任边界。运营方为青海老铁软件有限公司，联系邮箱为 `1499058156@qq.com`。条款参考[《中华人民共和国民法典》](https://www.court.gov.cn/zixun/xiangqing/233181.html)第 496、497、506、1195、1197 条，对主要风险和责任条款作显著提示，保留平台法定义务及用户依法寻求救济的权利。声明不替代隐私政策，正式上线前应由法律专业人员结合实际运营复核。

完整文本唯一来源为 `backend/src/main/resources/legal/registration-notice-v2026-10-16.json`。更新条款应新增版本文件，并更新 `RegistrationNoticeService` 的资源路径及文本中的版本、生效日期；前端直接从服务端读取，避免两端文本不一致。摘要按规范序列化后的完整 JSON 文本计算 SHA-256。新表 `registration_consents` 自动创建，保存成员编号、版本、摘要、文本快照及服务端时间，与注册账号和会话创建处于同一事务中。现有账号及演示账号不补造同意记录，登录不受影响；本次仅对新注册执行确认，后续若要面向老用户征求同意，需要另行明确触发流程。

## 隐私政策

“我的 → 协议与说明”提供《圆梦隐私政策》，访客也可阅读。全文位于 `frontend/src/lib/privacy.js`，与注册声明使用同一套文档样式渲染（`pages/legal/privacy.vue`），内容按本项目实际收集的数据编写：账号与加密密码、个人资料、发布与互动内容、媒体文件、同意与审核记录，以及匿名规则和相机、相册、麦克风权限。更新条款时修改其中的版本与生效日期。

隐私政策目前仅供查看，注册时尚未要求单独勾选同意。上架应用商店前通常需要在注册页同时展示并征得同意，并由法律专业人员结合实际部署（服务器所在地、是否接入第三方 SDK 等）复核。

## 图片与视频

上传使用 [Wot Upload](https://netlify.wot-design-uni.cn/component/upload) 和 [uni.uploadFile](https://uniapp.dcloud.net.cn/api/request/network-file)，需要先登录。

- 每条梦想、每次进展可附 9 张图片和 1 个视频。图片支持 JPG / PNG / GIF / WebP，每张最多 10 MB；视频支持 MP4 / WebM，每个最多 100 MB。
- 上传失败可重试或移除；上传未完成或有失败项时不能提交。发布页草稿保留已上传附件，刷新后可以继续。
- 后端校验文件类型、大小及附件所有者，附件只能绑定一次；匿名响应不包含上传者身份或原始文件名。图片与视频的内容本身由发布者提供。
- 文件保存在 `backend/uploads/`，可用环境变量 `MEDIA_DIR` 指定绝对路径；元数据保存到 MySQL 的 `media_files` 表。部署时需要持久化此目录，并与数据库一起备份。
- 文件通过不可预测的随机地址访问，持有地址即可读取，包括尚未发布的上传。移除未发布附件会删除文件；已经发布的附件不能从上传区删除。中途离开、清空草稿或取消头像编辑后遗留的未绑定文件暂不自动清理；制定保留策略时应同时满足 `dream_id IS NULL` 且未被 `member_avatars.media_id`、`comment_attachments.media_id` 或 `media_favorites.media_id` 引用，避免清理正在使用的头像。
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

## 打包移动端 App（Android / iOS）

`frontend` 是 uni-app（Vue 3 + Vite）CLI 工程，同一份代码可编译为 App。`src/manifest.json` 的 `app-plus` 已配置相机、相册与视频模块，Android 权限及 iOS 隐私说明（相机、相册、麦克风），均用于发布梦想、头像和评论附件。

```powershell
cd frontend
# 在 .env.local 中设置手机可访问的接口地址，例如 VITE_API_BASE_URL=https://你的接口域名/api
npm run build:app
```

产物位于 `frontend/dist/build/app`，用 HBuilderX 导入后选择“发行 → 原生 App-云打包”，或用 Android Studio / Xcode 做离线打包。打包前需要：

- 在 HBuilderX 登录 DCloud 账号并为 `manifest.json` 生成 `appid`（云打包必需）。
- 配置应用图标与启动图（HBuilderX 的 manifest 可视化界面可一键生成各尺寸）。
- Android 准备签名证书（keystore）；iOS 需要 Apple 开发者账号、证书与描述文件。
- 接口地址不能是 `127.0.0.1`（那是手机自己）。正式环境请使用 HTTPS 域名；反向代理需放行至少 101 MB 上传请求体。

`npm run dev:app` 会持续编译到 `dist/dev/app`，配合 HBuilderX“运行到手机或模拟器”进行真机调试。构建通过不代表已完成真机权限、上架审核验证。

## 部署到服务器（www.qhlaotie.cn/dream）

正式包的接口统一走 `https://www.qhlaotie.cn/dream`：Nginx 把 `/dream/` 转发到后端并去掉这个前缀，后端本身仍然提供 `/api/...`，无需改动接口路径。

- 用户端：`frontend/.env.production` 设置 `VITE_API_BASE_URL=https://www.qhlaotie.cn/dream/api`，`npm run build:h5 / build:mp-weixin / build:app` 与 HBuilderX 发行都会使用它；本地 `npm run dev:*` 和 HBuilderX 运行不受影响。图片、视频地址同样经过这个前缀。
- 管理后台：`admin/.env.production` 设置 `VITE_API_PREFIX=/dream`，打包后请求 `/dream/api/admin`。后台产物使用相对路径，可放在任意目录（例如 `/dream-admin/`）。
- 后端环境变量：`ADMIN_COOKIE_PATH=/dream/api/admin`（让后台登录 Cookie 与浏览器路径一致，否则后台无法保持登录）、`ADMIN_COOKIE_SECURE=true`、`CORS_ORIGINS=https://www.qhlaotie.cn`（H5 部署在其他域名时改为对应地址，可用逗号分隔多个），以及数据库 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 和 `MEDIA_DIR`。

数据库初始化：`backend/sql/init-mysql.sql` 是完整的 MySQL 5.7+ 初始化脚本，创建 `dream_realize` 库（utf8mb4）与全部 25 张表，可重复执行、不覆盖已有数据，也不含任何账号数据；管理员由后端首次启动时根据 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 创建。执行 `mysql -u root -p < backend/sql/init-mysql.sql` 即可；后端启动时同样会用 `schema.sql` 补建缺失的表，两份建表语句由测试 `InitScriptConsistencyTest` 保证一致。

Nginx 配置：完整配置见 [deploy/nginx/dream.conf](deploy/nginx/dream.conf)，放到 `http {}` 中引用（如 `/etc/nginx/conf.d/dream.conf`），只需按实际修改证书路径、静态文件目录和后端端口。它包含 HTTP 跳转 HTTPS、`/dream/` 去前缀转发到后端、101 MB 上传上限（流式转发）、视频 Range 透传与 300 秒超时、管理后台 `/dream-admin/` 与 H5 `/dream-h5/` 静态目录（带哈希的资源缓存 30 天）。修改后执行 `nginx -t` 校验再 `nginx -s reload`。

H5 使用 hash 路由并以相对路径加载资源，可直接放在子目录。若站点暂未配置 HTTPS，把 `.env.production` 中的 `https` 改为 `http` 并保持 `ADMIN_COOKIE_SECURE=false`；微信小程序正式版必须使用 HTTPS，并在小程序后台把 `https://www.qhlaotie.cn` 配置为 request、uploadFile、downloadFile 合法域名。

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

可通过 `PLAYWRIGHT_BASE_URL`、`API_TEST_URL` 指定测试服务，`PW_EXECUTABLE_PATH` 指定已有 Chromium 可执行文件。后端测试覆盖声明注册、评论资格分数边界与权限、匿名身份、公开名片、越权访问、帮助状态转换、金额校验、搜索、退出失效、媒体上传、附件归属与视频分段读取；媒体测试文件写入独立临时目录并在结束后清理。浏览器测试覆盖注册、评论问答、发布、帮助交流、完成梦想，以及图片视频的重试、移除、草稿恢复与实际播放。

## 接口约定

返回格式：`{ "code": 0, "message": "ok", "data": ... }`；失败使用对应 HTTP 状态码和可读提示。登录后请求头使用 `Authorization: Bearer <token>`。

| 接口 | 用途 |
| --- | --- |
| `GET /api/admission/questions` | 获取 10 道评论资格题、版本及 90 分门槛（不下发标准答案） |
| `GET /api/admission/status` | 登录用户查看自己的评论资格、首次通过成绩和时间 |
| `POST /api/admission/check` | 登录用户提交 `answers` 和 `examVersion`，返回本次成绩、反馈及账号评论资格 |
| `GET /api/legal/registration-notice` | 公开获取注册声明全文、版本及 SHA-256 摘要 |
| `POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout` | 账户会话 |
| `GET/PUT /api/me` | 当前用户名片 |
| `GET /api/members/{id}` | 公开名片，仅包含非匿名梦想 |
| `GET /api/stats` | 梦想与帮助数量 |
| `GET/POST /api/dreams` | 检索 / 发布梦想 |
| `GET /api/dreams/{id}` | 梦想详情与足迹 |
| `POST /api/dreams/{id}/updates` | 发布者更新进展与状态 |
| `PUT /api/dreams/{id}/reaction` | 登录用户表态，`reaction` 为 `SUPPORT` / `DISAGREE` / `NONE` |
| `GET/POST /api/dreams/{id}/comments` | 分页读取 / 发表评论，发表字段 `content`、`anonymous`、`mediaIds`；文字和图片至少有一项 |
| `GET /api/media-favorites` | 登录用户自己的图片/GIF 收藏，支持 `page/size` 分页 |
| `PUT/DELETE /api/media-favorites/{id}` | 收藏 / 移除收藏；收藏只能引用可见评论图片或本人未发布的上传 |
| `DELETE /api/dreams/{id}/comments/{commentId}` | 评论作者删除自己的评论 |
| `POST /api/media` | multipart 上传，字段 `file` 和 `kind`（`IMAGE` / `VIDEO`），需要登录 |
| `GET /api/media/{随机文件名}` | 读取图片 / 视频，支持 Range |
| `DELETE /api/media/{id}` | 上传者移除尚未发布的附件 |
| `POST /api/dreams/{id}/helps` | 提交帮助意愿 |
| `GET /api/helps`、`GET/PATCH /api/helps/{id}` | 双方申请与状态管理 |
| `GET/POST /api/helps/{id}/messages` | 双方消息，`before` 查询更早记录 |
| `POST /api/direct/conversations` | 提交 `memberId`，打开或首次创建与该成员的私信会话 |
| `GET /api/direct/conversations`、`GET /api/direct/conversations/{id}` | 私信会话列表（含 `lastMessage`、`unread`）与详情，仅双方可见 |
| `GET/POST /api/direct/conversations/{id}/messages` | 私信消息，`before` 查询更早记录；读取最新一页时标记已读 |
| `POST /api/admin/login`、`POST /api/admin/logout`、`GET /api/admin/me` | 独立管理员会话 |
| `GET /api/admin/overview` | 后台统计 |
| `GET /api/admin/contents`、`GET/PATCH /api/admin/contents/{type}/{id}` | `DREAM` / `UPDATE` / `COMMENT` 查询、详情、下架与恢复 |
| `GET /api/admin/members`、`PATCH /api/admin/members/{id}/restriction` | 账号查询、限制与解除 |
| `GET /api/admin/logs` | 分页查看操作日志 |
| `POST /api/feedback`、`GET /api/feedback/mine` | 提交意见反馈（`category`、`content`、`contact`）/ 查看自己的反馈与回复 |
| `GET /api/admin/feedback`、`PATCH /api/admin/feedback/{id}` | 后台反馈列表（`state`、`category`、`keyword`、分页）/ 处理，提交 `{ status, expectedStatus, reply }` |
| `GET /api/admin/media/{随机文件名}` | 管理员访问已下架媒体 |

`GET /api/dreams` 支持 `keyword/category/helpType/status/page/size/mine`，`size` 为 1–30。匿名字段在后端投影时移除，不依赖前端隐藏。所有内部账户主键、密码哈希和会话摘要均不会作为匿名作者信息下发。

`POST /api/auth/register` 提交账号信息和 `noticeAccepted: true`、`noticeVersion`、`noticeSha256`（后二者取自声明接口），不再要求 `admissionToken`。未明确同意或缺少字段返回 400；过期版本或不匹配摘要返回 412。服务端保存自身文本，不接受客户端传来的声明原文、账号编号或同意时间。

问答提交返回 `score/passed/passScore/feedback/qualification`，其中 `passed` 表示本次答题结果，`qualification.passed` 表示账号是否已取得资格。`GET /api/me`、登录/注册响应的 `user.commentQualification` 以及已登录用户的评论列表响应中均提供资格状态。缺少答案、题号无效或选项不合法返回 400，试卷版本过期返回 412；未通过问答直接发表评论返回 403，客户端伪造 `score`、`passed` 或其他成员编号不生效。

梦想列表、详情及公开名片中的梦想返回 `supportCount`、`disagreeCount`、`commentCount`、`myReaction`；访客的 `myReaction` 为 `NONE`，不提供投票人员名单。评论列表支持 `page/size`（默认 20，最多 50），返回 `items/total/page/size/hasMore`；条目包含 `authorName/authorAvatar/isAuthor/isMine/authorReaction`，`authorReaction` 为作者当前的 `SUPPORT`、`DISAGREE` 或 `NONE`，匿名条目不含 `authorId`。未对该梦想表态的用户提交评论返回 403。

上传成功返回 `{ id, type, url, contentType, size }`，其中 `url` 为 `/api/media/` 下的相对路径。发布梦想或进展时，将这些 `id` 放入 `mediaIds` 数组；列表、详情与进展响应中的 `media` 数组用于渲染附件。

`PUT /api/me` 接收 `{ nickname, city, bio, skills, avatarId }`。`avatarId` 使用图片上传返回的 ID；空字符串恢复默认，省略或 `null` 保留现有头像。`GET /api/me` 和登录响应的 `user` 返回 `avatarId`、`avatarUrl`，公开名片只返回 `avatarUrl`。默认头像对应空 URL，由前端统一呈现；梦想使用 `authorAvatar`，帮助会话使用 `partnerAvatar`，消息使用 `senderAvatar`，匿名作者均返回空 URL。

后台内容处理提交 `{ hidden, expectedHidden, reason }`；账号限制提交 `{ mode, durationHours, expectedRevision, reason }`，`durationHours=null` 表示长期。旧状态或版本过期返回 409，避免覆盖其他管理员的操作。`/api/me` 中的 `restriction` 为当前有效限制；用户端直接调用写接口也会受到同样限制。

Java 的顶层类、嵌套入参类、测试类均包含作者、邮箱和公司注释；Vue/JS 源码也带有同样的署名。

## 本次验证（2026-10-16）

- Java 8（1.8.0_291）编译与运行，生成的 class 主版本为 52。
- 39 组后端集成测试在 H2 与 MySQL 5.7 独立测试库均通过，覆盖后管权限、内容下架、账号限制、头像资料、表态与评论，以及注册声明校验、同意留痕、评论资格 80/90 分边界、账号绑定与持久化、旧账号及旧评论兼容。
- 10 组浏览器流程全部通过，覆盖阅读声明后注册、未阅读时禁用同意、版本更新后重新确认、80 分重答至 90 分后返回评论及刷新保留资格，以及头像编辑、发布媒体、帮助交流、表态评论和后管操作。后管测试需启动后台服务，提供 `ADMIN_TEST_USERNAME`、`ADMIN_TEST_PASSWORD`，或使用本地 `.runtime/admin-access.json`；可用 `ADMIN_TEST_URL` 指定后台地址。未配置账号时后管用例跳过。
- H5、微信小程序、独立后台构建成功；后端可执行 JAR 打包成功。
- Java 类（含嵌套类与测试类）保留作者、邮箱、公司注释。
- 微信真机与原生安装包尚未验证。

评论媒体专项验证：`npm run test:e2e -- tests/comments-popup.spec.js` 使用模拟接口验证表情、上传重试、收藏操作和窄屏布局；`COMMENT_MEDIA_TEST_URL` 指定独立测试后端后，运行 `tests/comment-media.spec.js` 验证真实 MySQL 上传、收藏、刷新后发送及失败请求回滚。不要将此变量指向业务数据库的服务。
