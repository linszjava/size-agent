export interface Endpoint {
  id: string
  title: string
  label: string
  path: string
  description: string
  detail: string
  tag: string
  example: string
  stream?: boolean
  basic?: boolean
  java: string
}

export const endpoints: Endpoint[] = [
  {
    id: 'chat',
    title: '基础对话',
    label: '基础对话',
    path: '/api/chat/v1',
    tag: 'QUICK START',
    basic: true,
    description: '从一句提问开始，完成你的第一次模型调用。',
    detail:
      '最精简的对话接口。只需传入 message，后端使用已配置的 ChatClient 与默认模型参数，返回完整回答和 Token 使用情况。',
    example: '你好，请用三句话介绍 Spring AI Alibaba。',
    java: 'chatClient.prompt()\n    .user(request.message())\n    .call()\n    .chatResponse();',
  },
  {
    id: 'chat-model',
    title: 'ChatModel',
    label: 'ChatModel 调用',
    path: '/api/core/chat-model',
    tag: 'CORE API',
    description: '直接与模型对话，掌控每一次请求。',
    detail:
      'ChatModel 是 Spring AI 调用聊天模型的核心抽象。通过 Prompt 组合系统指令、用户消息和生成参数，一次请求即可获得结构化的模型响应。',
    example: 'ChatModel 的作用是什么？请用一个简单的 Java 示例解释。',
    java: 'List<Message> messages = List.of(\n    new SystemMessage(systemPrompt),\n    new UserMessage(request.message())\n);\n\nPrompt prompt = new Prompt(messages, options);\nChatResponse response = chatModel.call(prompt);',
  },
  {
    id: 'chat-client',
    title: 'ChatClient',
    label: 'ChatClient 调用',
    path: '/api/core/chat-client',
    tag: 'CORE API',
    description: '用流畅的链式 API，让模型调用更简洁。',
    detail:
      'ChatClient 在 ChatModel 之上提供流式构建 API。依次设置系统指令、用户输入和生成参数，再调用 call() 获取完整响应。',
    example: 'ChatClient 和 ChatModel 有什么区别？分别适合什么场景？',
    java: 'chatClient.prompt()\n    .system(systemPrompt)\n    .user(request.message())\n    .options(options)\n    .call()\n    .chatResponse();',
  },
  {
    id: 'messages',
    title: '消息历史',
    label: '消息历史',
    path: '/api/core/messages',
    tag: 'CONVERSATION',
    description: '把上下文带入对话，让回答有所依据。',
    detail:
      '通过按顺序传入 SystemMessage、UserMessage 和 AssistantMessage，让模型结合已有对话回答当前问题。当前接口使用后端固定的两条历史消息。',
    example: '结合我正在学习的框架，解释一下 Prompt 是什么。',
    java: 'List<Message> messages = List.of(\n    new SystemMessage(systemPrompt),\n    new UserMessage("我正在学习 Spring AI Alibaba。"),\n    new AssistantMessage("好的，我会结合 Spring AI Alibaba 进行讲解。"),\n    new UserMessage(request.message())\n);\nchatModel.call(new Prompt(messages, options));',
  },
  {
    id: 'stream',
    title: '流式输出',
    label: '流式输出',
    path: '/api/core/stream',
    tag: 'STREAMING',
    stream: true,
    description: '无需等待完整回答，实时接收模型的每一段输出。',
    detail:
      '通过 POST 请求获取 text/event-stream 响应。后端将 Flux<String> 持续写入 SSE 事件，调试台会按顺序拼接内容，并支持随时停止接收。',
    example: '请循序渐进地解释 Spring AI 中的流式响应，并给出使用建议。',
    java: 'return chatClient.prompt()\n    .system(systemPrompt)\n    .user(request.message())\n    .options(options)\n    .stream()\n    .content();',
  },
]

export const parameters = [
  {
    name: 'message',
    type: 'string',
    required: true,
    default: '必填',
    description: '本次发送给模型的用户消息。不能为空白，最多 4,000 个字符。',
  },
  {
    name: 'systemPrompt',
    type: 'string',
    required: false,
    default: '后端默认',
    description: '设定模型的角色与回答风格。留空使用后端系统提示词，最多 1,000 个字符。',
  },
  {
    name: 'temperature',
    type: 'number',
    required: false,
    default: '0.7',
    description:
      '控制回答的随机性。数值越低越稳定，越高越灵活。后端校验范围为 0–2，模型可能有更严格限制。',
  },
  {
    name: 'topP',
    type: 'number',
    required: false,
    default: '0.8',
    description: '控制候选词的累计概率范围。大于 0 且不超过 1，通常与 temperature 选择其一调整。',
  },
  {
    name: 'maxTokens',
    type: 'integer',
    required: false,
    default: '1000',
    description: '限制模型生成的最大 Token 数量。取值为 1–4,000，不等同于字符数量。',
  },
  {
    name: 'stopSequences',
    type: 'string[]',
    required: false,
    default: '不设置',
    description: '生成遇到指定字符串时停止。最多 4 项，每一项都不能是空白字符串。',
  },
]

export const responseFields = [
  ['content', 'string', '模型生成的回答正文'],
  ['model', 'string', '模型名称；上游未提供时可能为空'],
  ['promptTokens', 'number | null', '输入消耗的 Token 数'],
  ['completionTokens', 'number | null', '输出消耗的 Token 数'],
  ['totalTokens', 'number | null', '本次请求总 Token 数'],
  ['finishReason', 'string', '生成结束原因，例如 STOP'],
]
