export type DistributedMode = 'mcp' | 'a2a'
export class DistributedApiError extends Error {
  constructor(
    message: string,
    public detail: string,
    public status: number,
  ) {
    super(message)
  }
}
export async function callDistributed(
  mode: DistributedMode,
  message: string,
  signal?: AbortSignal,
): Promise<unknown> {
  const response = await fetch(
    mode === 'mcp' ? '/api/mcp/chat' : `/api/a2a/hr?${new URLSearchParams({ question: message })}`,
    mode === 'mcp'
      ? {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message }),
          signal,
        }
      : { method: 'GET', signal },
  )
  const raw = await response.text()
  if (!response.ok) {
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const data = JSON.parse(raw)
      message = data.message || data.error || message
    } catch {
      /* 保留原始错误 */
    }
    throw new DistributedApiError(message, raw, response.status)
  }
  try {
    return JSON.parse(raw)
  } catch {
    throw new DistributedApiError('接口未返回有效 JSON。', raw, response.status)
  }
}
export function extractAnswer(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value)) return value.map(extractAnswer).filter(Boolean).join('\n\n')
  if (!value || typeof value !== 'object') return ''
  const item = value as Record<string, unknown>
  const role =
    item.messageType ??
    item.role ??
    (item.metadata as Record<string, unknown> | undefined)?.messageType
  if (typeof role === 'string' && role.toLowerCase() !== 'assistant') return ''
  if (typeof item.text === 'string') return item.text
  if (typeof item.content === 'string') return item.content
  if (Array.isArray(item.content)) return extractAnswer(item.content)
  if (item.messages !== undefined) return extractAnswer(item.messages)
  return ''
}
