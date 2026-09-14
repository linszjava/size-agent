# Size Agent

基于 Spring Boot、Spring AI Alibaba、Vue 3 和 TypeScript 的 AI 应用学习项目。项目按功能阶段逐步实现模型调用、流式输出、对话记忆、Tool Calling 和 RAG 知识库问答，并为后端接口提供本地文档与调试页面。

## 项目结构

```text
size-agent/
├── 01-spring-ai-alibaba-core/
│   ├── backend/    # Spring Boot 后端
│   ├── frontend/   # Vue 3 + TypeScript 前端
│   └── sql/        # MySQL 初始化脚本
└── 02-mcp-nacos-a2a/  # MCP、Nacos 与 A2A 多服务示例

output/pdf/        # 知识库上传测试 PDF
```

## 版本记录

版本标签用于保存每个学习阶段完成时的代码状态。可以在 IDEA 的 Git 日志中签出对应标签，查看当时的完整项目代码。

| 标签        | 日期       | 提交      | 完成内容                                                                                                        |
| ----------- | ---------- | --------- | --------------------------------------------------------------------------------------------------------------- |
| `tag-1.0`   | 2026-09-06 | `44188b4` | 完成 DashScope ChatClient 基础接入接口及接口测试。                                                              |
| `tag-1.1`   | 2026-09-07 | `aaf6ba1` | 完成 DashScope ChatModel、ChatClient、消息历史和 SSE 流式输出接口；新增 Vue 3 + TypeScript 接口文档与调试页面。 |
| `tag-1.2-1` | 2026-09-08 | `783fd4e` | 完成 Chat Memory 用户对话记忆的内存存储实现，支持普通对话、流式对话、查询历史和清空历史。                       |
| `tag-1.2-2` | 2026-09-08 | `b50aa40` | 将 Chat Memory 改为 MySQL 持久化存储；完成对话记忆前端页面及接口联调。                                          |
| `tag-1.3`   | 2026-09-10 | `0c09b12` | 完成 Tool Calling 前后端接口联调，支持天气、时间、员工排班、企业制度查询，以及带二次确认的换班申请。            |
| `tag-1.4`   | 2026-09-12 | `5fe702f` | 完成 RAG 文档上传与知识问答、租户与部门过滤、前端调试页面，以及测试 PDF。                                       |
| `tag-1.5`   | 2026-09-13 | `93c5a5f` | 完成 ReactAgent 员工助手与对应前端页面，支持按员工、会话隔离上下文及工具调用。                                  |
| `tag-1.6`   | 2026-09-13 | `7ce25c8` | 完成 Spring AI Alibaba Graph 换班工作流、人工中断与恢复，以及对应前端调试页面。                                 |
| `tag-1.7`   | 2026-09-14 | `ca21859` | 完成 MCP、Nacos 与 A2A 多模块示例，以及 MCP、A2A 前端调试页面和独立代理配置。                                   |

### 版本分支与标签

当前版本分支的边界如下：

```text
v-1.0 → 截止 tag-1.6，包含第一阶段核心能力、RAG、ReactAgent 和 Graph
v-1.1 → 从 v-1.0 继续开发，增加 tag-1.7 的 MCP、Nacos 与 A2A
main  → 当前最新、完整代码
```

`v-1.1` 包含 `v-1.0` 的全部历史是正常的：新版本分支从旧版本的最后一个提交创建，然后继续增加新功能。开发下一版本前，应先创建并切换分支，再提交新版本代码：

```bash
git switch v-1.0
git switch -c v-1.1

# 修改并验证代码后
git add .
git commit -m "MCP Nacos A2A 的整合"
git push -u github v-1.1
```

标签是固定代码快照。签出标签会进入 detached HEAD 状态：

```bash
git switch --detach tag-1.6
```

如果要在历史版本上继续开发，应基于标签创建新分支：

```bash
git switch -c feature/from-tag-1.6 tag-1.6
```

### 为什么签出旧标签后仍可能看到新版本目录

Git 只切换已经纳入版本控制的文件，不会自动删除 `.gitignore` 忽略的构建产物。例如在 `v-1.1` 编译过 `02-mcp-nacos-a2a` 后，其中的 `target/classes`、测试报告等文件会留在本地；签出 `tag-1.6` 时，MCP 源码会消失，但残留的 `target` 目录仍可能被 IDEA 显示。

先预览仅属于 `02-mcp-nacos-a2a` 的忽略文件：

```bash
git clean -ndX -- 02-mcp-nacos-a2a/
```

确认输出只有可重新生成的构建产物后，再清理：

```bash
git clean -fdX -- 02-mcp-nacos-a2a/
```

