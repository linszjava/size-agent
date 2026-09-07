import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, buildCurl, buildPayload, callChat, createSseParser } from '../src/lib/api'
import type { Draft } from '../src/lib/api'

const draft = (overrides: Partial<Draft> = {}): Draft => ({
  message: '你好',
  systemPrompt: '',
  temperature: 0.7,
  topP: 0.8,
  maxTokens: 1000,
  stopSequences: '',
  ...overrides,
})
afterEach(() => vi.unstubAllGlobals())

describe('请求参数与后端约束', () => {
  it('基础接口只发送 message', () => {
    expect(buildPayload(draft({ systemPrompt: '助手', stopSequences: 'END' }), true)).toEqual({
      message: '你好',
    })
  })
  it('空可选项不发送，让后端使用默认值', () => {
    expect(buildPayload(draft({ temperature: '', topP: '', maxTokens: '' }))).toEqual({
      message: '你好',
    })
  })
  it('接受 temperature 为 0、小数 topP 及最大 token 上界', () => {
    expect(buildPayload(draft({ temperature: 0, topP: 0.001, maxTokens: 4000 }))).toMatchObject({
      temperature: 0,
      topP: 0.001,
      maxTokens: 4000,
    })
  })
  it.each([
    { message: ' \n\t' },
    { message: '字'.repeat(4001) },
    { systemPrompt: '字'.repeat(1001) },
    { temperature: -0.1 },
    { temperature: 2.1 },
    { topP: 0 },
    { topP: 1.1 },
    { maxTokens: 0 },
    { maxTokens: 4001 },
    { maxTokens: 2.5 },
    { temperature: 'NaN' },
    { stopSequences: 'a\nb\nc\nd\ne' },
    { stopSequences: 'END\n \nSTOP' },
  ])('拒绝不合法参数 %j', (value) => {
    expect(() => buildPayload(draft(value))).toThrow()
  })
  it('保留停止序列中的有效空格', () => {
    expect(buildPayload(draft({ stopSequences: ' END \r\nSTOP' })).stopSequences).toEqual([
      ' END ',
      'STOP',
    ])
  })
  it('cURL 转义单引号，流式请求添加 -N', () => {
    expect(buildCurl('/api/core/stream', { message: "what's next?" }, true)).toContain('curl -N')
    expect(buildCurl('/api/chat/v1', { message: "what's next?" })).toContain(`what'"'"'s next?`)
  })
})

describe('SSE 分段解析', () => {
  it('逐字符接收 CRLF、多行事件及注释，保留文本空格', () => {
    const events: string[] = []
    const parser = createSseParser((text) => events.push(text))
    for (const char of ': heartbeat\r\ndata:  你好 \r\ndata:世界\r\n\r\ndata: !\r\n\r\n')
      parser.push(char)
    parser.push('', true)
    expect(events).toEqual([' 你好 \n世界', '!'])
  })
  it('处理 CR 换行以及末尾无空行的事件', () => {
    const events: string[] = []
    const parser = createSseParser((text) => events.push(text))
    parser.push('data: hello\r\rdata: world', true)
    expect(events).toEqual(['hello', 'world'])
  })
  it('传递 error 事件类型，并在事件结束后重置类型', () => {
    const events: [string, string][] = []
    const parser = createSseParser((data, event) => events.push([data, event]))
    parser.push('event: error\ndata: broken\n\ndata: normal\n\n', true)
    expect(events).toEqual([
      ['broken', 'error'],
      ['normal', 'message'],
    ])
  })
})

describe('HTTP 与流式调用', () => {
  const options = () => ({
    path: '/api/core/chat-model',
    payload: { message: '你好' },
    stream: false,
    signal: new AbortController().signal,
  })
  it('发送 JSON POST 请求并读取真实响应字段', async () => {
    const result = { content: '回答', model: '', totalTokens: null }
    const fetchMock = vi.fn().mockResolvedValue(Response.json(result))
    vi.stubGlobal('fetch', fetchMock)
    expect(await callChat(options())).toEqual(result)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/core/chat-model',
      expect.objectContaining({ method: 'POST', body: '{"message":"你好"}' }),
    )
  })
  it('保留 Spring 校验错误的状态码、信息及 requestId', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          Response.json({ message: 'message 不能为空', requestId: 'test-id' }, { status: 400 }),
        ),
    )
    await expect(callChat(options())).rejects.toMatchObject({
      status: 400,
      message: 'message 不能为空',
      detail: expect.stringContaining('test-id'),
    })
  })
  it('保留后端 500 默认错误信息', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(Response.json({ error: 'Internal Server Error' }, { status: 500 })),
    )
    await expect(callChat(options())).rejects.toMatchObject({
      status: 500,
      message: 'Internal Server Error',
    })
  })
  it('拒绝返回 HTML 的错误代理配置', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html>not an API</html>')))
    await expect(callChat(options())).rejects.toBeInstanceOf(ApiError)
  })
  it('拒绝格式错误的成功响应', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ answer: 'not content' })))
    await expect(callChat(options())).rejects.toMatchObject({
      message: '响应格式与 ChatResponseDto 不一致。',
    })
  })
  it('UTF-8 中文按字节切分后无乱码，DONE 后的内容不会追加', async () => {
    const bytes = new TextEncoder().encode(
      'data: 你好\n\ndata:  world\n\ndata: [DONE]\n\ndata: ignored\n\n',
    )
    const body = new ReadableStream({
      start(controller) {
        for (const byte of bytes) controller.enqueue(new Uint8Array([byte]))
        controller.close()
      },
    })
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(body, { headers: { 'Content-Type': 'text/event-stream;charset=UTF-8' } }),
        ),
    )
    let text = ''
    const result = await callChat({
      ...options(),
      stream: true,
      onChunk: (chunk) => {
        text += chunk
      },
    })
    expect(result).toBeNull()
    expect(text).toBe('你好 world')
  })
  it('处理流内错误，同时保留已接收内容', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response('data: partial\n\nevent: error\ndata: upstream failed\n\n', {
            headers: { 'Content-Type': 'text/event-stream' },
          }),
        ),
    )
    const onChunk = vi.fn()
    await expect(callChat({ ...options(), stream: true, onChunk })).rejects.toMatchObject({
      detail: 'upstream failed',
    })
    expect(onChunk).toHaveBeenCalledWith('partial')
  })
  it('流式接口返回非 SSE 时明确失败', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ content: 'plain json' })))
    await expect(callChat({ ...options(), stream: true })).rejects.toMatchObject({
      message: '接口未返回 SSE 流，请检查后端响应。',
    })
  })
  it('传递同一个 AbortSignal，允许取消进行中的请求', async () => {
    const controller = new AbortController()
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_path, init: RequestInit) =>
          new Promise((_resolve, reject) => {
            init.signal!.addEventListener(
              'abort',
              () => reject(new DOMException('Aborted', 'AbortError')),
              { once: true },
            )
          }),
      ),
    )
    const result = callChat({ ...options(), signal: controller.signal })
    controller.abort()
    await expect(result).rejects.toMatchObject({ name: 'AbortError' })
  })
})
