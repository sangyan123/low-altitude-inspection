# 低空无人机巡查平台（Low-Altitude Inspection）

面向城市治理的低空无人机综合巡查平台：覆盖 **巡查/运输任务调度 → AI 视觉识别 → 事件处置闭环 → 应急指挥 → 数据成果管理 → 统计大屏** 的完整业务链路。

> 前后端分离 · Spring Boot 3 + Vue 3 · JWT 认证 · WebSocket 实时推送 · 智谱 GLM-4.6V 视觉识别

---

## ✨ 功能总览

| 模块 | 功能 |
|---|---|
| 🏠 首页大屏 | 设备实时分布地图、状态分布图、近30天业务趋势、今日/昨日环比、30s 定时推送刷新 |
| 🛰 飞行态势 | 天地图底图实时无人机位置，沿航线往返飞行模拟（Haversine 弧长参数化） |
| 📋 任务中心 | 巡检任务（派单通知）、运输任务（载重校验 + 起飞/到达/签收状态机）、航线库（天地图可视化绘制）、飞行计划（空域预检 + 审批流） |
| 🤖 AI 中心 | 算法管理、智谱 GLM-4.6V 视觉识别（可切 Mock 引擎）、结果确认/误报/一键推送事件 |
| 📌 事件中心 | 人工上报 + AI 推送双来源，派单 → 处置 → 核验 → 销号全流程闭环，处置日志时间线 |
| 🚨 应急中心 | 五类应急事件、无人机调度指令、搜救目标标注/确认/追踪/分享 |
| 🗂 数据成果 | 影像/视频/三维模型（OBJ/GLB/GLTF/FBX/B3DM）/点云上传，Three.js 在线预览 |
| 🔔 消息中心 | 站内通知（5类业务类型）+ WebSocket 定向实时推送、未读管理 |
| 📊 统计报表 | 多维度统计（日/周/月粒度、部门维度）、Excel / PDF 导出 |
| ⚙️ 系统管理 | 部门（树形）、用户、角色管理，BCrypt 密码，JWT 无状态认证 |

**安全与基建**：接口限流（登录 5/min、AI 20/min、写操作 120/min，Redis 固定窗口 + 故障降级）、全局异常处理、统一响应体、Swagger 文档。

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2 · Spring Security · MyBatis-Plus 3.5 · MySQL 8 · Redis · WebSocket · JJWT · EasyExcel · OpenPDF · SpringDoc |
| 前端 | Vue 3 · TypeScript · Vite · Element Plus · Pinia · Leaflet（天地图）· ECharts 6 · Three.js |
| 测试 | JUnit 5 + Mockito（93 用例）· Vitest（23 用例）· JaCoCo 覆盖率 |

## 🚀 快速开始

### 环境要求

- JDK 17+、Maven 3.6+
- Node.js 20+、npm
- MySQL 8、Redis 6+

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init_database.sql
```

> 如从旧版本升级，按需执行 `sql/upgrade_*.sql` 增量脚本。

### 2. 启动后端（端口 8080）

```bash
cd backend
mvn spring-boot:run
```

数据库连接、JWT 密钥等见 `backend/src/main/resources/application.yml`，均可通过环境变量覆盖（如 `SPRING_DATASOURCE_URL`、`JWT_SECRET`）。Redis 默认连接 `localhost:6379`。

### 3. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 ，地图底图需要在 `frontend/.env` 中配置天地图浏览器端 Key（[申请地址](https://console.tianditu.gov.cn)）。

### 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 超级管理员 |

### 接口文档

启动后端后访问：http://localhost:8080/api/swagger-ui.html

## ⚙️ 关键配置项

| 配置 | 说明 |
|---|---|
| `spring.datasource.url/username/password` | MySQL 连接 |
| `jwt.secret` / `jwt.expiration` | JWT 签名密钥（**生产必须通过 `JWT_SECRET` 注入随机密钥**）与过期时间 |
| `ai.glm.model` / `ai.glm.base-url` | 智谱视觉模型与 API 地址 |
| `telemetry.simulation.enabled` | 遥测模拟开关；接入真实设备（MQTT/MAVLink）后置 `false` |
| `app.upload.dir` / `app.upload.max-size-mb` | 成果文件上传目录与大小限制 |

## 🧪 测试与覆盖率

```bash
# 后端：93 个单元用例（JWT/限流/过滤器 + 四大业务工作流 + 支撑模块）
cd backend && mvn test

# 后端覆盖率报告 → target/site/jacoco/index.html
cd backend && mvn verify

# 前端：23 个用例（坐标转换/设备标记/通知store）
cd frontend && npm test
```

## 📁 目录结构

```
├── backend/                  # Spring Boot 后端
│   └── src/main/java/com/drone/platform/
│       ├── common/           # 统一响应、全局异常、限流
│       ├── config/           # Security / MyBatis-Plus / Web 配置
│       ├── security/         # JWT 认证过滤器
│       ├── websocket/        # WebSocket 推送通道
│       └── module/           # 业务模块（ai/device/task/event/emergency/
│                             #   data/report/dashboard/notification/system/file）
├── frontend/                 # Vue 3 前端
│   └── src/
│       ├── api/              # 接口封装
│       ├── components/       # 3D预览、航线绘制、GIS地图等
│       ├── utils/            # 坐标转换、WebSocket、请求封装
│       ├── stores/           # Pinia 状态
│       └── views/            # 21 个业务页面
└── sql/                      # 建库脚本与增量升级脚本
```

## 🔒 安全须知

- 生产环境务必通过环境变量注入随机 `JWT_SECRET`，不要使用默认值
- 密码使用 BCrypt 存储；`sys_config` 中的第三方 Key 注意权限隔离
- AI 识别当前内置 GLM Key 于 `GlmVisionEngine`，**生产部署前建议重构为环境变量注入并轮换密钥**

## 🗺 Roadmap

- [ ] Docker Compose 一键部署
- [ ] 菜单级 RBAC 权限（表结构已就绪）
- [ ] 真实无人机 MQTT/MAVLink 遥测接入
- [ ] AI 识别 Key 环境变量化
