<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  Check,
  CheckCheck,
  ChevronDown,
  ChevronRight,
  CircleHelp,
  Code2,
  Copy,
  FileJson,
  Layers,
  LoaderCircle,
  Menu,
  MessageSquare,
  Play,
  Radio,
  RotateCcw,
  Search,
  Settings2,
  Square,
  Terminal,
  Wrench,
  X,
  Zap,
} from '@lucide/vue'
import CodeBlock from './components/CodeBlock.vue'
import MemoryChatPage from './components/MemoryChatPage.vue'
import DistributedPage from './components/DistributedPage.vue'
import ShiftWorkflowPage from './components/ShiftWorkflowPage.vue'
import EmployeeAgentPage from './components/EmployeeAgentPage.vue'
import ToolChatPage from './components/ToolChatPage.vue'
import KnowledgeBasePage from './components/KnowledgeBasePage.vue'
import { endpoints, parameters, responseFields } from './data/endpoints'
import { ApiError, buildCurl, buildPayload, callChat } from './lib/api'
import type { ChatResponse, Draft } from './lib/api'
import { registerApiNavigation } from './lib/webmcp'
import type { ToolContext } from './lib/webmcp'

type RequestState = 'idle' | 'loading' | 'success' | 'error' | 'cancelled'
const resolveRoute = () => {
  const value = window.location.hash.slice(1)
  return value === 'quickstart' || endpoints.some((item) => item.id === value)
    ? value
    : 'chat-model'
}
const route = ref(resolveRoute())
const current = computed(() => endpoints.find((item) => item.id === route.value) || endpoints[1]!)
const isGuide = computed(() => route.value === 'quickstart')
const query = ref('')
const mobileMenu = ref(false)
const searchInput = ref<HTMLInputElement>()
const filtered = computed(() =>
  endpoints.filter((item) =>
    `${item.title} ${item.label} ${item.path}`.toLowerCase().includes(query.value.toLowerCase()),
  ),
)
const iconFor = (id: string) =>
  ({
    chat: MessageSquare,
    'chat-model': Layers,
    'chat-client': Code2,
    messages: MessageSquare,
    stream: Radio,
    memory: MessageSquare,
    tools: Wrench,
    agent: MessageSquare,
    graph: Layers,
    knowledge: BookOpen,
  })[id] || Code2
