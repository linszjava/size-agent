<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { Layers, LoaderCircle, Send, Square } from '@lucide/vue'
import { callDistributed, DistributedApiError, extractAnswer } from '../lib/distributedApi'
import type { DistributedMode } from '../lib/distributedApi'
const props = defineProps<{ mode: DistributedMode }>()
const isMcp = computed(() => props.mode === 'mcp')
const title = computed(() => (isMcp.value ? 'MCP 远程工具' : 'A2A 人事专家'))
const examples = computed(() =>
  isMcp.value
    ? ['查询员工 E1001 今天的排班，必须调用工具', '查询员工 E1002 明天的排班，必须调用工具']
    : ['公司年假制度是什么？', '未休完的年假最多可以结转几天？', '公司的报销标准是什么？'],
)
const message = ref(examples.value[0]!)
const answer = ref('')
const raw = ref('')
const error = ref('')
const state = ref('等待请求')
const loading = ref(false)
const validation = computed(() => (message.value.trim() ? '' : '请输入问题。'))
let controller: AbortController | undefined
let timeout: ReturnType<typeof setTimeout> | undefined
onBeforeUnmount(() => {
  clearTimeout(timeout)
  controller?.abort()
})
async function send() {
  if (loading.value || validation.value) return
  loading.value = true
  answer.value = raw.value = error.value = ''
  state.value = '正在等待远程服务…'
  const active = new AbortController()
  controller = active
  let timedOut = false
  timeout = setTimeout(() => {
    timedOut = true
    active.abort()
  }, 120_000)
  try {
    const result = await callDistributed(props.mode, message.value.trim(), active.signal)
    raw.value = JSON.stringify(result, null, 2)
    answer.value = extractAnswer(result)
    state.value = '请求完成'
  } catch (caught) {
    state.value = active.signal.aborted ? '已停止等待' : '请求失败'
    error.value = active.signal.aborted
      ? timedOut
        ? '请求超过 120 秒，请检查后端与 Nacos 日志。'
        : '已停止等待，远程服务可能仍在执行。'
      : caught instanceof DistributedApiError
        ? caught.message
        : '无法连接服务，请检查对应客户端及 Vite 代理配置。'
    raw.value = caught instanceof DistributedApiError ? caught.detail : ''
  } finally {
    clearTimeout(timeout)
    loading.value = false
  }
}
</script>
<template>
  <div class="tool-workspace">
    <article class="tool-doc">
      <div class="eyebrow">MCP · NACOS · A2A <span class="stable-tag">跨服务调用</span></div>
      <h1>{{ title }}</h1>
      <p class="lead">
        {{ isMcp ? '让模型调用另一个服务中的员工排班工具。' : '将人事问题交给远程专家 Agent。' }}
      </p>
      <p class="intro-detail">
        {{
          isMcp
            ? 'ChatClient 根据问题选择工具，通过 Nacos 发现 MCP 服务，获取排班数据后组织回答。'
            : '客户端通过 Nacos 发现 hr-specialist，经 A2A 协议调用远程 ReactAgent，并返回消息状态。'
        }}
      </p>
      <div class="memory-endpoints tool-endpoints">
        <div>
          <span class="method">{{ isMcp ? 'POST' : 'GET' }}</span
          ><code>{{ isMcp ? '/api/mcp/chat' : '/api/a2a/hr' }}</code>
        </div>
      </div>
      <section class="doc-section">
        <h2>调用路径</h2>
        <ol class="service-path">
          <li>前端调试页面</li>
          <li>{{ isMcp ? 'employee-mcp-client · 9002' : 'employee-a2a-client · 9102' }}</li>
          <li>Nacos 服务发现</li>
          <li>{{ isMcp ? 'employee-mcp-server · 9001' : 'hr-a2a-server · 9101' }}</li>
          <li>返回结果</li>
        </ol>
        <p class="section-description">此处展示调用架构，不代表实时服务健康状态或执行轨迹。</p>
      </section>
      <section class="doc-section">
        <h2>请求与响应</h2>
        <p class="intro-detail">
          {{
            isMcp
              ? 'JSON 请求体包含 message，响应为 content。员工编号写在演示问题中，此接口不使用 X-Employee-Id 请求头。'
              : '问题通过 question 查询参数发送。后端返回 Agent 的 messages 状态，页面提取可识别的助手文本，并保留完整 JSON。当前接口没有会话 ID 参数。'
          }}
        </p>
      </section>
      <div class="notice">
        <Layers :size="18" />
        <div>
          <strong>启动所需服务</strong>
          <p>
            先启动 Nacos 和对应服务端，再启动客户端。{{
              isMcp
                ? '排班为演示数据，模型传入的员工编号仅用于学习。'
                : '本模块演示规则为每年 10 天年假、最多结转 5 天，与第一章的测试 PDF 独立。'
            }}
          </p>
        </div>
      </div>
    </article>
    <aside class="tool-console distributed-console">
      <div class="playground-heading">
        <span class="playground-title"><Layers :size="18" /> {{ title }}</span
        ><span class="subtle-pill">{{ isMcp ? '9002' : '9102' }}</span>
      </div>
      <form class="tool-form" @submit.prevent="send">
        <div class="tool-examples">
          <span>快速示例</span
          ><button
            v-for="example in examples"
            :key="example"
            type="button"
            :disabled="loading"
            @click="message = example"
          >
            {{ example }}
          </button>
        </div>
        <label class="tool-field"
          >{{ isMcp ? '工具调用问题' : '人事问题'
          }}<textarea
            v-model="message"
            rows="5"
            :disabled="loading"
            @keydown.meta.enter.prevent.stop="send"
            @keydown.ctrl.enter.prevent.stop="send"
          ></textarea>
        </label>
        <p v-if="validation" class="validation-error">{{ validation }}</p>
        <button
          v-if="loading"
          type="button"
          class="primary-button tool-send"
          @click="controller?.abort()"
        >
          <Square :size="15" />停止等待
        </button>
        <button v-else class="primary-button tool-send" :disabled="!!validation">
          <Send :size="15" />发送请求
        </button>
      </form>
      <section class="tool-output" aria-live="polite" :aria-busy="loading">
        <div class="section-heading">
          <h2>回答</h2>
          <span class="subtle-pill"
            ><LoaderCircle v-if="loading" :size="13" class="spin" />{{ state }}</span
          >
        </div>
        <p v-if="error" role="alert" class="validation-error">{{ error }}</p>
        <div v-if="answer" class="tool-answer">{{ answer }}</div>
        <div v-else-if="!error" class="tool-empty">
          <Layers :size="25" /><span>{{
            loading
              ? '正在等待服务返回…'
              : raw
                ? '响应中未识别到回答文本，请查看原始 JSON。'
                : '选择示例或输入问题开始测试'
          }}</span>
        </div>
        <details v-if="raw">
          <summary>查看原始响应</summary>
          <pre>{{ raw }}</pre>
        </details>
      </section>
    </aside>
  </div>
</template>
<style scoped>
.distributed-console {
  position: static;
  max-height: none;
  min-height: 0;
  align-self: start;
}
.service-path {
  padding-left: 24px;
}
.service-path li {
  padding: 12px;
  border-bottom: 1px solid var(--border);
  font-size: 0.87rem;
  color: #35463c;
}
.tool-examples button {
  text-align: left;
  line-height: 1.6;
}
</style>
