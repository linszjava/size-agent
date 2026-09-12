<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { CalendarClock, CheckCircle2, LoaderCircle, Send, ShieldCheck, Wrench } from '@lucide/vue'
import { confirmShiftSwap } from '../lib/toolApi'
import { callEmployeeAgent } from '../lib/agentApi'
import type { SwapResult } from '../lib/toolApi'

const examples = [
  { icon: CalendarClock, label: '本人排班', text: '查询我今天的排班。' },
  { icon: ShieldCheck, label: '企业制度', text: '公司的换班制度是什么？' },
]
const employeeId = ref('employee-001')
const conversationId = ref('agent-' + crypto.randomUUID())
const history = ref<{ question: string; answer: string }[]>([])
const busy = computed(() => loading.value || confirming.value)
let controller: AbortController | undefined
let timeout: ReturnType<typeof setTimeout> | undefined
function beginRequest() {
  controller = new AbortController()
  timeout = setTimeout(() => controller?.abort(), 120_000)
  return controller.signal
}
onBeforeUnmount(() => {
  clearTimeout(timeout)
  controller?.abort()
})
function newConversation() {
  conversationId.value = 'agent-' + crypto.randomUUID()
}
watch([employeeId, conversationId], () => {
  history.value = []
  answer.value = token.value = raw.value = error.value = ''
  result.value = null
})
const message = ref('查询我今天的排班。')
const answer = ref('')
const token = ref('')
const raw = ref('')
const error = ref('')
const loading = ref(false)
const confirming = ref(false)
const result = ref<SwapResult | null>(null)
const idPattern = /^[A-Za-z0-9_-]{1,64}$/
const validation = computed(() => {
  if (!idPattern.test(employeeId.value))
    return '员工 ID 仅支持字母、数字、下划线和短横线，长度 1–64。'
  if (!idPattern.test(conversationId.value))
    return '会话 ID 仅支持字母、数字、下划线和短横线，长度 1–64。'
  if (!message.value.trim()) return '请输入问题。'
  if (message.value.length > 1000) return '消息最多 1,000 个字符。'
  return ''
})

function useExample(text: string) {
  message.value = text
}
async function send() {
  if (validation.value || busy.value) return
  loading.value = true
  error.value = ''
  answer.value = ''
  raw.value = ''
  result.value = null
  try {
    token.value = ''
    const response = await callEmployeeAgent(
      employeeId.value,
      conversationId.value,
      message.value.trim(),
      beginRequest(),
    )
    history.value.push({ question: message.value.trim(), answer: response.content })
    answer.value = response.content
    raw.value = JSON.stringify(response, null, 2)
    const detected = response.content.match(/\b[a-fA-F0-9]{32}\b/)?.[0]
    if (detected) token.value = detected
  } catch (caught) {
    error.value = controller?.signal.aborted
      ? '已停止等待，请检查后端结果后再重试。'
      : caught instanceof Error
        ? caught.message
        : String(caught)
    raw.value =
      caught && typeof caught === 'object' && 'detail' in caught ? String(caught.detail) : ''
  } finally {
    clearTimeout(timeout)
    loading.value = false
  }
}
async function confirm() {
  if (!idPattern.test(employeeId.value) || !token.value.trim() || busy.value) return
  confirming.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await confirmShiftSwap(employeeId.value, token.value.trim(), beginRequest())
    token.value = ''
  } catch (caught) {
    error.value = controller?.signal.aborted
      ? '已停止等待，请检查后端结果后再重试。'
      : caught instanceof Error
        ? caught.message
        : String(caught)
  } finally {
    clearTimeout(timeout)
    confirming.value = false
  }
}
</script>

