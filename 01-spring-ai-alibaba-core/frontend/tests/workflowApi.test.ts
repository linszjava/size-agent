import { afterEach, expect, it, vi } from 'vitest'
import { startWorkflow, approveWorkflow } from '../src/lib/workflowApi'
afterEach(() => vi.unstubAllGlobals())
it('创建流程携带员工请求头与日期班次原因', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(new Response('{"workflowId":"w1","status":"WAITING_EMPLOYEE_CONFIRMATION"}'))
  vi.stubGlobal('fetch', fetch)
  const body = { shiftDate: '2026-09-15', targetShift: '白班' as const, reason: '个人安排' }
  await expect(startWorkflow('e1', body)).resolves.toMatchObject({
    workflowId: 'w1',
    status: 'WAITING_EMPLOYEE_CONFIRMATION',
  })
  expect(fetch).toHaveBeenCalledWith(
    '/api/workflows/shift-swaps',
    expect.objectContaining({
      headers: { 'Content-Type': 'application/json', 'X-Employee-Id': 'e1' },
      body: JSON.stringify(body),
    }),
  )
})
it.each([true, false])('确认结果 %s 使用布尔值并携带取消信号', async (approved) => {
  const fetch = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetch)
  const signal = new AbortController().signal
  await approveWorkflow('e1', 'w1', approved, signal)
  expect(fetch).toHaveBeenCalledWith(
    '/api/workflows/shift-swaps/w1/approval',
    expect.objectContaining({ signal, body: JSON.stringify({ approved }) }),
  )
})
it('返回后端错误信息并保留原始响应', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response('{"message":"工作流不存在"}', { status: 400 })),
  )
  await expect(approveWorkflow('e1', 'w1', true)).rejects.toMatchObject({
    message: '工作流不存在',
    detail: '{"message":"工作流不存在"}',
  })
})
