export interface AgentChatResponse {
  conversationId: string
  content: string
}
export class AgentApiError extends Error {
  constructor(
    message: string,
    public detail = '',
  ) {
    super(message)
  }
}

async function request<T>(
  path: string,
  employeeId: string,
  body: object,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json', 'X-Employee-Id': employeeId },
    body: JSON.stringify(body),
  })
  const raw = await response.text()
  if (!response.ok) {
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const error = JSON.parse(raw)
      message = error.message || error.error || message
    } catch {
      // 非 JSON 错误保留在 detail 中。
    }
    throw new AgentApiError(message, raw)
  }
  try {
    return JSON.parse(raw) as T
  } catch {
    throw new AgentApiError('接口未返回有效 JSON。', raw)
  }
}

export function callEmployeeAgent(
  employeeId: string,
  conversationId: string,
  message: string,
  signal?: AbortSignal,
) {
  return request<AgentChatResponse>(
    '/api/agents/employee/chat',
    employeeId,
    { conversationId, message },
    signal,
  )
}
