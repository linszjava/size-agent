<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Layers, LoaderCircle, CheckCircle2, X, Play } from '@lucide/vue'
import { startWorkflow, approveWorkflow, WorkflowApiError } from '../lib/workflowApi'
import type { ShiftWorkflowResponse } from '../lib/workflowApi'
const employeeId = ref('employee-001')
const shiftDate = ref(new Date().toLocaleDateString('sv-SE'))
const targetShift = ref<'白班' | '晚班'>('白班')
const reason = ref('因个人安排申请调整班次。')
const workflowId = ref('')
const response = ref<ShiftWorkflowResponse | null>(null)
const submitted = ref('')
const error = ref('')
const detail = ref('')
const pending = ref<'start' | 'approve' | 'reject' | null>(null)
const busy = computed(() => pending.value !== null)
const idPattern = /^[A-Za-z0-9_-]{1,64}$/
const waiting = computed(() => response.value?.status === 'WAITING_EMPLOYEE_CONFIRMATION')
const terminal = computed(() => ['COMPLETED', 'REJECTED'].includes(response.value?.status ?? ''))
const validation = computed(() => {
  if (!idPattern.test(employeeId.value)) return '员工 ID 仅支持 1–64 位字母、数字、下划线和短横线。'
  const date = new Date(`${shiftDate.value}T00:00:00Z`)
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(shiftDate.value) ||
    !Number.isFinite(date.getTime()) ||
    date.toISOString().slice(0, 10) !== shiftDate.value
  )
    return '请选择有效的换班日期。'
  if (!reason.value.trim() || reason.value.length > 200) return '换班原因不能为空，最多 200 字符。'
  return ''
})
const canApprove = computed(
  () =>
    !busy.value &&
    idPattern.test(employeeId.value) &&
    idPattern.test(workflowId.value) &&
    !terminal.value &&
    (!response.value || waiting.value),
)
const labels: Record<string, string> = {
  SCHEDULE_QUERIED: '已查询排班',
  POLICY_CHECKED: '已检查制度',
  WAITING_EMPLOYEE_CONFIRMATION: '等待员工确认',
  EMPLOYEE_CONFIRMED: '员工已确认',
  SCHEDULE_UPDATED: '排班已更新',
  COMPLETED: '流程完成',
  REJECTED: '已拒绝',
}
const statusLabel = computed(() =>
  response.value ? (labels[response.value.status] ?? response.value.status) : '尚未创建',
)
let controller: AbortController | undefined
onBeforeUnmount(() => controller?.abort())
watch(employeeId, () => {
  workflowId.value = ''
  response.value = null
  submitted.value = ''
  error.value = detail.value = ''
})
watch(workflowId, () => {
  if (response.value && workflowId.value !== response.value.workflowId) {
    response.value = null
    submitted.value = ''
  }
  error.value = detail.value = ''
})
async function run(action: 'start' | 'approve' | 'reject') {
  if (busy.value || (action === 'start' ? !!validation.value || waiting.value : !canApprove.value))
    return
  pending.value = action
  error.value = detail.value = ''
  const active = new AbortController()
  controller = active
  const timeout = setTimeout(() => active.abort(), 120_000)
  try {
    const result =
      action === 'start'
        ? await startWorkflow(
            employeeId.value,
            {
              shiftDate: shiftDate.value,
              targetShift: targetShift.value,
              reason: reason.value.trim(),
            },
            active.signal,
          )
        : await approveWorkflow(
            employeeId.value,
            workflowId.value,
            action === 'approve',
            active.signal,
          )
    workflowId.value = result.workflowId
    response.value = result
    if (action === 'start') submitted.value = `${shiftDate.value} · ${reason.value.trim()}`
  } catch (caught) {
    error.value = active.signal.aborted
      ? '等待超过 120 秒。后端可能仍在执行，请核对结果后再操作。'
      : caught instanceof Error
        ? caught.message
        : '请求失败，请检查后端服务。'
    detail.value = caught instanceof WorkflowApiError ? caught.detail : ''
  } finally {
    clearTimeout(timeout)
    pending.value = null
  }
}
</script>