const drafts = reactive<Record<string, Draft>>(
  Object.fromEntries(
    endpoints.map((item) => [
      item.id,
      {
        message: item.example,
        systemPrompt: '',
        temperature: 0.7,
        topP: 0.8,
        maxTokens: 1000,
        stopSequences: '',
      },
    ]),
  ),
)
const draft = computed(() => drafts[current.value.id]!)
const payload = computed(() => {
  try {
    return buildPayload(draft.value, current.value.basic)
  } catch {
    return null
  }
})
const validation = computed(() => {
  try {
    buildPayload(draft.value, current.value.basic)
    return ''
  } catch (error) {
    return (error as Error).message
  }
})
const codeTab = ref<'json' | 'curl' | 'java'>('json')
const code = computed(() =>
  codeTab.value === 'java'
    ? current.value.java
    : !payload.value
      ? '// 请先修正表单参数'
      : codeTab.value === 'curl'
        ? buildCurl(current.value.path, payload.value, current.value.stream)
        : JSON.stringify(payload.value, null, 2),
)
const visibleParams = computed(() => (current.value.basic ? parameters.slice(0, 1) : parameters))
const advanced = ref(false)
const status = ref<RequestState>('idle')
const response = ref<ChatResponse | null>(null)
const content = ref('')
const raw = ref('')
const error = ref('')
const httpStatus = ref<number | null>(null)
const elapsed = ref(0)
const chunks = ref(0)
const resultTab = ref<'answer' | 'raw'>('answer')
const responsePane = ref<HTMLElement>()
const followOutput = ref(true)
const copyNotice = ref('')
const isLoading = computed(() => status.value === 'loading')
const stateLabel = computed(
  () =>
    ({
      idle: '等待请求',
      loading: current.value.stream ? '正在接收' : '模型思考中',
      success: '请求完成',
      error: '请求失败',
      cancelled: '已停止',
    })[status.value],
)
let controller: AbortController | null = null
let timer: ReturnType<typeof setInterval> | undefined
let timeout: ReturnType<typeof setTimeout> | undefined
let noticeTimer: ReturnType<typeof setTimeout> | undefined
let requestId = 0
let cleanupTools: (() => void) | undefined
function clearTimers() {
  clearInterval(timer)
  clearTimeout(timeout)
}
function stopRequest() {
  controller?.abort()
  clearTimers()
  if (isLoading.value) status.value = 'cancelled'
}
function clearResult() {
  requestId++
  stopRequest()
  status.value = 'idle'
  response.value = null
  content.value = ''
  raw.value = ''
  error.value = ''
  httpStatus.value = null
  elapsed.value = 0
  chunks.value = 0
}
function scrollSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  mobileMenu.value = false
}
function navigate(id: string) {
  window.location.hash = id
  route.value = id
  mobileMenu.value = false
}
function resetDraft() {
  Object.assign(draft.value, {
    message: current.value.example,
    systemPrompt: '',
    temperature: 0.7,
    topP: 0.8,
    maxTokens: 1000,
    stopSequences: '',
  })
  clearResult()
}
async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    copyNotice.value = '已复制到剪贴板'
  } catch {
    copyNotice.value = '复制失败，请选中文本手动复制'
  }
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => {
    copyNotice.value = ''
  }, 2400)
}
async function sendRequest() {
  if (isLoading.value || !payload.value) return
  clearResult()
  const id = ++requestId
  const endpoint = current.value
  const body = payload.value!
  const activeController = new AbortController()
  controller = activeController
  status.value = 'loading'
  resultTab.value = 'answer'
  followOutput.value = true
  const start = performance.now()
  let timedOut = false
  timer = setInterval(() => {
    elapsed.value = Math.round(performance.now() - start)
  }, 100)
  timeout = setTimeout(() => {
    timedOut = true
    activeController.abort()
  }, 120_000)
  try {
    const result = await callChat({
      path: endpoint.path,
      payload: body,
      stream: !!endpoint.stream,
      signal: activeController.signal,
      onHeaders: (value) => {
        if (id === requestId) httpStatus.value = value
      },
      onChunk: (text) => {
        if (id !== requestId || activeController.signal.aborted) return
        content.value += text
        chunks.value++
        raw.value = content.value
      },
    })
    if (id !== requestId) return
    if (activeController.signal.aborted) {
      status.value = 'cancelled'
      return
    }
    response.value = result
    if (result) {
      content.value = result.content
      raw.value = JSON.stringify(result, null, 2)
    }
    status.value = 'success'
  } catch (caught) {
    if (id !== requestId) return
    if (activeController.signal.aborted && !timedOut) {
      status.value = 'cancelled'
      return
    }
    status.value = 'error'
    error.value = timedOut
      ? '请求超过 120 秒，已停止等待。请检查后端日志后重试。'
      : caught instanceof ApiError
        ? caught.message
        : '无法完成请求，请检查后端是否已启动，以及代理地址是否正确。'
    raw.value = caught instanceof ApiError ? caught.detail : String(caught)
  } finally {
    if (id === requestId) {
      elapsed.value = Math.round(performance.now() - start)
      clearTimers()
      controller = null
    }
  }
}
watch(route, () => {
  clearResult()
  advanced.value = false
  document.title = `${isGuide.value ? '快速开始' : current.value.title} · Size Agent Docs`
  window.scrollTo({ top: 0 })
})
watch(content, async () => {
  if (followOutput.value) {
    await nextTick()
    responsePane.value?.scrollTo({ top: responsePane.value.scrollHeight })
  }
})
function syncRoute() {
  route.value = resolveRoute()
}
function onKey(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    mobileMenu.value = true
    nextTick(() => searchInput.value?.focus())
  }
  if (
    (event.metaKey || event.ctrlKey) &&
    event.key === 'Enter' &&
    !isGuide.value &&
    !current.value.memory &&
    !current.value.tools &&
    !current.value.knowledge &&
    !current.value.agent &&
    !current.value.graph &&
    !current.value.distributed
  ) {
    event.preventDefault()
    void sendRequest()
  }
  if (event.key === 'Escape') mobileMenu.value = false
}
function onResponseScroll() {
  const pane = responsePane.value
  if (pane) followOutput.value = pane.scrollHeight - pane.scrollTop - pane.clientHeight < 50
}
onMounted(() => {
  document.title = `${isGuide.value ? '快速开始' : current.value.title} · Size Agent Docs`
  cleanupTools = registerApiNavigation(
    (document as Document & { modelContext?: ToolContext }).modelContext,
    async (id) => {
      navigate(id)
      await nextTick()
    },
  )
  window.addEventListener('hashchange', syncRoute)
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  cleanupTools?.()
  requestId++
  stopRequest()
  clearTimeout(noticeTimer)
  window.removeEventListener('hashchange', syncRoute)
  window.removeEventListener('keydown', onKey)
})
</script>