也可以在切换版本前进入对应 Maven 项目执行 `mvn clean`。清理后刷新 IDEA 或重新加载 Maven 项目。`github/HEAD` 是远程默认分支的本地符号指针，不是独立版本分支；推荐在 GitHub 将默认分支设置为 `main`，然后执行 `git remote set-head github --auto` 更新本地指向。

## 启动项目

### 后端

使用 IDEA 打开 `01-spring-ai-alibaba-core/backend`，配置所需环境变量后运行 `AliyunApplication`。后端默认监听 `8888` 端口。

后端使用 Java 21 和 Maven。先准备 MySQL 数据库及支持向量检索的 Redis（例如 Redis Stack，需包含 Search 和 JSON 功能），再配置以下环境变量：

| 环境变量                                          | 用途                                                    |
| ------------------------------------------------- | ------------------------------------------------------- |
| `AI_DASHSCOPE_API_KEY`                            | 百炼模型 API Key，必填                                  |
| `AI_MODEL` / `AI_EMBEDDING_MODEL`                 | 聊天模型 / 向量模型名称                                 |
| `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | MySQL 连接与认证信息                                    |
| `REDIS_HOST` / `REDIS_PORT`                       | Redis 地址，默认 `localhost:6379`                       |
| `REDIS_USERNAME` / `REDIS_PASSWORD`               | Redis 认证信息，须与本地实例一致                        |
| `REDIS_VECTOR_INDEX` / `REDIS_VECTOR_PREFIX`      | 向量索引与键前缀，默认 `knowledge-index` / `knowledge:` |
| `AI_REQUEST_TIMEOUT`                              | AI 请求总预算，默认 `30s`                               |
| `AI_MAX_INPUT_CHARACTERS`                         | 输入字符数上限配置，默认 `4000`                         |
| `AI_MAX_AGENT_ROUNDS`                             | Agent 最大执行轮数配置，默认 `8`                        |
| `AI_MAX_RAG_RESULTS`                              | RAG 最大返回数量配置，默认 `5`                          |

具体默认值及模型服务地址见 `backend/src/main/resources/application.yml`。模型名应与配置的百炼服务地址匹配。开发配置启用了数据库表和向量索引初始化。

也可在仓库根目录运行：

```bash
cd 01-spring-ai-alibaba-core/backend
mvn spring-boot:run
```

### 前端

需要 Node.js 22.12+。在仓库根目录执行：

```bash
cd 01-spring-ai-alibaba-core/frontend
npm ci
npm run dev
```

默认页面地址为 `http://127.0.0.1:5173`。前端通过 Vite 将 `/api` 请求代理到 `http://localhost:8888`。

## 已适配接口

| 页面           | 方法   | 路径                                               | 响应                    |
| -------------- | ------ | -------------------------------------------------- | ----------------------- |
| 基础对话       | POST   | `/api/chat/v1`                                     | ChatResponseDto         |
| ChatModel      | POST   | `/api/core/chat-model`                             | ChatResponseDto         |
| ChatClient     | POST   | `/api/core/chat-client`                            | ChatResponseDto         |
| 消息历史       | POST   | `/api/core/messages`                               | ChatResponseDto         |
| 流式输出       | POST   | `/api/core/stream`                                 | text/event-stream       |
| 记忆对话       | POST   | `/api/memory/chat`                                 | JSON                    |
| 记忆流式       | POST   | `/api/memory/stream`                               | text/event-stream       |
| 查询记忆       | GET    | `/api/memory/conversations/{conversationId}`       | JSON                    |
| 清空记忆       | DELETE | `/api/memory/conversations/{conversationId}`       | 204                     |
| 工具调用       | POST   | `/api/tools/chat`                                  | JSON                    |
| 确认换班       | POST   | `/api/tools/shift-swaps/confirm`                   | JSON                    |
| 文档上传       | POST   | `/api/knowledge/documents`                         | KnowledgeUploadResponse |
| 知识问答       | POST   | `/api/knowledge/ask`                               | RagAnswerResponse       |
| 员工 Agent     | POST   | `/api/agents/employee/chat`                        | AgentChatResponse       |
| Graph 创建流程 | POST   | `/api/workflows/shift-swaps`                       | ShiftWorkflowResponse   |
| Graph 员工确认 | POST   | `/api/workflows/shift-swaps/{workflowId}/approval` | ShiftWorkflowResponse   |

## 前端检查与构建

```bash
cd 01-spring-ai-alibaba-core/frontend
npm test
npm run build
```

### 前端开发说明