<template>
  <div class="tool-workspace">
    <article class="tool-doc">
      <div class="eyebrow">AGENT <span class="stable-tag">可用接口</span></div>
      <h1>员工 Agent</h1>
      <p class="lead">结合会话上下文，查询排班、理解制度并准备换班。</p>
      <p class="intro-detail">
        模型负责理解自然语言并选择工具，后端工具负责读取演示数据或执行受控操作。员工身份只从
        <code>X-Employee-Id</code> 请求头传入，不由模型生成。
      </p>
      <div class="memory-endpoints tool-endpoints">
        <div>
          <span class="method">POST</span><code>/api/agents/employee/chat</code
          ><small>Agent 对话</small>
        </div>
        <div>
          <span class="method">POST</span><code>/api/tools/shift-swaps/confirm</code
          ><small>确认换班</small>
        </div>
      </div>
      <div class="notice">
        <ShieldCheck :size="18" />
        <div>
          <strong>换班采用二次确认</strong>
          <p>对话接口只生成待确认申请和令牌；将令牌提交到确认接口后，才会正式生成换班申请编号。</p>
        </div>
      </div>
      <section class="doc-section">
        <h2>会话与请求说明</h2>
        <div class="parameter-list">
          <div class="parameter-row">
            <code>conversationId</code>
            <p>必填，1–64 位字母、数字、下划线或短横线。同一员工和会话 ID 延续上下文。</p>
          </div>
          <div class="parameter-row">
            <code>message</code>
            <p>必填，最多 1,000 字符。Agent 可连续选择员工工具完成任务，返回最终回答。</p>
          </div>
          <div class="parameter-row">
            <code>conversationId / content</code>
            <p>响应返回会话 ID 和回答正文。当前接口不提供流式输出或工具执行过程。</p>
          </div>
        </div>
        <div class="notice">
          <ShieldCheck :size="18" />
          <div>
            <strong>会话保存在后端内存</strong>
            <p>
              重启后端会丢失上下文。新建会话使用新
              ID，不会删除旧会话。此处仅显示本次打开页面收到的对话，刷新不会加载历史记录。
            </p>
          </div>
        </div>
      </section>
    </article>

    <aside class="tool-console">
      <div class="playground-heading">
        <span class="playground-title"><Wrench :size="18" /> Agent 调试</span
        ><span class="subtle-pill">REACT AGENT</span>
      </div>
      <div class="tool-form">
        <label class="tool-field"
          >员工 ID<input v-model.trim="employeeId" :disabled="busy" maxlength="64"
        /></label>
        <label class="tool-field"
          >会话 ID<input v-model.trim="conversationId" :disabled="busy" maxlength="64"
        /></label>
        <button class="text-button" :disabled="busy" @click="newConversation">新建会话</button>
        <div class="tool-examples">
          <span>快速示例</span>
          <button
            v-for="item in examples"
            :key="item.label"
            :disabled="busy"
            @click="useExample(item.text)"
          >
            <component :is="item.icon" :size="14" />{{ item.label }}
          </button>
        </div>
        <label class="tool-field"
          >自然语言问题<textarea
            v-model="message"
            :disabled="busy"
            rows="4"
            maxlength="1000"
            @keydown.meta.enter.prevent.stop="send"
            @keydown.ctrl.enter.prevent.stop="send"
          ></textarea>
        </label>
        <p v-if="validation" class="validation-error">{{ validation }}</p>
        <button class="primary-button tool-send" :disabled="!!validation || busy" @click="send">
          <LoaderCircle v-if="loading" class="spin" :size="15" /><Send v-else :size="15" />
          {{ loading ? 'Agent 正在处理…' : '发送消息' }}
        </button>
      </div>
      <div class="tool-output" aria-live="polite" :aria-busy="loading">
        <div class="section-heading">
          <h2>会话记录</h2>
          <span v-if="answer" class="response-code">200 OK</span>
        </div>
        <div v-if="history.length" class="agent-history">
          <div v-for="(turn, index) in history" :key="index" class="agent-turn">
            <p class="agent-question">{{ turn.question }}</p>
            <div class="tool-answer">{{ turn.answer }}</div>
          </div>
        </div>
        <div v-else class="tool-empty">
          <Wrench :size="25" /><span>选择示例或输入问题开始测试</span>
        </div>
        <p v-if="error" class="validation-error" role="alert">{{ error }}</p>
        <details v-if="raw">
          <summary>查看原始 JSON</summary>
          <pre>{{ raw }}</pre>
        </details>
      </div>
      <div class="swap-confirm">
        <div class="section-heading">
          <h2>确认换班</h2>
          <span class="subtle-pill">二次确认</span>
        </div>
        <p>令牌会从模型回答中自动识别，也可以手动粘贴。</p>
        <div class="token-row">
          <input
            v-model.trim="token"
            :disabled="busy"
            aria-label="换班确认令牌"
            placeholder="confirmationToken"
          /><button
            class="primary-button"
            :disabled="!idPattern.test(employeeId) || !token.trim() || busy"
            @click="confirm"
          >
            <LoaderCircle v-if="confirming" class="spin" :size="14" /><CheckCircle2
              v-else
              :size="14"
            />确认提交
          </button>
        </div>
        <div v-if="result" class="swap-result">
          <strong>{{ result.status }}</strong
          ><span>申请编号：{{ result.requestId }}</span
          ><span>{{ result.shiftDate }} · {{ result.targetShift }}</span>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.tool-console {
  position: static;
  max-height: none;
}
.agent-history {
  display: grid;
  gap: 18px;
  max-height: 520px;
  overflow: auto;
}
.agent-question {
  margin: 0 0 10px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: #f5f7f6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 0.84rem;
}
.agent-turn {
  min-width: 0;
}
</style>