<template>
  <a class="skip-link" href="#main-content">跳到主要内容</a>
  <header class="topbar">
    <button
      class="icon-button mobile-toggle"
      aria-label="打开导航"
      @click="mobileMenu = !mobileMenu"
    >
      <Menu :size="21" />
    </button>
    <a class="brand" href="#chat-model" @click.prevent="navigate('chat-model')"
      ><span class="brand-mark"><Zap :size="21" fill="currentColor" /></span
      ><span>size<span class="brand-light">agent</span></span
      ><span class="docs-label">docs</span></a
    >
    <nav class="top-links" aria-label="主导航">
      <button :class="{ active: !isGuide }" @click="navigate('chat-model')">接口文档</button
      ><button :class="{ active: isGuide }" @click="navigate('quickstart')">
        快速开始 <ArrowUpRight :size="13" />
      </button>
    </nav>
    <div class="top-right">
      <span class="version">v0.1.0</span><span class="top-divider"></span
      ><span class="local-label"><span class="status-dot neutral"></span>本地开发环境</span
      ><a
        class="icon-button source-link"
        href="https://github.com/alibaba/spring-ai-alibaba"
        target="_blank"
        rel="noreferrer"
        aria-label="Spring AI Alibaba 源码"
        ><Code2 :size="20"
      /></a>
    </div>
  </header>

  <button
    v-if="mobileMenu"
    class="sidebar-overlay"
    aria-label="关闭导航"
    @click="mobileMenu = false"
  ></button>
  <aside class="sidebar" :class="{ 'is-open': mobileMenu }">
    <label class="search-box"
      ><Search :size="16" /><input
        ref="searchInput"
        v-model="query"
        placeholder="搜索接口…"
        aria-label="搜索接口"
      /><kbd>⌘ K</kbd></label
    >
    <div class="nav-group">
      <div class="nav-heading">开始使用</div>
      <button class="nav-item" :class="{ selected: isGuide }" @click="navigate('quickstart')">
        <BookOpen :size="17" /><span>快速开始</span>
      </button>
    </div>
    <div class="nav-group">
      <div class="nav-heading">模型接口</div>
      <nav aria-label="接口列表">
        <button
          v-for="item in filtered"
          :key="item.id"
          class="nav-item"
          :class="{ selected: route === item.id }"
          :aria-current="route === item.id ? 'page' : undefined"
          @click="navigate(item.id)"
        >
          <component :is="iconFor(item.id)" :size="17" /><span>{{ item.label }}</span
          ><span v-if="item.stream" class="tiny-live"></span>
        </button>
        <p v-if="!filtered.length" class="search-empty">没有匹配的接口</p>
      </nav>
    </div>
    <div class="nav-group sidebar-toc" v-if="!isGuide">
      <div class="nav-heading">本页内容</div>
      <a href="#" @click.prevent="scrollSection('overview')">概述</a
      ><a href="#" @click.prevent="scrollSection('parameters')">请求参数</a
      ><a href="#" @click.prevent="scrollSection('response-schema')">响应说明</a>
    </div>
    <div class="sidebar-bottom">
      <div class="powered-icon"><Layers :size="18" /></div>
      <div><strong>Spring AI Alibaba</strong><span>从一次调用，探索 AI 应用</span></div>
      <a
        href="https://github.com/alibaba/spring-ai-alibaba"
        target="_blank"
        rel="noreferrer"
        aria-label="查看 Spring AI Alibaba"
        ><ArrowUpRight :size="16"
      /></a>
    </div>
  </aside>

  <main id="main-content" class="main" tabindex="-1">
    <div class="breadcrumb">
      <span>开发文档</span><ChevronRight :size="13" /><span>{{
        isGuide ? '开始使用' : '模型接口'
      }}</span
      ><ChevronRight :size="13" /><strong>{{ isGuide ? '快速开始' : current.title }}</strong>
    </div>
    <template v-if="isGuide">
      <div class="guide-layout">
        <article class="guide-document">
          <span class="eyebrow">GETTING STARTED</span>
          <h1>让第一条请求，<br />成为你的起点<span class="green">。</span></h1>
          <p class="lead">连接本地服务，在文档中探索 Spring AI Alibaba。</p>
          <div class="guide-note">
            <Zap :size="19" /><span>模型对话 · 对话记忆 · 工具调用 · 知识库问答</span>
          </div>
          <section class="guide-step">
            <span class="step-number">01</span>
            <div>
              <h2>启动后端服务</h2>
              <p>
                在 IDEA 中运行 <code>AliyunApplication</code>。确认模型 API Key
                已在后端配置，服务监听 <code>8888</code> 端口。
              </p>
              <div class="inline-code">http://localhost:8888</div>
            </div>
          </section>
          <section class="guide-step">
            <span class="step-number">02</span>
            <div>
              <h2>选择接口，发送请求</h2>
              <p>
                从左侧选择接口，输入问题并点击「发送请求」。基础对话只需要一条消息；核心接口还可以调整系统提示词与生成参数。
              </p>
              <button class="primary-button" @click="navigate('chat')">
                尝试基础对话 <ArrowRight :size="16" />
              </button>
            </div>
          </section>
          <section class="guide-step">
            <span class="step-number">03</span>
            <div>
              <h2>阅读响应，理解调用过程</h2>
              <p>
                在响应面板阅读回答、查看原始 JSON 和 Token
                用量。使用流式接口时，回答将逐段出现；可以点击停止，保留已收到的内容。
              </p>
            </div>
          </section>
          <div class="notice">
            <CircleHelp :size="18" />
            <div>
              <strong>遇到错误？先看响应，再看后端日志</strong>
              <p>
                400 通常是参数校验失败；500 / 502
                需要结合后端异常判断。模型返回的详细报错是否可见，取决于后端异常处理。前端不会要求填写模型密钥。
              </p>
            </div>
          </div>
        </article>
        <aside class="guide-catalog">
          <div class="section-heading">
            <Layers :size="17" />
            <h2>探索模型接口</h2>
          </div>
          <button
            v-for="item in endpoints"
            :key="item.id"
            class="guide-card"
            @click="navigate(item.id)"
          >
            <span class="guide-card-icon"><component :is="iconFor(item.id)" :size="20" /></span
            ><strong>{{ item.title }}</strong>
            <p>{{ item.description }}</p>
            <span class="guide-path">{{ item.path }} <ArrowUpRight :size="15" /></span>
          </button>
        </aside>
      </div>
    </template>

    <MemoryChatPage v-else-if="current.memory" />

    <ToolChatPage v-else-if="current.tools" />

    <EmployeeAgentPage v-else-if="current.agent" />

    <ShiftWorkflowPage v-else-if="current.graph" />

    <DistributedPage
      v-else-if="current.distributed"
      :key="current.id"
      :mode="current.distributed"
    />

    <KnowledgeBasePage v-else-if="current.knowledge" />

    <div v-else class="workspace">
      <article class="document">
        <section id="overview" class="intro">
          <div class="eyebrow">{{ current.tag }}<span class="stable-tag">可用接口</span></div>
          <h1>
            {{ current.title
            }}<span v-if="current.stream" class="title-stream"><Radio :size="26" /></span>
          </h1>
          <p class="lead">{{ current.description }}</p>
          <p class="intro-detail">{{ current.detail }}</p>
          <div class="endpoint-banner">
            <span class="method">POST</span><code>{{ current.path }}</code
            ><button class="icon-button" aria-label="复制接口路径" @click="copyText(current.path)">
              <Copy :size="15" />
            </button>
          </div>
          <div class="endpoint-meta">
            <span><FileJson :size="14" /> application/json</span><ArrowRight :size="13" /><span>{{
              current.stream ? 'text/event-stream' : 'ChatResponseDto'
            }}</span>
          </div>
        </section>
        <div v-if="current.id === 'messages'" class="notice">
          <MessageSquare :size="18" />
          <div>
            <strong>历史消息来自后端固定示例</strong>
            <p>
              当前上下文为「我正在学习 Spring AI
              Alibaba」及对应助手回复。连续发送不会自动保存真实聊天历史。
            </p>
          </div>
        </div>
        <div v-else-if="current.stream" class="notice">
          <Radio :size="18" />
          <div>
            <strong>回答会逐段到达</strong>
            <p>使用 POST + SSE 持续接收内容。当前接口仅返回文本片段，不包含模型名或 Token 用量。</p>
          </div>
        </div>
        <div v-else class="notice">
          <Zap :size="18" />
          <div>
            <strong>在右侧，试试你的第一条请求</strong>
            <p>示例已准备好。修改用户消息或直接发送，即可查看模型的真实响应。</p>
          </div>
        </div>
        <section id="parameters" class="doc-section">
          <div class="section-heading">
            <h2>请求参数</h2>
            <span class="subtle-pill">Request body</span>
          </div>
          <p class="section-description">
            以 JSON 格式发送。<span class="required-dot">*</span> 为必填参数，其余可使用默认值。
          </p>
          <div class="parameter-list">
            <div v-for="param in visibleParams" :key="param.name" class="parameter-row">
              <div class="parameter-title">
                <code
                  >{{ param.name }}<span v-if="param.required" class="required-dot"> *</span></code
                ><span class="type-label">{{ param.type }}</span
                ><span class="param-default">{{
                  param.required ? '必填' : `默认 ${param.default}`
                }}</span>
              </div>
              <p>{{ param.description }}</p>
            </div>
          </div>
        </section>
        <section id="response-schema" class="doc-section">
          <div class="section-heading">
            <h2>响应说明</h2>
            <span class="response-code">200 OK</span>
          </div>
          <p class="section-description">
            {{
              current.stream
                ? 'SSE 事件中的 data 为文本，按到达顺序拼接为完整回答。'
                : '请求成功时返回以下字段，可在右侧切换查看原始 JSON。'
            }}
          </p>
          <div v-if="current.stream" class="stream-example">
            <CodeBlock :code="'data: 你好\n\ndata: ，世界！\n\n'" />
            <p>连接正常结束即完成。停止接收不保证上游立即停止生成。</p>
          </div>
          <div v-else class="response-table-wrap">
            <table class="response-table">
              <thead>
                <tr>
                  <th>字段</th>
                  <th>类型</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="field in responseFields" :key="field[0]">
                  <td>
                    <code>{{ field[0] }}</code>
                  </td>
                  <td>{{ field[1] }}</td>
                  <td>{{ field[2] }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <div class="doc-footer">
          <span>01 · 阿里云模型接入</span
          ><button
            @click="
              navigate(
                endpoints[
                  (endpoints.findIndex((item) => item.id === current.id) + 1) % endpoints.length
                ]!.id,
              )
            "
          >
            下一个接口 <ArrowRight :size="15" />
          </button>
        </div>
      </article>

      <aside class="playground" aria-label="接口调试台">
        <div class="playground-heading">
          <span class="playground-title"><Terminal :size="18" /> 在线调试</span
          ><span class="subtle-pill">PLAYGROUND</span>
        </div>
        <form class="request-panel" @submit.prevent="sendRequest">
          <div class="request-panel-top">
            <span class="method">POST</span><code>{{ current.path }}</code
            ><span v-if="current.stream" class="sse-tag">SSE</span>
          </div>
          <div class="form-body">
            <div class="field-header">
              <label for="message">用户消息 <span class="required-dot">*</span></label
              ><button
                type="button"
                class="text-button"
                :disabled="isLoading"
                @click="draft.message = current.example"
              >
                填入示例 <ArrowDown :size="12" />
              </button>
            </div>
            <textarea
              id="message"
              v-model="draft.message"
              :disabled="isLoading"
              placeholder="输入你想问模型的问题…"
              rows="4"
              maxlength="4000"
              required
            ></textarea>
            <div class="field-hint">
              <span>描述越清晰，回答越有帮助</span><span>{{ draft.message.length }} / 4000</span>
            </div>
            <template v-if="!current.basic"
              ><div class="field-header system-label">
                <label for="system-prompt">系统提示词 <span class="optional">可选</span></label>
              </div>
              <textarea
                id="system-prompt"
                v-model="draft.systemPrompt"
                :disabled="isLoading"
                rows="2"
                maxlength="1000"
                placeholder="留空使用后端默认提示词"
              ></textarea
              ><button
                type="button"
                class="advanced-toggle"
                :aria-expanded="advanced"
                aria-controls="advanced-options"
                @click="advanced = !advanced"
              >
                <Settings2 :size="15" /><span>生成参数</span
                ><span class="advanced-summary"
                  >{{ draft.temperature || '默认' }} / {{ draft.maxTokens || '默认' }}</span
                ><ChevronDown :size="15" :class="{ rotated: advanced }" />
              </button>
              <div v-if="advanced" id="advanced-options" class="advanced-fields">
                <div class="number-fields">
                  <label for="temperature"
                    >Temperature<input
                      id="temperature"
                      v-model="draft.temperature"
                      :disabled="isLoading"
                      type="number"
                      min="0"
                      max="2"
                      step="0.1"
                      placeholder="0.7" /></label
                  ><label for="top-p"
                    >Top P<input
                      id="top-p"
                      v-model="draft.topP"
                      :disabled="isLoading"
                      type="number"
                      min="0"
                      max="1"
                      step="any"
                      placeholder="0.8" /></label
                  ><label for="max-tokens"
                    >Max tokens<input
                      id="max-tokens"
                      v-model="draft.maxTokens"
                      :disabled="isLoading"
                      type="number"
                      min="1"
                      max="4000"
                      step="1"
                      placeholder="1000"
                  /></label>
                </div>
                <label class="stop-label" for="stops"
                  >停止序列 <span class="optional">每行一项，最多 4 项</span></label
                ><textarea
                  id="stops"
                  v-model="draft.stopSequences"
                  :disabled="isLoading"
                  rows="2"
                  placeholder="例如：END"
                ></textarea></div
            ></template>
            <p v-if="validation" class="validation-error" role="alert">{{ validation }}</p>
            <div class="send-row">
              <button
                v-if="isLoading"
                type="button"
                class="primary-button stop-button"
                @click="stopRequest"
              >
                <Square :size="14" fill="currentColor" />停止接收</button
              ><button v-else class="primary-button" type="submit" :disabled="!!validation">
                <Play :size="14" fill="currentColor" />发送请求 <kbd>⌘ ↵</kbd></button
              ><button
                type="button"
                class="reset-button"
                :disabled="isLoading"
                title="重置表单和响应"
                aria-label="重置表单和响应"
                @click="resetDraft"
              >
                <RotateCcw :size="17" />
              </button>
            </div>
          </div>
        </form>
        <div class="code-panel">
          <div class="code-tabs" aria-label="请求示例">
            <button
              v-for="tab in ['json', 'curl', 'java'] as const"
              :key="tab"
              :aria-pressed="codeTab === tab"
              :class="{ active: codeTab === tab }"
              @click="codeTab = tab"
            >
              {{ { json: 'JSON', curl: 'cURL', java: 'Java' }[tab] }}</button
            ><button class="code-copy" aria-label="复制请求示例" @click="copyText(code)">
              <Copy :size="14" />
            </button>
          </div>
          <CodeBlock :code="code" />
        </div>
        <section class="response-panel" aria-label="响应结果" :aria-busy="isLoading">
          <div class="response-heading">
            <h2>响应结果</h2>
            <span class="request-status" role="status" :class="status"
              ><LoaderCircle v-if="isLoading" class="spin" :size="13" /><span
                v-else
                class="status-dot"
              ></span
              >{{ stateLabel }}</span
            >
          </div>
          <div v-if="status !== 'idle'" class="result-toolbar">
            <div class="result-tabs">
              <button :class="{ active: resultTab === 'answer' }" @click="resultTab = 'answer'">
                回答</button
              ><button :class="{ active: resultTab === 'raw' }" @click="resultTab = 'raw'">
                {{ current.stream ? '原始文本 / 错误' : '原始 JSON' }}
              </button>
            </div>
            <span v-if="httpStatus" class="http-status" :class="{ failed: status === 'error' }">{{
              httpStatus
            }}</span
            ><span class="elapsed">{{ (elapsed / 1000).toFixed(1) }}s</span
            ><button
              class="icon-button"
              aria-label="复制响应"
              :disabled="!raw && !content"
              @click="copyText(resultTab === 'raw' ? raw : content)"
            >
              <Copy :size="14" />
            </button>
          </div>
          <div v-if="status === 'idle'" class="empty-response">
            <div class="empty-icon">
              <MessageSquare :size="24" /><span class="empty-spark"
                ><Zap :size="10" fill="currentColor"
              /></span>
            </div>
            <strong>一个好问题，是一切的开始</strong>
            <p>发送请求后，模型的回答会显示在这里。</p>
          </div>
          <div v-else>
            <div v-if="error" class="error-banner" role="alert">
              <CircleHelp :size="17" />
              <div>
                <strong>{{ error }}</strong>
                <p>展开原始响应查看详情；服务端异常请结合 IDEA 控制台排查。</p>
              </div>
            </div>
            <div ref="responsePane" class="response-content" @scroll="onResponseScroll">
              <pre v-if="resultTab === 'raw'">{{ raw || '等待响应数据…' }}</pre>
              <template v-else
                ><div v-if="content" class="answer-text">
                  {{ content }}<span v-if="isLoading && current.stream" class="typing-caret"></span>
                </div>
                <div v-else-if="isLoading" class="thinking">
                  <span></span><span></span><span></span>
                  <p>正在等待模型回复…</p>
                </div>
                <p v-else class="no-content">
                  {{
                    status === 'cancelled'
                      ? '请求已停止，尚未收到内容。'
                      : status === 'error'
                        ? '本次请求未获得回答。'
                        : '请求已结束，未返回文本内容。'
                  }}
                </p></template
              >
            </div>
            <div v-if="response || current.stream" class="token-stats">
              <template v-if="current.stream"
                ><span
                  >已接收 <b>{{ chunks }}</b> 个片段</span
                ><span>Token 用量 <b>未提供</b></span></template
              ><template v-else-if="response"
                ><span
                  >输入 <b>{{ response.promptTokens ?? '未提供' }}</b></span
                ><span
                  >输出 <b>{{ response.completionTokens ?? '未提供' }}</b></span
                ><span
                  >总计 <b>{{ response.totalTokens ?? '未提供' }}</b></span
                ></template
              >
            </div>
            <div v-if="response" class="model-meta">
              模型：{{ response.model || '未提供'
              }}<span>结束原因：{{ response.finishReason || '未提供' }}</span>
            </div>
            <p v-if="status === 'cancelled' && content" class="cancel-note">
              已停止接收，以上为部分回答。
            </p>
          </div>
        </section>
        <div class="playground-footnote">
          <CheckCheck :size="14" /><span>请求通过本地代理发送 · 密钥由后端管理</span>
        </div>
      </aside>
    </div>
    <footer class="page-footer">
      <span>Size Agent <span class="footer-slash">/</span> 学习、构建，然后创造。</span
      ><span>Built with Vue 3 & TypeScript <span class="footer-dot"></span></span>
    </footer>
  </main>
  <Transition name="toast"
    ><div v-if="copyNotice" class="toast" role="status">
      <Check :size="17" />{{ copyNotice
      }}<button aria-label="关闭提示" @click="copyNotice = ''"><X :size="14" /></button></div
  ></Transition>
</template>
