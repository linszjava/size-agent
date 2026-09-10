# Size Agent Docs

当前 Spring Boot 项目的本地接口文档与调试台，使用 Vue 3、TypeScript 和 Vite。

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

## 启动

需要 Node.js 22.12+（或满足 Vite 8 要求的较新版本）。

1. 在 `01-spring-ai-alibaba-core/backend` 中运行 `AliyunApplication`，默认后端端口为 `8888`。
2. 在本目录执行：

```bash
npm ci
npm run dev
```

默认页面地址为 `http://127.0.0.1:5173`。若端口被占用，使用 `npm run dev -- --port 5174`，以终端实际打印的地址为准。

页面的相对 `/api` 请求由 Vite 代理至 `http://localhost:8888`，无需更改后端 CORS。可复制 `.env.example` 为 `.env.local` 修改 `API_PROXY_TARGET`，修改后重启 Vite。该变量仅在 Vite 服务端使用，模型密钥仍由后端管理；不要将密钥放入任何 `VITE_` 前缀变量。

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

提供按接口保存的本次页面会话表单、参数校验、JSON / cURL / Java 示例、复制、原始错误、耗时及 Token 用量。刷新页面会重置表单，不保存聊天内容到浏览器存储。`⌘/Ctrl + Enter` 发送，`⌘/Ctrl + K` 搜索接口。

消息历史由后端固定构造，前端不会伪造自动记忆功能。流式输出通过 `fetch` POST 和 SSE 增量解析读取，保留空格、中文和多行文本。停止或切换接口会中止当前前端请求，保留停止前已收到的输出（切换接口会清空响应面板）；停止接收不保证上游模型立即停止生成。请求等待上限为 120 秒。

后端未提供的模型名或 Token 显示“未提供”，不会生成模拟回答。SSE 目前只返回文本，没有 Token 统计。500 / 502 的完整上游原因可能仅存在于后端日志中，前端展示实际收到的错误。

## 检查与构建

```bash
npm test
npm run build
npm run preview
```

构建会先执行严格 TypeScript 检查，再生成 `dist/`。测试覆盖参数边界、HTTP 错误、UTF-8 分块、SSE 换行与取消信号。

`npm run preview` 默认监听 `4173`，保留同一后端代理规则。生产部署时，需要将 `dist/` 交给静态服务，并单独配置 `/api` 反向代理到 Spring Boot；Vite 开发代理不会被编译进静态文件。也可以将构建产物放入 Spring Boot 的静态资源目录由同源服务提供。当前实现按本地项目集成，未发布到外部托管服务。

## 扩展

- `src/data/endpoints.ts`：接口元数据、参数和示例。
- `src/lib/api.ts`：类型、请求校验、HTTP 客户端与 SSE 解析。
- `src/App.vue`：文档导航、请求表单与响应面板。
- `src/style.css`：文档站主题与响应式布局。

新增接口时先确认后端契约，再更新接口数据、对应表单与客户端类型。cURL 示例使用默认后端地址 `http://localhost:8888`，自定义代理目标时请同步调整复制后的命令地址。

参考：[Vue TypeScript](https://vuejs.org/guide/typescript/overview.html)、[Vite 代理配置](https://vite.dev/config/server-options.html#server-proxy)。

可选的 WebMCP 浏览器接口仅用于打开指定文档，不会发送模型请求。不支持该能力时不影响正常使用；其注册与参数契约已通过模拟上下文测试，未在实际支持 WebMCP 的浏览器中验证。
