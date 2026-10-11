[中文](README.md) | [English](README.en.md)

# LessonDesk · 知华驾校约课与教务系统

知华科技（上海如静知华信息科技有限公司） · [官网](https://www.zhuatech.cn/)

LessonDesk 为多教练、共享教练车的驾校提供学员档案、套餐报读、手机约课、教练车辆排课、签到、训练记录、分钟核销与收退费。教务、财务、教练和学员使用同一套持久化记录，公众可看课程和提交私有询盘。Java 21 / Spring Boot 4 / Vue 3 / MySQL 8.4，中英文操作界面。

**公开源码学习版／非商业源码版**：仅限个人学习交流，未经上海如静知华信息科技有限公司书面授权不得商用。不是允许免费商用的 OSI 开源许可，详见 [LICENSE](LICENSE)。

## 谁使用，怎样完成一节课

适合有多名教练、需要协调车辆、采用课时套餐的独立驾校，也供承接品牌网站和驾校软件交付的实施团队评估。单教练只记纸质日历的场景不预设需要本系统。首版是单经营主体、分校组织范围的 H5 与管理端，不是政府驾考系统或原生 App。

1. 管理员创建分校、岗位与独立登录账号；教务绑定学员和教练档案，维护车辆及教学类别。
2. 发布教练工作时段，记录请假、车辆检修及课间缓冲；教练可管理本人工作段。
3. 创建报读并冻结套餐分钟、价格、币种、有效期和取消规则。财务逐笔核实收款；足额后才可预约，零价套餐无需付款。
4. 学员手机自助或教务代约；先查询空档，提交时同时核验教练、车辆、学员占用与分钟余额。
5. 课前签到，教练开始授课，登记实际分钟、训练项目与反馈；完成后释放预留并核销实际分钟。
6. 改期不重复预留；取消、缺席按购买时规则处理。停课前处理全部未完成课程，再按原收款逐笔登记可退金额。

教学类别是内部分类，默认手动挡、自动挡，不是驾驶资质认证。教练可有1–8个类别，车辆及套餐各一个；本版一课一学员，不含团体理论课。授课分钟由教练确认，不从GPS/设备或政府学时系统获取。

## 已实现的岗位与入口

| 入口 | 可用功能 |
|---|---|
| 教务／管理端 | 分校、教练、车辆、套餐和学员；公开询盘跟进；报读；代约、改期、取消、签到、缺席；工作时段／请假／检修；搜索、分页、排序；报表和CSV |
| 财务 | 原单价格及有效期快照、核实收款、停止报读、按原款退款、凭据唯一性和分币种统计；不会发起银行卡或支付网关交易 |
| 教练手机端 | 本人被指派课次；本人工作时段与请假；签到、开课、登记授课分钟和训练反馈；不能读取全校学员联系人和财务 |
| 学员手机端 | 本人报读、预留／已用／可用分钟、有效期、收退款流水；真实空档查询、自助约课、改期、取消、签到和本人课次进度 |
| 公众 | 启用分校和套餐；提交私有咨询并取得随机回执；不获取学员、教练账号、他人预约或联系人 |
| 系统管理员 | 账号启停、强密码重置、角色和权限、菜单、组织时区、教学字典、参数、审计及本人改密码 |

后台关键写入使用事务行锁、版本号和绑定账号／载荷的 UUID 请求，重复请求不重复扣时或入账。账号、密码、角色和组织状态在每次访问时检查。最后一个完整管理员不能被停用。

## 分钟与费用规则

- 每课30–180分钟，按15分钟递增；套餐总分钟30–60,000，有效1–1,095天。金额用最小货币单位，仅支持两位小数ISO币种，各币种分别统计。
- 预留分钟和已核销分钟分开。教练确认0至预约分钟的实际时长，不足部分释放；0分钟须说明中断原因。终态不回写、不删除。
- 学员逾期取消或缺席按冻结的 `lateCharge` 规则核销预约分钟；教务可说明理由取消并豁免。自助改期必须在取消提前小时之前。
- 不可退金额＝套餐价格×已核销分钟÷总分钟，最小货币单位向上取整。关闭报读后，每笔退款同时受总可退上限和原收款未退额限制。
- 时间存UTC。查询日期使用分校时区，显示及工作段输入使用浏览器时区。提前完成仍保留原定占用和缓冲；超时未完成课程继续阻塞后续开课。

详细操作见 [操作手册](docs/OPERATIONS.md)，完整接口见 [API](docs/API.md)。

## 环境与一键启动

Docker Engine 27+／Docker Desktop、Docker Compose v2（支持`--wait`）、Python 3.10+，建议4GB可用内存和4GB以上磁盘。源码调试需JDK21、Maven3.9+、Node.js **24.19.0+**、npm及MySQL8.4。

```bash
git clone https://github.com/zhuatech-han/zhuatech-lessondesk.git
cd zhuatech-lessondesk
python3 scripts/init-env.py
docker compose -p lessondesk config --quiet
docker compose -p lessondesk up -d --build --wait --wait-timeout 180
```

前端 <http://127.0.0.1:8146/>，健康 <http://127.0.0.1:8146/actuator/health>，公开课程 <http://127.0.0.1:8146/#public>。管理员默认`admin`，口令取本机生成的`.env`中`ADMIN_PASSWORD`，**没有统一演示密码**。口令12–72字节、含大小写和数字；不要公开`.env`或保存临时测试密码。

首次空库只初始化管理员、一个分校、五种岗位角色、权限菜单、教学类别和参数，**没有业务学员、课次或收费**。账号由分校创建，学员和教练需绑定档案。重启不重置密码；后端内存会话重启后需重新登录。

## 架构、目录与数据库

浏览器 → Nginx同源代理 → Spring Security／Spring Boot业务事务 → MySQL。Vue所有核心记录来自API；MySQL保存身份、资源、报读快照、课次、分钟／现金流水、询盘、事件和审计。JPA只验证模型；Flyway按版本建表和升级，数据库行锁用于低容量实例，不宣称分布式高并发。

数据库为 MySQL 8.4，JDBC 使用未修改的 MariaDB Connector/J 3.5.10（LGPL），第三方许可独立，见 [第三方许可](docs/THIRD_PARTY.md)。

```text
backend/src/main/java/cn/zhuatech/lessondesk/  身份、主数据、排课及账务
backend/src/main/resources/db/migration/    V1身份、V2业务、V3多类别与凭据约束
backend/src/test/                           算法与真实接口测试
frontend/src/                              中文／英文管理、教练、学员与公开入口
frontend/public/brand/                     原始LOGO、两个微信二维码
scripts/                                   初始化、验收、备份恢复和发布检查
compose.yaml / compose.dev.yaml            MySQL、后端、前端／仅本机调试覆盖
.github/workflows/verify.yml                全新数据库及完整业务自动验证
docs/                                      操作、API、部署、安全和实际截图
```

版本发布后不能改旧迁移，新增结构用新版本；升级先备份，不使用`ddl-auto=update`替代迁移。`V3__instructor_categories.sql`保留原有单类别并扩展为集合。初始化和数据库说明见 [部署手册](docs/DEPLOYMENT.md)。

## 配置与源码调试

`.env.example`只有字段和说明；`scripts/init-env.py`生成独立随机密码，拒绝覆盖现有`.env`。

| 配置 | 用途 |
|---|---|
| `MYSQL_ROOT_PASSWORD`、`DATABASE_PASSWORD` | 捆绑MySQL强密码，不能留空 |
| `ADMIN_USERNAME`、`ADMIN_PASSWORD` | 仅首次空库管理员身份及强密码 |
| `WEB_PORT`、`BIND_ADDRESS` | 默认8146、127.0.0.1；占用时改端口，勿停止其他项目 |
| `COOKIE_SECURE` | 本机HTTP为false；正式HTTPS部署启用true |
| `DATABASE_URL`、`DATABASE_USER` | 可选外部MySQL；应验证CA和主机名，不由捆绑备份脚本管理 |
| `MYSQL_DEV_PORT` | 调试覆盖文件的本机端口33146 |

```bash
# 需要本机JDK/Maven/Node；只开放本机调试数据库
docker compose -p lessondesk-dev -f compose.yaml -f compose.dev.yaml up -d mysql
python3 scripts/dev-backend.py
# 另一个终端：
cd frontend
npm ci --no-audit --no-fund
npm run dev
```

Vite默认5173代理本机8080，生产Nginx访问容器服务名，不依赖本机开发地址。端口覆盖示例：`WEB_PORT=8150 docker compose -p lessondesk up -d`。

## 当前实际运行页面

截图来自明确标记TEST的隔离验收录入，不是真实客户案例。全新安装没有这些业务记录；截图未包含密码、密钥或系统保存密码弹窗。

### 登录：独立账号认证
![登录：独立账号认证](docs/screenshots/login.jpg)

### 教务：课次检索与状态
![教务：课次检索与状态](docs/screenshots/office-home.jpg)

### 学员：本人报读与分钟余额
![学员：本人报读与分钟余额](docs/screenshots/student-home.jpg)

### 约课：查询教练与车辆的真实空档
![约课：查询教练与车辆的真实空档](docs/screenshots/booking.jpg)

### 教练：本人授课与训练反馈
![教练：本人授课与训练反馈](docs/screenshots/instructor-desk.jpg)

### 原单：收退款及分钟流水
![原单：收退款及分钟流水](docs/screenshots/enrollment.jpg)

### 教练与车辆目录：资源维护
![教练与车辆目录：资源维护](docs/screenshots/resources.jpg)

### 后台：账号启停与角色分配
![后台：账号启停与角色分配](docs/screenshots/accounts.jpg)

### 统计：按币种核对实收与退款
![统计：按币种核对实收与退款](docs/screenshots/reports.jpg)

### 权限：组织与岗位范围
![权限：组织与岗位范围](docs/screenshots/roles.jpg)

### 公开课程：套餐展示与私有询盘
![公开课程：套餐展示与私有询盘](docs/screenshots/public.jpg)

### 移动端：本人约课入口
![移动端：本人约课入口](docs/screenshots/mobile.jpg)

### 英文：同一真实业务记录
![英文：同一真实业务记录](docs/screenshots/english.jpg)

## 验证、部署与故障

```bash
mvn -B -f backend/pom.xml spotless:check verify
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run format:check
npm --prefix frontend run lint
npm --prefix frontend test
npm --prefix frontend run build
python3 scripts/release-check.py
```

后端28项（6算法、22接口集成），前端12项。Docker Maven构建不跳过测试。真实MySQL验收会创建TEST资料，**只对独立本机空业务库运行**：

```bash
python3 scripts/quality.py --base http://127.0.0.1:8146
docker compose -p lessondesk restart backend
docker compose -p lessondesk up -d --wait --wait-timeout 180
python3 scripts/verify-persistence.py
```

实际记录的HTTP请求数及结果在忽略的`output/mysql-quality.json`；快照和临时密码保持私有。不能把合成数据与自测当成外部使用或付费验证。详见 [测试范围](docs/TESTING.md)。

```bash
python3 scripts/backup.py --project lessondesk --output /private/path/lessondesk-backup.zip
python3 scripts/restore.py /private/path/lessondesk-backup.zip --project lessondesk-restore --env-file /private/path/restore.env
```

restore.env必须是私有文件并指定不同空闲端口；恢复拒绝覆盖旧容器、卷、网络和损坏备份。备份短暂停止写入并自动启动，含密码散列，应保密。只有专用测试项目才用`down -v`删测试卷。

常见问题：初次无学员／教练入口数据时先建档；无法约课先查足额收款、工作段、类别、占用、有效期和提前时间；版本冲突先刷新；网络写入超时先查原单，勿盲目重发；重启后登录过期属内存会话边界。Compose失败查看本项目日志，修复后重测，不跳过测试。正式部署需域名、TLS、COOKIE_SECURE、私有数据库网络、边缘限流、监控与可靠备份，见 [部署](docs/DEPLOYMENT.md) 和 [安全](docs/SECURITY.md)。

## 已知限制与第三方配置

- 不含政府驾考／学时认证、考试预约、官方题库、证书、GPS计时、车辆硬件或地图动态通勤。
- 不含团体理论班、多学员同车、自动周期排课、候补递补、教练工资提成、原生App、离线、跨公司SaaS、自助注册和密码找回邮件。
- 不含短信、自动外发邮件、真实支付扣款、法定电子签名或税务发票；收退费为人工核实记录。没有外部凭据也可完成核心流程。
- 工作段显式新增，教练最多8个类别；固定缓冲不替代实际行车时间。终态课程不提供事后改写分钟或无痕删除，特殊纠错需另设计审批。
- 单目录10,000条保护上限，审计最近1,000条；未做生产负载、外网渗透和所有浏览器验证，不宣称生产认证或全部国家规则适配。
- 公开询盘应用限额默认代理来源会聚合；公网需按实际代理拓扑部署边缘限流。外部CA、域名TLS和付费第三方对接由部署方配置并单独验收。

## 授权、反馈及联系知华科技

自有代码依 [LICENSE](LICENSE) 仅限个人学习、技术研究与非商业交流；商业使用、企业部署、收费交付、二次销售及深度定制须事先书面授权。第三方依赖保留原版权和许可，见 [第三方说明](docs/THIRD_PARTY.md)。按现状提供，不保证适配全部经营制度；部署方应核对实际制度和适用要求。

欢迎通过仓库Issue／PR提交脱敏复现及改进；不贴密码、Cookie或个人资料。安全问题通过官网和咨询入口私下说明，不公开敏感证据。

[知华科技官网](https://www.zhuatech.cn/) · 微信 `zhuatech`、`zhuatech2`。**商业授权或深度定制开发请联系知华科技**。服务包括源码授权、业务规则定制、部署和系统集成。

<table><tr><td align="center"><img src="frontend/public/brand/wechat-zhuatech.png" width="220" height="220" alt="微信 zhuatech"/><br/>zhuatech</td><td align="center"><img src="frontend/public/brand/wechat-zhuatech2.png" width="220" height="220" alt="微信 zhuatech2"/><br/>zhuatech2</td></tr></table>