- 可复制前端目录的 `.env.example` 为 `.env.local`，通过 `API_PROXY_TARGET` 修改后端代理地址，修改后需重启 Vite。模型密钥由后端管理。
- `npm run preview` 默认监听 `4173`；生产部署需要单独配置 `/api` 反向代理，开发代理不会编译进静态文件。
- `npm run format` / `npm run format:check` 用于格式化及检查前端文件。
- `src/data/endpoints.ts` 管理导航与接口元数据，`src/lib/knowledgeApi.ts` 封装知识库请求，`src/components/KnowledgeBasePage.vue` 实现知识库页面。
- `⌘/Ctrl + K` 搜索接口；知识问答输入框中可使用 `⌘/Ctrl + Enter` 发送。刷新页面会重置表单。
- 知识库请求等待上限为 120 秒；停止等待不保证后端停止处理，上传超时后应先检查后端结果，避免重复上传。

## RAG 知识库流程

入库：上传 PDF / Word / TXT / Markdown → 提取文字 → 添加元数据 → 文本分块 → Embedding 向量化 → 将文字、向量和元数据写入 Redis。

问答：问题向量化 → 带权限条件检索相关分块 → 再次校验权限 → 将原文作为 context 与问题一起交给聊天模型 → 返回回答及引用来源。原始 PDF 文件不作为文件存入向量数据库；交给聊天模型的是检索到的文字，而不是向量。

当前检索最多返回 5 个分块，相似度阈值为 0.60。文档入库和问题检索应使用相同的向量模型；更换模型或向量维度时，应使用新索引和前缀，重新上传文档。

### 权限与接口约定

两个知识库接口都需要 `X-Tenant-Id` 和 `X-Department-Id` 请求头，值为 1–64 位字母、数字、下划线或短横线。

- `POST /api/knowledge/documents`：使用 multipart/form-data，提交 `file` 和 `visibility`；默认 `DEPARTMENT`，同租户同部门可见。`PUBLIC` 表示同租户内公开。
- `POST /api/knowledge/ask`：提交 JSON `{"question":"员工申请换班需要谁审批？"}`，问题最多 1,000 字符；返回 `answer` 和 `sources`。
- 当前身份来自调试页面传入的请求头，尚未接入登录认证。正式应用应由服务端认证身份确定租户和部门。

### 上传测试

1. 打开前端 `/#knowledge`，填写 `tenant-001` 和 `department-001`。
2. 选择 [员工休假与排班测试手册](output/pdf/知识库测试-员工休假与排班手册.pdf)，点击「上传并建立索引」。资料为虚构演示制度。
3. 等待返回文档 ID、分块数和 `ACTIVE` 状态，再提问：「连续休假 4 天，需要提前多久申请？」
4. 检查回答及来源文件、页码、相关性分数；保持租户和部门一致以验证部门文档检索。

前端支持 PDF、DOC、DOCX、TXT、MD，文件需小于 20 MB；后端单文件及整个 multipart 请求上限均为 20 MB。

### 常见问题

- **上传成功但没有相关资料**：核对上传和提问的租户、部门是否一致，并检查相似度阈值及文档实际提取出的文字。空结果不等于没有权限。
- **Redis 查询语法错误**：当前已对权限过滤中 ID 的短横线进行 Redis TAG 转义，并保留公开或同部门条件的分组；修改后需重启后端。存储的 ID 无需修改。
- **接口返回 500**：结合后端异常堆栈区分 Redis 检索、向量模型及聊天模型调用错误，前端通用错误信息无法单独确定原因。

## 后端检查

```bash
cd 01-spring-ai-alibaba-core/backend
mvn test
```

当前回归测试覆盖 Redis 权限查询转义、条件分组、权限校验、非法 ID、请求关联 ID 和敏感数据脱敏；实际模型与数据库联调需要可用的外部服务。

## 企业级工程能力

第 13 章在现有后端基础上补充了统一工程预算、请求链路标识、敏感数据脱敏和运行状态观测：

```text
backend/src/main/java/com/size/
├── config/
│   ├── AiEngineeringConfig.java
│   └── AiEngineeringProperties.java
├── support/
│   └── SensitiveDataMasker.java
└── web/
    └── RequestCorrelationFilter.java
```

`AiEngineeringProperties` 统一绑定 `app.ai.engineering` 下的配置，集中保存请求超时、输入长度、Agent 最大轮数和 RAG 最大返回数量。当前这些值提供统一配置入口；具体 ChatClient、Agent、RAG 或外部工具还需要在各自执行位置读取并落实对应限制。

`RequestCorrelationFilter` 会为每次 HTTP 请求建立关联 ID：

- 请求携带合法的 `X-Request-Id` 时沿用该值；
- 请求未携带或格式不合法时生成新的 32 位 ID；
- 响应头返回 `X-Request-Id`；
- 请求处理期间将 ID 写入 MDC，结束后立即清理，避免 Tomcat 线程复用导致串号。

例如：

```bash
curl -i http://localhost:8888/actuator/health \
  -H 'X-Request-Id: local-health-check'
```

响应头中可以看到：

```text
X-Request-Id: local-health-check
```

