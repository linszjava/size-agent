# Size Agent

基于 Spring Boot、Spring AI Alibaba、Vue 3 和 TypeScript 的 AI 应用学习项目。项目按功能阶段逐步实现模型调用、流式输出、对话记忆、Tool Calling 和 RAG 知识库问答，并为后端接口提供本地文档与调试页面。

## 项目结构

```text
size-agent/
└── 01-spring-ai-alibaba-core/
    ├── backend/    # Spring Boot 后端
    ├── frontend/   # Vue 3 + TypeScript 前端
    └── sql/        # MySQL 初始化脚本

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

标签记录的是固定的代码快照。签出标签后会进入 detached HEAD 状态；如果需要在历史版本上继续开发，应从该标签新建分支。

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

| 页面       | 方法   | 路径                                         | 响应                    |
| ---------- | ------ | -------------------------------------------- | ----------------------- |
| 基础对话   | POST   | `/api/chat/v1`                               | ChatResponseDto         |
| ChatModel  | POST   | `/api/core/chat-model`                       | ChatResponseDto         |
| ChatClient | POST   | `/api/core/chat-client`                      | ChatResponseDto         |
| 消息历史   | POST   | `/api/core/messages`                         | ChatResponseDto         |
| 流式输出   | POST   | `/api/core/stream`                           | text/event-stream       |
| 记忆对话   | POST   | `/api/memory/chat`                           | JSON                    |
| 记忆流式   | POST   | `/api/memory/stream`                         | text/event-stream       |
| 查询记忆   | GET    | `/api/memory/conversations/{conversationId}` | JSON                    |
| 清空记忆   | DELETE | `/api/memory/conversations/{conversationId}` | 204                     |
| 工具调用   | POST   | `/api/tools/chat`                            | JSON                    |
| 确认换班   | POST   | `/api/tools/shift-swaps/confirm`             | JSON                    |
| 文档上传   | POST   | `/api/knowledge/documents`                   | KnowledgeUploadResponse |
| 知识问答   | POST   | `/api/knowledge/ask`                         | RagAnswerResponse       |
| 员工 Agent | POST   | `/api/agents/employee/chat`                  | AgentChatResponse       |

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

当前回归测试覆盖 Redis 权限查询转义、条件分组、权限校验和非法 ID；实际模型与数据库联调需要可用的外部服务。

## 员工 Agent 页面

从导航进入「员工 Agent」，或访问 `http://127.0.0.1:5173/#agent`。填写员工 ID、会话 ID，发送排班或制度问题，可在同一会话中继续追问。员工身份通过 `X-Employee-Id` 请求头传入，请求体为 `conversationId` 和 `message`；两个 ID 均为 1–64 位字母、数字、下划线或短横线，消息最多 1,000 字符。

后端使用 ReactAgent 和 MemorySaver 按员工与会话隔离上下文，重启后端会丢失会话。新建会话生成新 ID；页面只展示本次打开后收到的记录，不提供历史记录加载或服务端会话删除。接口返回最终回答，不返回流式片段或工具执行轨迹。

换班仍需用户核对回答后，在「确认换班」区域提交令牌，复用 `/api/tools/shift-swaps/confirm`。切换身份或会话会清空当前展示内容与令牌。请求最多等待 120 秒，超时或离开页面不保证后端立即停止，重试前应检查处理结果。
