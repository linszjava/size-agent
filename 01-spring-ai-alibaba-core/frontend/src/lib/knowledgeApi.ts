export interface KnowledgeScope {
  tenantId: string
  departmentId: string
}
export type KnowledgeVisibility = 'PUBLIC' | 'DEPARTMENT'
export interface KnowledgeUploadResponse {
  documentId: string
  fileName: string
  chunkCount: number
  status: string
}
export interface RagAnswerResponse {
  answer: string
  sources: {
    documentId: string
    fileName: string
    pageNumber: number | null
    score: number | null
  }[]
}
export class KnowledgeApiError extends Error {
  constructor(
    message: string,
    public detail = '',
  ) {
    super(message)
  }
}
async function request<T>(
  path: string,
  scope: KnowledgeScope,
  body: FormData | string,
  signal?: AbortSignal,
): Promise<T> {
  const headers: Record<string, string> = {
    'X-Tenant-Id': scope.tenantId,
    'X-Department-Id': scope.departmentId,
  }
  if (typeof body === 'string') headers['Content-Type'] = 'application/json'
  const response = await fetch(path, { method: 'POST', headers, body, signal })
  const raw = await response.text()
  if (!response.ok) {
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const error = JSON.parse(raw)
      message = error.message || error.error || message
    } catch {
      /* 保留非 JSON 错误详情。 */
    }
    throw new KnowledgeApiError(message, raw)
  }
  try {
    return JSON.parse(raw) as T
  } catch {
    throw new KnowledgeApiError('接口未返回有效 JSON。', raw)
  }
}
export function uploadKnowledge(
  scope: KnowledgeScope,
  file: File,
  visibility: KnowledgeVisibility,
  signal?: AbortSignal,
) {
  const body = new FormData()
  body.append('file', file)
  body.append('visibility', visibility)
  return request<KnowledgeUploadResponse>('/api/knowledge/documents', scope, body, signal)
}
export function askKnowledge(scope: KnowledgeScope, question: string, signal?: AbortSignal) {
  return request<RagAnswerResponse>(
    '/api/knowledge/ask',
    scope,
    JSON.stringify({ question }),
    signal,
  )
}
