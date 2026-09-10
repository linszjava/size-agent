<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  CalendarClock,
  CheckCircle2,
  Clock3,
  CloudSun,
  LoaderCircle,
  Send,
  ShieldCheck,
  Wrench,
} from '@lucide/vue'
import { callToolChat, confirmShiftSwap } from '../lib/toolApi'
import type { SwapResult } from '../lib/toolApi'

const examples = [
  { icon: CloudSun, label: '演示天气', text: '杭州今天天气怎么样？' },
  { icon: Clock3, label: '当前时间', text: '现在上海时间是几点？' },
  { icon: CalendarClock, label: '本人排班', text: '查询我 2026-09-12 的排班。' },
  { icon: ShieldCheck, label: '企业制度', text: '公司的换班制度是什么？' },
]
const employeeId = ref('employee-001')
const message = ref('查询我 2026-09-12 的排班。')
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
  if (!message.value.trim()) return '请输入问题。'
  if (message.value.length > 1000) return '消息最多 1,000 个字符。'
  return ''
})

function useExample(text: string) {
  message.value = text
}
async function send() {
  if (validation.value || loading.value) return
  loading.value = true
  error.value = ''
  answer.value = ''
  raw.value = ''
  result.value = null
  try {
    const response = await callToolChat(employeeId.value, message.value.trim())
    answer.value = response.content
    raw.value = JSON.stringify(response, null, 2)
    const detected = response.content.match(/\b[a-fA-F0-9]{32}\b/)?.[0]
    if (detected) token.value = detected
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : String(caught)
    raw.value =
      caught && typeof caught === 'object' && 'detail' in caught ? String(caught.detail) : ''
  } finally {
    loading.value = false
  }
}
async function confirm() {
  if (!idPattern.test(employeeId.value) || !token.value.trim() || confirming.value) return
  confirming.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await confirmShiftSwap(employeeId.value, token.value.trim())
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : String(caught)
  } finally {
    confirming.value = false
  }
}
</script>

<template>
  <div class="tool-workspace">
    <article class="tool-doc">
      <div class="eyebrow">TOOLS <span class="stable-tag">可用接口</span></div>
      <h1>Tool Calling</h1>
      <p class="lead">让大模型通过 Java 方法安全地完成业务操作。</p>
      <p class="intro-detail">
        模型负责理解自然语言并选择工具，后端工具负责读取演示数据或执行受控操作。员工身份只从
        <code>X-Employee-Id</code> 请求头传入，不由模型生成。
      </p>
      <div class="memory-endpoints tool-endpoints">
        <div>
          <span class="method">POST</span><code>/api/tools/chat</code><small>工具对话</small>
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
        <div class="section-heading">
          <h2>已注册工具</h2>
          <span class="subtle-pill">4 TOOLS</span>
        </div>
        <div class="tool-cards">
          <div>
            <CloudSun :size="18" /><strong>天气查询</strong>
            <p>杭州、上海、北京的演示天气</p>
          </div>
          <div>
            <Clock3 :size="18" /><strong>时间查询</strong>
            <p>根据 ZoneId 查询当前时间</p>
          </div>
          <div>
            <CalendarClock :size="18" /><strong>员工助手</strong>
            <p>排班、制度和换班准备</p>
          </div>
          <div>
            <CheckCircle2 :size="18" /><strong>换班确认</strong>
            <p>校验员工身份和一次性令牌</p>
          </div>
        </div>
      </section>
    </article>

    <aside class="tool-console">
      <div class="playground-heading">
        <span class="playground-title"><Wrench :size="18" /> 工具调试</span
        ><span class="subtle-pill">TOOL CALLING</span>
      </div>
      <div class="tool-form">
        <label class="tool-field"
          >员工 ID<input v-model.trim="employeeId" :disabled="loading || confirming" maxlength="64"
        /></label>
        <div class="tool-examples">
          <span>快速示例</span>
          <button
            v-for="item in examples"
            :key="item.label"
            :disabled="loading"
            @click="useExample(item.text)"
          >
            <component :is="item.icon" :size="14" />{{ item.label }}
          </button>
        </div>
        <label class="tool-field"
          >自然语言问题<textarea
            v-model="message"
            :disabled="loading"
            rows="4"
            maxlength="1000"
            @keydown.meta.enter.prevent="send"
            @keydown.ctrl.enter.prevent="send"
          ></textarea>
        </label>
        <p v-if="validation" class="validation-error">{{ validation }}</p>
        <button class="primary-button tool-send" :disabled="!!validation || loading" @click="send">
          <LoaderCircle v-if="loading" class="spin" :size="15" /><Send v-else :size="15" />
          {{ loading ? '模型正在调用工具…' : '发送工具请求' }}
        </button>
      </div>
      <div class="tool-output">
        <div class="section-heading">
          <h2>模型回答</h2>
          <span v-if="answer" class="response-code">200 OK</span>
        </div>
        <div v-if="answer" class="tool-answer">{{ answer }}</div>
        <div v-else class="tool-empty">
          <Wrench :size="25" /><span>选择示例或输入问题开始测试</span>
        </div>
        <p v-if="error" class="validation-error">{{ error }}</p>
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
            :disabled="confirming"
            placeholder="confirmationToken"
          /><button class="primary-button" :disabled="!token || confirming" @click="confirm">
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
