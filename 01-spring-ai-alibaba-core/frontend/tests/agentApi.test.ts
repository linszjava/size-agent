import { afterEach, expect, it, vi } from 'vitest'
import { callEmployeeAgent } from '../src/lib/agentApi'
import { confirmShiftSwap } from '../src/lib/toolApi'
afterEach(() => vi.unstubAllGlobals())
it('员工身份通过请求头传入，会话及消息放在 JSON 中，并支持取消', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(
      new Response(JSON.stringify({ conversationId: 'c-1', content: '由主管审批' })),
    )
  vi.stubGlobal('fetch', fetch)
  const signal = new AbortController().signal
  await expect(callEmployeeAgent('employee-001', 'c-1', '谁审批？', signal)).resolves.toMatchObject(
    { conversationId: 'c-1', content: '由主管审批' },
  )
  expect(fetch).toHaveBeenCalledWith('/api/agents/employee/chat', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json', 'X-Employee-Id': 'employee-001' },
    body: JSON.stringify({ conversationId: 'c-1', message: '谁审批？' }),
  })
})
it('展示后端错误消息并保留原始详情', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response('{"message":"Agent 执行失败"}', { status: 502 })),
  )
  await expect(callEmployeeAgent('e', 'c', '问题')).rejects.toMatchObject({
    message: 'Agent 执行失败',
    detail: '{"message":"Agent 执行失败"}',
  })
})
it('非 JSON 响应报告解析失败', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('invalid')))
  await expect(callEmployeeAgent('e', 'c', '问题')).rejects.toThrow('有效 JSON')
})
it('换班确认复用员工身份与取消信号', async () => {
  const fetch = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetch)
  const signal = new AbortController().signal
  await confirmShiftSwap('e', 'token', signal)
  expect(fetch.mock.calls[0]![1]).toMatchObject({
    signal,
    headers: { 'X-Employee-Id': 'e' },
    body: '{"confirmationToken":"token"}',
  })
})
