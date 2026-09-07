export interface ChatPayload {
  message: string
  systemPrompt?: string
  temperature?: number
  topP?: number
  maxTokens?: number
  stopSequences?: string[]
}

export interface ChatResponse {
  content: string
  model?: string | null
  promptTokens?: number | null
  completionTokens?: number | null
  totalTokens?: number | null
  finishReason?: string | null
}

export interface Draft {
  message: string
  systemPrompt: string
  temperature: number | string
  topP: number | string
  maxTokens: number | string
  stopSequences: string
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public detail: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export function buildPayload(draft: Draft, basic = false): ChatPayload {
  if (!draft.message.trim()) throw new Error('请输入非空白的用户消息。')
  if (draft.message.length > 4000) throw new Error('用户消息不能超过 4,000 个字符。')
  const payload: ChatPayload = { message: draft.message }
  if (basic) return payload
  if (draft.systemPrompt.length > 1000) throw new Error('系统提示词不能超过 1,000 个字符。')
  if (draft.systemPrompt.trim()) payload.systemPrompt = draft.systemPrompt
  for (const [name, min, max, exclusive, integer] of [
    ['temperature', 0, 2, false, false],
    ['topP', 0, 1, true, false],
    ['maxTokens', 1, 4000, false, true],
  ] as const) {
    const raw = draft[name]
    if (raw === '' || (typeof raw === 'string' && !raw.trim())) continue
    const value = Number(raw)
    if (
      !Number.isFinite(value) ||
      value > max ||
      (exclusive ? value <= min : value < min) ||
      (integer && !Number.isInteger(value))
    ) {
      throw new Error(
        `${name} 必须${exclusive ? '大于' : '不小于'} ${min} 且不超过 ${max}${integer ? '，并且为整数' : ''}。`,
      )
    }
    payload[name] = value
  }
  if (draft.stopSequences.trim()) {
    const stops = draft.stopSequences.split(/\r?\n/)
    if (stops.length > 4 || stops.some((item) => !item.trim()))
      throw new Error('停止序列最多 4 行，每行都需要有非空白内容。')
    payload.stopSequences = stops
  }
  return payload
}

export function buildCurl(path: string, payload: ChatPayload, stream = false) {
  const body = JSON.stringify(payload, null, 2).replace(/'/g, `'"'"'`)
  return `curl ${stream ? '-N ' : ''}-X POST 'http://localhost:8888${path}' \\\n  -H 'Content-Type: application/json' \\\n  -H 'Accept: ${stream ? 'text/event-stream' : 'application/json'}' \\\n  --data-raw '${body}'`
}

/** Incremental SSE parser: preserves spaces and handles CR, LF and CRLF across chunks. */
export function createSseParser(onEvent: (data: string, event: string) => void) {
  let pending = ''
  let data: string[] = []
  let event = 'message'
  function line(value: string) {
    if (!value) {
      if (data.length) onEvent(data.join('\n'), event)
      data = []
      event = 'message'
      return
    }
    if (value.startsWith(':')) return
    const colon = value.indexOf(':')
    const field = colon < 0 ? value : value.slice(0, colon)
    let content = colon < 0 ? '' : value.slice(colon + 1)
    if (content.startsWith(' ')) content = content.slice(1)
    if (field === 'data') data.push(content)
    if (field === 'event') event = content
  }
  return {
    push(chunk: string, final = false) {
      pending += chunk
      let start = 0
      for (let index = 0; index < pending.length; index++) {
        const char = pending[index]
        if (char !== '\r' && char !== '\n') continue
        if (char === '\r' && index === pending.length - 1 && !final) break
        line(pending.slice(start, index))
        if (char === '\r' && pending[index + 1] === '\n') index++
        start = index + 1
      }
      pending = pending.slice(start)
      if (final) {
        if (pending) line(pending)
        pending = ''
        line('')
      }
    },
  }
}

export async function callChat(options: {
  path: string
  payload: ChatPayload
  stream: boolean
  signal: AbortSignal
  onHeaders?: (status: number) => void
  onChunk?: (text: string) => void
}): Promise<ChatResponse | null> {
  const response = await fetch(options.path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: options.stream ? 'text/event-stream' : 'application/json',
    },
    body: JSON.stringify(options.payload),
    signal: options.signal,
  })
  options.onHeaders?.(response.status)
  if (!response.ok) {
    const raw = await response.text()
    let message = `请求失败（HTTP ${response.status}）`
    let detail = raw || '服务端未提供错误详情。'
    try {
      const body = JSON.parse(raw)
      message = body.message || body.error || message
      detail = JSON.stringify(body, null, 2)
    } catch {
      /* HTML / plain text errors remain available in the raw response. */
    }
    throw new ApiError(message, response.status, detail)
  }
  if (!options.stream) {
    const raw = await response.text()
    let body: unknown
    try {
      body = JSON.parse(raw)
    } catch {
      throw new ApiError('接口未返回有效 JSON，请检查后端地址与代理配置。', response.status, raw)
    }
    if (
      !body ||
      typeof body !== 'object' ||
      !('content' in body) ||
      typeof body.content !== 'string'
    ) {
      throw new ApiError('响应格式与 ChatResponseDto 不一致。', response.status, raw)
    }
    return body as ChatResponse
  }
  if (!response.headers.get('content-type')?.includes('text/event-stream')) {
    throw new ApiError(
      '接口未返回 SSE 流，请检查后端响应。',
      response.status,
      await response.text(),
    )
  }
  if (!response.body) throw new ApiError('当前环境无法读取响应流。', response.status, '')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let doneMarker = false
  const parser = createSseParser((data, event) => {
    if (doneMarker) return
    if (event === 'error') throw new ApiError('流式响应发生错误。', response.status, data)
    if (data === '[DONE]') {
      doneMarker = true
      return
    }
    options.onChunk?.(data)
  })
  try {
    while (!doneMarker) {
      const { value, done } = await reader.read()
      parser.push(decoder.decode(value, { stream: !done }), done)
      if (done) break
    }
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
  return null
}
