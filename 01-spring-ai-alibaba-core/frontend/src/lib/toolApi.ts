export interface SwapResult {
  requestId: string
  employeeId: string
  shiftDate: string
  targetShift: string
  status: string
}

export class ToolApiError extends Error {
  constructor(
    message: string,
    public detail = '',
  ) {
    super(message)
  }
}

async function request<T>(path: string, employeeId: string, body: object): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
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
    throw new ToolApiError(message, raw)
  }
  try {
    return JSON.parse(raw) as T
  } catch {
    throw new ToolApiError('接口未返回有效 JSON。', raw)
  }
}

export function callToolChat(employeeId: string, message: string) {
  return request<{ content: string }>('/api/tools/chat', employeeId, { message })
}

export function confirmShiftSwap(employeeId: string, confirmationToken: string) {
  return request<SwapResult>('/api/tools/shift-swaps/confirm', employeeId, { confirmationToken })
}
