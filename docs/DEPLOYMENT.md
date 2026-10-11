# LessonDesk 部署与备份

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

Java21、Maven3.9+、Node24.19.0+、MySQL8.4。推荐Docker Compose v2（支持--wait），4GB可用内存及4GB以上磁盘。初始化私有.env后运行：

```bash
python3 scripts/init-env.py
docker compose -p lessondesk config --quiet
docker compose -p lessondesk up -d --build --wait --wait-timeout 180
```

默认仅本机8146；占用时改WEB_PORT或`WEB_PORT=8150 docker compose -p lessondesk up -d`，不停止其他服务。MySQL不公开端口，生产浏览器通过Nginx同源/api。健康路径/actuator/health；GET/auth/me检查会话。管理员默认admin，口令来自本机.env，无共享演示口令。ADMIN_USERNAME仅首次空库生效，已有数据重启不覆盖账号或密码。

正式对外部署使用正式域名、HTTPS反向代理、COOKIE_SECURE=true、受保护的数据库网络、主机防火墙、访问限流和备份监控。默认forward-headers-strategy=none，不信任访客伪造代理头。询盘应用限额按到达后端的连接来源；内置Nginx到后端时来源会聚合，生产应在边缘按真实客户端地址限流并按实际代理拓扑进行验收，不能用伪造转发头直接绕过。

外部MySQL需DATABASE_URL/DATABASE_USER及对应密码，jdbc:mariadb协议（连接MySQL数据库），使用sslMode=verify-full、可信CA与主机名验证。只配置加密的trust不等于外部身份核验；独立Compose使用内置MySQLTLS，不应宣称已验证外部CA或公网TLS。外部库应由部署方独立管理，捆绑备份脚本不接管外部库。

应用会话在后端内存；重建/重启后需重新登录，密码库不参与认证。配置修改后重新创建后端，避免误认为restart重读.env。正式上线应另核实数据库容量、日志留存、性能、用户隐私及实际业务制度。本机验收不是生产负载或跨国家合规认证。

版本迁移：V1建立身份，V2建立业务，V3扩展教练多类别及唯一收退款凭据。JPA只validate；发布后不得改旧迁移，结构变更新增版本。升级前备份，迁移失败读取日志，不跳过校验，不用ddl-auto=update。

```bash
python3 scripts/backup.py --project lessondesk --output /private/path/lessondesk-backup.zip
# 另建私有restore.env，设置不同且空闲的WEB_PORT，只允许本机绑定
python3 scripts/restore.py /private/path/lessondesk-backup.zip --project lessondesk-restore --env-file /private/path/restore.env
```

备份短暂停后端写入并自动重启，ZIP含数据库与密码散列，0600保存且不上传Git。恢复先校验成员、产品、大小和SHA256，拒绝旧容器、卷、网络、无效/已被Docker发布的端口；只导入自己可信的SQL。恢复后核对账号、原款、分钟流水与报表，正式搬迁后更换必要凭据。恢复失败保留新隔离资源供诊断，不自动删除未知旧库。

只对专用测试项目用down -v；业务停机用stop或down保留卷。勿使用docker system prune清理他人资源。源码调试使用compose.dev.yaml仅公开本机33146，随后scripts/dev-backend.py和frontend/npm run dev，开发时需本机JDK/Maven/Node。
