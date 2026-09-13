export interface ShiftWorkflowResponse {
  workflowId: string
  status: string
  currentShift: string
  targetShift: string
  policy: string
  requestId: string
  message: string
}
export interface ShiftWorkflowRequest {
  shiftDate: string
  targetShift: '白班' | '晚班'
  reason: string
}
export class WorkflowApiError extends Error {
  constructor(
    message: string,
    public detail = '',
  ) {
    super(message)
  }
}

async function request<T>(
  path: string,
  employeeId: string,
  body: object,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json', 'X-Employee-Id': employeeId },
    body: JSON.stringify(body),
  })
  const raw = await response.text()
  if (!response.ok) {
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const error = JSON.parse(raw)
      message = error.message || error.error || message
    } catch {
      // 非 JSON 错误保留在 detail 中。
    }
    throw new WorkflowApiError(message, raw)
  }
  try {
    return JSON.parse(raw) as T
  } catch {
    throw new WorkflowApiError('接口未返回有效 JSON。', raw)
  }
}

export function startWorkflow(
  employeeId: string,
  body: ShiftWorkflowRequest,
  signal?: AbortSignal,
) {
  return request<ShiftWorkflowResponse>('/api/workflows/shift-swaps', employeeId, body, signal)
}
export function approveWorkflow(
  employeeId: string,
  workflowId: string,
  approved: boolean,
  signal?: AbortSignal,
) {
  return request<ShiftWorkflowResponse>(
    `/api/workflows/shift-swaps/${encodeURIComponent(workflowId)}/approval`,
    employeeId,
    { approved },
    signal,
  )
}
