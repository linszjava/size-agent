# Size Agent

基于 Spring Boot、Spring AI Alibaba、Vue 3 和 TypeScript 的 AI 应用学习项目。项目按功能阶段逐步实现模型调用、流式输出、对话记忆和 Tool Calling，并为后端接口提供本地文档与调试页面。

## 项目结构

```text
size-agent/
└── 01-spring-ai-alibaba-core/
    ├── backend/    # Spring Boot 后端
    ├── frontend/   # Vue 3 + TypeScript 前端
    └── sql/        # MySQL 初始化脚本
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

标签记录的是固定的代码快照。签出标签后会进入 detached HEAD 状态；如果需要在历史版本上继续开发，应从该标签新建分支。

## 启动项目

### 后端

使用 IDEA 打开 `01-spring-ai-alibaba-core/backend`，配置所需环境变量后运行 `AliyunApplication`。后端默认监听 `8888` 端口。

### 前端

需要 Node.js 22.12+。在仓库根目录执行：

```bash
cd 01-spring-ai-alibaba-core/frontend
npm ci
npm run dev
```

默认页面地址为 `http://127.0.0.1:5173`。前端通过 Vite 将 `/api` 请求代理到 `http://localhost:8888`。

## 已适配接口

| 页面       | 方法   | 路径                                         | 响应              |
| ---------- | ------ | -------------------------------------------- | ----------------- |
| 基础对话   | POST   | `/api/chat/v1`                               | ChatResponseDto   |
| ChatModel  | POST   | `/api/core/chat-model`                       | ChatResponseDto   |
| ChatClient | POST   | `/api/core/chat-client`                      | ChatResponseDto   |
| 消息历史   | POST   | `/api/core/messages`                         | ChatResponseDto   |
| 流式输出   | POST   | `/api/core/stream`                           | text/event-stream |
| 记忆对话   | POST   | `/api/memory/chat`                           | JSON              |
| 记忆流式   | POST   | `/api/memory/stream`                         | text/event-stream |
| 查询记忆   | GET    | `/api/memory/conversations/{conversationId}` | JSON              |
| 清空记忆   | DELETE | `/api/memory/conversations/{conversationId}` | 204               |
| 工具调用   | POST   | `/api/tools/chat`                            | JSON              |
| 确认换班   | POST   | `/api/tools/shift-swaps/confirm`             | JSON              |
| 文档上传 | POST | `/api/knowledge/documents` | KnowledgeUploadResponse |
| 知识问答 | POST | `/api/knowledge/ask` | RagAnswerResponse |

## 前端检查与构建

```bash
cd 01-spring-ai-alibaba-core/frontend
npm test
npm run build
```

更完整的前端开发说明见 [`01-spring-ai-alibaba-core/frontend/README.md`](01-spring-ai-alibaba-core/frontend/README.md)。
