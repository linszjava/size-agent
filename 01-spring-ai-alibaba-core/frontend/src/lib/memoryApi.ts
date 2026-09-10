export interface MemoryMessage {
  type: string
  text: string
}

export class MemoryApiError extends Error {
  constructor(
    message: string,
    public detail = '',
  ) {
    super(message)
  }
}

async function checked(response: Response) {
  if (response.ok) return response
  const detail = await response.text()
  let message = `请求失败（HTTP ${response.status}）`
  try {
    const body = JSON.parse(detail)
    message = body.message || body.error || message
  } catch {
    /* 保留原始响应 */
  }
  throw new MemoryApiError(message, detail)
}

const headers = (userId: string, json = false) => ({
  'X-User-Id': userId,
  ...(json ? { 'Content-Type': 'application/json' } : {}),
})

export async function memoryChat(userId: string, conversationId: string, message: string) {
  const response = await checked(
    await fetch('/api/memory/chat', {
      method: 'POST',
      headers: headers(userId, true),
      body: JSON.stringify({ conversationId, message }),
    }),
  )
  return (await response.json()) as { conversationId: string; content: string }
}

export async function memoryHistory(userId: string, conversationId: string) {
  const response = await checked(
    await fetch(`/api/memory/conversations/${encodeURIComponent(conversationId)}`, {
      headers: headers(userId),
    }),
  )
  return (await response.json()) as MemoryMessage[]
}

export async function clearMemory(userId: string, conversationId: string) {
  await checked(
    await fetch(`/api/memory/conversations/${encodeURIComponent(conversationId)}`, {
      method: 'DELETE',
      headers: headers(userId),
    }),
  )
}

export async function streamMemory(
  userId: string,
  conversationId: string,
  message: string,
  onChunk: (value: string) => void,
  signal: AbortSignal,
) {
  const response = await checked(
    await fetch('/api/memory/stream', {
      method: 'POST',
      headers: { ...headers(userId, true), Accept: 'text/event-stream' },
      body: JSON.stringify({ conversationId, message }),
      signal,
    }),
  )
  if (!response.body) throw new MemoryApiError('浏览器无法读取响应流。')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    // Spring WebFlux 的 Flux<String> 可能以 SSE data 行返回，也可能直接返回文本块。
    const text = decoder.decode(value, { stream: true })
    const chunks = text.includes('data:')
      ? text
          .split(/\r?\n/)
          .filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5).replace(/^ /, ''))
      : [text]
    chunks.forEach(onChunk)
  }
}
