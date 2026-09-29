# 心屿 Treehole

校园匿名树洞与情绪支持平台。公开树洞和私人心情记录使用统一的“情绪记录”模型，并通过四级风险识别连接教师关怀工作流。

## 快速启动

1. 安装 Docker Desktop。
2. 复制 `.env.example` 为 `.env`，至少修改管理员密码和 JWT 密钥。
3. 运行 `docker compose up --build`。
4. 打开 `http://localhost:3000`，API 文档位于 `http://localhost:8080/swagger-ui.html`。

默认管理员邮箱来自 `ADMIN_EMAIL`。学生可自行注册，教师需提交申请并由管理员审核。

## 本地开发

- 前端：Node.js 24 LTS，进入 `web` 后执行 `npm install && npm run dev`。
- 后端：Java 21 与 Maven 3.9，进入 `api` 后执行 `mvn spring-boot:run`。
- PostgreSQL：默认端口 `5432`。

## 隐私与安全

- 私人记录正文只返回给记录所有者，不进入风险事件快照。
- HIGH 风险身份访问需要管理员审批。
- CRITICAL 风险只向当前负责教师自动显示身份。
- 本系统不提供医疗诊断，也不会自动联系学校、家长或紧急机构。
