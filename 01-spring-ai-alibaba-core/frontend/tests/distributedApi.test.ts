import { afterEach, expect, it, vi } from 'vitest'
import { callDistributed, extractAnswer } from '../src/lib/distributedApi'
afterEach(() => vi.unstubAllGlobals())
it('MCP 使用 POST JSON，不附加第一章身份头', async () => {
  const fetch = vi.fn().mockResolvedValue(new Response('{"content":"白班"}'))
  vi.stubGlobal('fetch', fetch)
  await callDistributed('mcp', '员工 E1001')
  expect(fetch).toHaveBeenCalledWith(
    '/api/mcp/chat',
    expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"message":"员工 E1001"}',
    }),
  )
})
it('A2A 正确编码查询参数并传递取消信号', async () => {
  const fetch = vi.fn().mockResolvedValue(new Response('[]'))
  vi.stubGlobal('fetch', fetch)
  const signal = new AbortController().signal
  await callDistributed('a2a', '年假 & 结转？', signal)
  const [url, options] = fetch.mock.calls[0]!
  expect(new URL(url, 'http://localhost').searchParams.get('question')).toBe('年假 & 结转？')
  expect(options).toEqual({ method: 'GET', signal })
})
it('从助手消息中提取文本，不将用户问题或工具消息作为回答', () => {
  expect(
    extractAnswer([
      { messageType: 'USER', text: '问题' },
      { messageType: 'ASSISTANT', text: '回答' },
      { messageType: 'TOOL', text: '内部结果' },
    ]),
  ).toBe('回答')
  expect(extractAnswer({ content: 'MCP回答' })).toBe('MCP回答')
  expect(extractAnswer({ unknown: 123 })).toBe('')
  expect(extractAnswer(null)).toBe('')
})
it('保留服务端错误与非 JSON 详情', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response('proxy unavailable', { status: 502 })),
  )
  await expect(callDistributed('mcp', '问题')).rejects.toMatchObject({
    status: 502,
    detail: 'proxy unavailable',
  })
})
