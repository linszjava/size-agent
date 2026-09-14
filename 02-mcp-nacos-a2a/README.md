# 第 12 章：MCP、Nacos 与 A2A

这是一个独立的 Maven 多模块学习项目，版本固定为 Spring Boot `3.5.8`、Spring AI `1.1.2`、Spring AI Alibaba `1.1.2.2`、Java 21。

## 模块

| 模块 | 端口 | 作用 |
|---|---:|---|
| `employee-mcp-server` | 9001 | 暴露员工排班 MCP 工具，并注册到 Nacos |
| `employee-mcp-client` | 9002 | 从 Nacos 发现 MCP Server，让 ChatClient 调用远程工具 |
| `hr-a2a-server` | 9101 | 暴露人事专家 Agent，并注册 Agent Card |
| `employee-a2a-client` | 9102 | 从 Nacos 发现并调用远程人事 Agent |

## 前置条件

- JDK 21
- Maven 3.8+
- Docker
- 阿里云百炼 API Key
- Nacos 3.1.0+（Agent Registry 需要）

## 启动

```bash
cd /Volumes/ulinsz/idea/size-agent/02-mcp-nacos-a2a
docker compose up -d

export AI_DASHSCOPE_API_KEY='你的 Key'
export NACOS_USERNAME='nacos'
export NACOS_PASSWORD='nacos'
```

建议在四个终端按顺序启动：

```bash
mvn -pl employee-mcp-server spring-boot:run
mvn -pl hr-a2a-server spring-boot:run
mvn -pl employee-mcp-client spring-boot:run
mvn -pl employee-a2a-client spring-boot:run
```

## 验证 MCP

```bash
curl -X POST http://localhost:9002/api/mcp/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询员工 E1001 今天的排班，必须调用工具"}'
```

调用链：

```text
HTTP → employee-mcp-client → ChatModel 决定调用工具
     → Nacos 发现 employee-mcp-server → MCP → queryEmployeeSchedule
     → 工具结果返回模型 → 最终回答
```

## 验证 A2A

```bash
curl -G http://localhost:9102/api/a2a/hr \
  --data-urlencode 'question=公司年假制度是什么？'
```

调用链：

```text
HTTP → employee-a2a-client → Nacos 查询 hr-specialist
     → A2A JSON-RPC → hr-a2a-server → ReactAgent → 回答
```

## 注意

- 示例排班数据是内存数据，只用于观察跨进程调用。
- MCP 工具中的 `employeeId` 是学习参数。生产系统必须从可信登录上下文获得员工身份，不能相信模型生成的身份。
- 不要把 Nacos 和 DashScope 密码提交到 Git。
- A2A 和 MCP 的客户端、服务端版本必须保持一致。
- MCP 分布式客户端的 `configs` 必须配置在 `spring.ai.alibaba.mcp.nacos.client` 下；并且
  `configs.employee-server` 与 `streamable.connections.employee-server` 的键必须相同，否则无法取得
  `NacosMcpOperationService`，启动时会报 `nacosMcpOperationService cannot be null`。
- 本地示例把 MCP Server 注册为 `127.0.0.1:9001`，避免 macOS 系统代理接管对局域网地址的请求而产生
  `502 Bad Gateway`。容器或多机部署时设置 `NACOS_MCP_SERVER_HOST` 和
  `NACOS_MCP_SERVER_PORT`，注册地址必须是 MCP Client 实际能够访问的地址。