<template>
  <div class="tool-workspace">
    <article class="tool-doc">
      <div class="eyebrow">GRAPH <span class="stable-tag">可用接口</span></div>
      <h1>换班工作流</h1>
      <p class="lead">让业务按步骤执行，在关键节点等待你的确认。</p>
      <p class="intro-detail">
        提交换班信息后，流程查询排班、检查制度并准备申请，在员工确认前暂停。确认后继续执行，拒绝则结束流程。
      </p>
      <div class="memory-endpoints tool-endpoints">
        <div>
          <span class="method">POST</span><code>/api/workflows/shift-swaps</code
          ><small>创建流程</small>
        </div>
        <div>
          <span class="method">POST</span
          ><code>/api/workflows/shift-swaps/{workflowId}/approval</code><small>员工确认</small>
        </div>
      </div>
      <section class="doc-section">
        <h2>流程路径</h2>
        <ol class="graph-path">
          <li>查询排班 <small>query_schedule</small></li>
          <li>检查制度 <small>check_policy</small></li>
          <li>准备申请 <small>prepare_request</small></li>
          <li class="human-step">等待员工确认 <small>human_approval</small></li>
          <li>确认 → 更新排班 → 发送通知<br />拒绝 → 结束流程</li>
        </ol>
        <p class="section-description">
          这里展示流程定义，右侧状态来自接口响应；接口未提供逐节点实时执行记录。
        </p>
      </section>
      <div class="notice">
        <Layers :size="18" />
        <div>
          <strong>当前为内存中的演示流程</strong>
          <p>
            排班按日期生成演示数据，更新和通知节点仅返回演示状态，未接入真实排班或通知系统。后端重启会丢失流程。当前确认操作代表员工本人确认，未实现独立主管审批节点。
          </p>
        </div>
      </div>
    </article>
    <aside class="tool-console graph-console" aria-label="Graph 工作流调试">
      <div class="playground-heading">
        <span class="playground-title"><Layers :size="18" /> 工作流调试</span
        ><span class="subtle-pill">GRAPH</span>
      </div>
      <form class="tool-form" @submit.prevent="run('start')">
        <label class="tool-field"
          >员工 ID<input v-model.trim="employeeId" :disabled="busy" maxlength="64"
        /></label>
        <div class="graph-fields">
          <label class="tool-field"
            >换班日期<input v-model="shiftDate" type="date" :disabled="busy || waiting"
          /></label>
          <label class="tool-field"
            >目标班次<select v-model="targetShift" :disabled="busy || waiting">
              <option>白班</option>
              <option>晚班</option>
            </select></label
          >
        </div>
        <label class="tool-field"
          >换班原因<textarea
            v-model="reason"
            rows="3"
            maxlength="200"
            :disabled="busy || waiting"
          ></textarea>
        </label>
        <p class="section-description">{{ reason.length }} / 200</p>
        <p v-if="validation" class="validation-error">{{ validation }}</p>
        <button class="primary-button tool-send" :disabled="busy || waiting || !!validation">
          <LoaderCircle v-if="pending === 'start'" :size="15" class="spin" /><Play
            v-else
            :size="15"
          />{{ pending === 'start' ? '正在创建流程…' : '创建换班流程' }}
        </button>
        <p v-if="waiting" class="section-description">请先确认或拒绝当前申请，再创建下一条流程。</p>
      </form>
      <section class="tool-output" aria-live="polite" :aria-busy="busy">
        <div class="section-heading">
          <h2>流程状态</h2>
          <span class="subtle-pill">{{ statusLabel }}</span>
        </div>
        <template v-if="response">
          <p class="section-description">{{ response.status }}</p>
          <p v-if="submitted" class="graph-summary">{{ submitted }}</p>
          <div class="graph-shifts">
            <span
              >当前班次<strong>{{ response.currentShift || '未提供' }}</strong></span
            ><span>→</span
            ><span
              >目标班次<strong>{{ response.targetShift || '未提供' }}</strong></span
            >
          </div>
          <p class="tool-answer">{{ response.message || '未提供说明' }}</p>
          <p v-if="response.policy" class="graph-summary">制度：{{ response.policy }}</p>
          <p v-if="response.requestId" class="graph-summary">申请编号：{{ response.requestId }}</p>
          <details>
            <summary>查看原始 JSON</summary>
            <pre>{{ JSON.stringify(response, null, 2) }}</pre>
          </details>
        </template>
        <div v-else class="tool-empty">
          <Layers :size="25" /><span>创建流程后查看实际返回状态</span>
        </div>
        <p v-if="error" class="validation-error" role="alert">{{ error }}</p>
        <details v-if="detail">
          <summary>错误详情</summary>
          <pre>{{ detail }}</pre>
        </details>
      </section>
      <section class="swap-confirm">
        <div class="section-heading">
          <h2>员工确认</h2>
          <span class="subtle-pill">人工节点</span>
        </div>
        <label class="tool-field"
          >工作流 ID<input
            v-model.trim="workflowId"
            :disabled="busy"
            maxlength="64"
            placeholder="创建后自动填入，也可粘贴待确认流程 ID"
        /></label>
        <p>请核对班次与申请内容。使用创建流程时相同的员工 ID；手动填写 ID 不会自动加载申请详情。</p>
        <div class="graph-actions">
          <button class="primary-button" :disabled="!canApprove" @click="run('approve')">
            <LoaderCircle v-if="pending === 'approve'" :size="15" class="spin" /><CheckCircle2
              v-else
              :size="15"
            />确认并继续
          </button>
          <button
            class="primary-button graph-reject"
            :disabled="!canApprove"
            @click="run('reject')"
          >
            <LoaderCircle v-if="pending === 'reject'" :size="15" class="spin" /><X
              v-else
              :size="15"
            />拒绝申请
          </button>
        </div>
        <p v-if="terminal">该流程已结束，无需再次确认。</p>
      </section>
    </aside>
  </div>
</template>
<style scoped>
.graph-console {
  position: static;
  max-height: none;
}
.graph-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.tool-field select {
  display: block;
  width: 100%;
  margin-top: 7px;
  padding: 10px;
  background: white;
  border: 1px solid #dfe7e2;
  border-radius: 7px;
  color: #35463c;
}
.graph-path {
  padding-left: 25px;
}
.graph-path li {
  padding: 13px 10px;
  border-bottom: 1px solid var(--border);
  color: #35463c;
  line-height: 1.7;
}
.graph-path small {
  display: block;
  font-family: var(--mono);
  color: var(--muted);
}
.graph-path .human-step {
  background: #eef6f1;
  border-radius: 8px;
  color: var(--green);
}
.graph-shifts {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 10px;
  padding: 18px;
  margin-bottom: 14px;
  background: #f5f8f6;
  border-radius: 9px;
  font-size: 0.8rem;
}
.graph-shifts strong {
  display: block;
  margin-top: 8px;
  font-size: 1rem;
}
.graph-summary {
  font-size: 0.8rem;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.graph-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.graph-reject {
  background: white;
  color: #a54438;
  border: 1px solid #e2c5c0;
}
@media (max-width: 480px) {
  .graph-fields {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
