import { expect, it, vi } from 'vitest'
import { registerApiNavigation } from '../src/lib/webmcp'
import type { ToolContext } from '../src/lib/webmcp'

it('可选工具复用接口导航，拒绝无效输入并支持生命周期清理', async () => {
  const registerTool = vi.fn<ToolContext['registerTool']>()
  const select = vi.fn().mockResolvedValue(undefined)
  const cleanup = registerApiNavigation({ registerTool }, select)
  const [tool, options] = registerTool.mock.calls[0]!
  expect(tool.name).toBe('open_api_documentation')
  expect(tool.annotations.readOnlyHint).toBe(false)
  expect(tool.inputSchema).toMatchObject({ additionalProperties: false, required: ['endpointId'] })
  await expect(tool.execute({ endpointId: 'stream' })).resolves.toMatchObject({
    path: '/api/core/stream',
    requestSent: false,
  })
  expect(select).toHaveBeenCalledExactlyOnceWith('stream')
  await expect(tool.execute({ endpointId: 'missing' })).rejects.toThrow('接口不存在')
  await expect(tool.execute({ endpointId: 'chat', message: 'unexpected' })).rejects.toThrow()
  expect(select).toHaveBeenCalledTimes(1)
  cleanup()
  expect(options.signal.aborted).toBe(true)
})

it('不支持 WebMCP 的浏览器不受影响', () => {
  const select = vi.fn()
  const cleanup = registerApiNavigation(undefined, select)
  expect(() => cleanup()).not.toThrow()
  expect(select).not.toHaveBeenCalled()
})