`SensitiveDataMasker` 提供日志写入前的基础脱敏能力，可处理 Bearer Token、JSON 中的 `password`、`apiKey`、`token` 以及中国大陆手机号。它是显式调用的工具类，记录业务日志时应先调用：

```java
log.info("AI request: {}", SensitiveDataMasker.mask(requestText));
```

后端已加入 Spring Boot Actuator，并只开放以下端点：

| 端点 | 用途 |
| --- | --- |
| `/actuator/health` | 检查应用及依赖健康状态 |
| `/actuator/info` | 查看应用公开信息 |
| `/actuator/metrics` | 查看可用指标名称 |
| `/actuator/metrics/{metricName}` | 查看指定指标 |

健康端点不会返回详细依赖信息，`env`、`configprops` 等可能泄露配置的端点未开放。生产环境仍应在网关或安全配置中限制 Actuator 的访问范围。

## 员工 Agent 页面

从导航进入「员工 Agent」，或访问 `http://127.0.0.1:5173/#agent`。填写员工 ID、会话 ID，发送排班或制度问题，可在同一会话中继续追问。员工身份通过 `X-Employee-Id` 请求头传入，请求体为 `conversationId` 和 `message`；两个 ID 均为 1–64 位字母、数字、下划线或短横线，消息最多 1,000 字符。

后端使用 ReactAgent 和 MemorySaver 按员工与会话隔离上下文，重启后端会丢失会话。新建会话生成新 ID；页面只展示本次打开后收到的记录，不提供历史记录加载或服务端会话删除。接口返回最终回答，不返回流式片段或工具执行轨迹。

换班仍需用户核对回答后，在「确认换班」区域提交令牌，复用 `/api/tools/shift-swaps/confirm`。切换身份或会话会清空当前展示内容与令牌。请求最多等待 120 秒，超时或离开页面不保证后端立即停止，重试前应检查处理结果。

## Graph 换班工作流页面

从导航进入「Graph 工作流」，或访问 `http://127.0.0.1:5173/#graph`。填写员工 ID、换班日期、目标班次（白班或晚班）和原因（最多 200 字符），创建后查看工作流 ID、当前与目标班次、制度和状态。

流程在 `human_approval` 节点前暂停，状态为 `WAITING_EMPLOYEE_CONFIRMATION` 时由员工选择「确认并继续」或「拒绝申请」。确认接口提交布尔值 `approved`，并沿用创建时的 `X-Employee-Id`；也可手动填写待确认工作流 ID，但当前没有查询接口，页面无法预先加载该流程详情。结束后展示 `COMPLETED` 或 `REJECTED` 等后端实际状态及申请编号。

当前 Graph 使用内存检查点和演示排班：更新排班、发送通知节点只返回演示状态，并未连接真实业务系统，也没有独立主管审批节点。重启后端会丢失流程；页面展示流程定义和最终响应，不模拟实时节点执行轨迹。请求超时不保证后端停止执行，重复操作前需核对后端结果。

## MCP、Nacos 与 A2A 前端调试

第二阶段 `02-mcp-nacos-a2a` 复用 `01-spring-ai-alibaba-core/frontend` 页面：

| 页面入口              | 接口                                      | 默认代理目标            |
| --------------------- | ----------------------------------------- | ----------------------- |
| `/#mcp` · MCP / Nacos | `POST /api/mcp/chat`，JSON 字段 `message` | `http://localhost:9002` |
| `/#a2a` · A2A / Nacos | `GET /api/a2a/hr`，查询参数 `question`    | `http://localhost:9102` |

先启动 Nacos，再启动所需服务端与客户端。MCP 对应 `employee-mcp-server:9001` 和 `employee-mcp-client:9002`；A2A 对应 `hr-a2a-server:9101` 和 `employee-a2a-client:9102`。模块启动命令在 `02-mcp-nacos-a2a` 中执行 `mvn -pl <模块名> spring-boot:run`，Nacos 可通过该目录下 `docker compose up -d` 启动。

前端 `.env.local` 可配置 `MCP_PROXY_TARGET`、`A2A_PROXY_TARGET`，修改后重启 Vite。两个路径优先于第一阶段 `/api` 代理，开发与 preview 均生效；生产静态服务需要配置相同的路径分流。

MCP 可测试「查询员工 E1001 今天的排班，必须调用工具」。A2A 可测试「公司年假制度是什么？」；该模块独立演示每年 10 天年假、最多结转 5 天，和第一阶段 RAG 测试 PDF 无关。A2A 页面提取可识别的助手消息，完整状态保留在原始 JSON 中；两个接口均未暴露会话 ID 或执行轨迹。

页面支持空输入校验、请求取消、120 秒等待上限和错误详情。客户端停止等待不保证远程执行停止。页面所示调用路径是架构说明，不是健康检查。模型与 Nacos 凭据仅在后端配置。
