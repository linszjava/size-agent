import { endpoints } from '../data/endpoints'

export interface ToolContext {
  registerTool(
    tool: {
      name: string
      title: string
      description: string
      inputSchema: object
      annotations: { readOnlyHint: boolean; untrustedContentHint: boolean }
      execute: (input: unknown) => Promise<unknown>
    },
    options: { signal: AbortSignal },
  ): void | Promise<void>
}

/** Optional browser capability; unsupported browsers use the normal navigation. */
export function registerApiNavigation(
  context: ToolContext | undefined,
  select: (id: string) => Promise<void>,
) {
  const lifecycle = new AbortController()
  if (context?.registerTool) {
    try {
      void Promise.resolve(
        context.registerTool(
          {
            name: 'open_api_documentation',
            title: '打开接口文档与调试表单',
            description:
              '打开 Size Agent 中指定接口的文档和调试表单。仅切换页面，不发送模型请求；切换会取消正在接收的请求并清空响应面板。',
            inputSchema: {
              type: 'object',
              properties: {
                endpointId: { type: 'string', enum: endpoints.map((item) => item.id) },
              },
              required: ['endpointId'],
              additionalProperties: false,
            },
            annotations: { readOnlyHint: false, untrustedContentHint: false },
            async execute(input) {
              if (
                !input ||
                typeof input !== 'object' ||
                Array.isArray(input) ||
                Object.keys(input).some((key) => key !== 'endpointId') ||
                !('endpointId' in input)
              )
                throw new Error('需要提供 endpointId。')
              const endpoint = endpoints.find((item) => item.id === input.endpointId)
              if (!endpoint) throw new Error('接口不存在。')
              await select(endpoint.id)
              return {
                endpointId: endpoint.id,
                title: endpoint.title,
                method: endpoint.method ?? 'POST',
                path: endpoint.path,
                requestSent: false,
              }
            },
          },
          { signal: lifecycle.signal },
        ),
      ).catch((error) => console.warn('可选接口导航工具未注册', error))
    } catch (error) {
      console.warn('可选接口导航工具未注册', error)
    }
  }
  return () => lifecycle.abort()
}
