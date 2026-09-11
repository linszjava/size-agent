import { afterEach, expect, it, vi } from 'vitest'
import { askKnowledge, uploadKnowledge } from '../src/lib/knowledgeApi'
const scope = { tenantId: 'tenant-001', departmentId: 'department-001' }
afterEach(() => vi.unstubAllGlobals())
it('上传使用 multipart 并保留浏览器生成的 boundary，同时携带知识范围', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(new Response(JSON.stringify({ documentId: 'doc-1', chunkCount: 2 })))
  vi.stubGlobal('fetch', fetch)
  const file = new File(['hello'], 'guide.md')
  await uploadKnowledge(scope, file, 'DEPARTMENT')
  const [path, options] = fetch.mock.calls[0]!
  expect(path).toBe('/api/knowledge/documents')
  expect(options.headers).toEqual({
    'X-Tenant-Id': 'tenant-001',
    'X-Department-Id': 'department-001',
  })
  expect(options.body.get('file')).toBe(file)
  expect(options.body.get('visibility')).toBe('DEPARTMENT')
})
it('问答提交 question 字段并保留可空的来源信息', async () => {
  const result = {
    answer: '回答',
    sources: [{ documentId: 'd', fileName: 'a.md', pageNumber: null, score: null }],
  }
  const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify(result)))
  vi.stubGlobal('fetch', fetch)
  await expect(askKnowledge(scope, '问题')).resolves.toEqual(result)
  expect(fetch.mock.calls[0]![0]).toBe('/api/knowledge/ask')
  expect(fetch.mock.calls[0]![1]).toMatchObject({
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question: '问题' }),
  })
})
it('保留后端错误消息及非 JSON 错误详情', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValueOnce(new Response('{"message":"上传文件不能为空"}', { status: 400 }))
      .mockResolvedValueOnce(new Response('proxy error', { status: 502 })),
  )
  await expect(askKnowledge(scope, '问题')).rejects.toThrow('上传文件不能为空')
  await expect(askKnowledge(scope, '问题')).rejects.toMatchObject({
    detail: 'proxy error',
    message: '请求失败（HTTP 502）',
  })
})
