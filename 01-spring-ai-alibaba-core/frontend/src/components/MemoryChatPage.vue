<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { Database, History, LoaderCircle, MessageSquare, Radio, Send, Trash2 } from '@lucide/vue'
import { clearMemory, memoryChat, memoryHistory, streamMemory } from '../lib/memoryApi'
import type { MemoryMessage } from '../lib/memoryApi'

const idPattern = /^[A-Za-z0-9_-]{1,64}$/
const userId = ref('user-001')
const conversationId = ref('conversation-001')
const message = ref('我叫小林，请记住我的名字。')
const messages = ref<MemoryMessage[]>([])
const loading = ref(false)
const streaming = ref(false)
const error = ref('')
const notice = ref('')
const list = ref<HTMLElement>()
let controller: AbortController | null = null

const validation = computed(() => {
  if (!idPattern.test(userId.value)) return '用户 ID 仅支持字母、数字、下划线和短横线，长度 1–64。'
  if (!idPattern.test(conversationId.value))
    return '会话 ID 仅支持字母、数字、下划线和短横线，长度 1–64。'
  if (!message.value.trim()) return '请输入用户消息。'
  if (message.value.length > 4000) return '用户消息最多 4,000 个字符。'
  return ''
})

async function scrollBottom() {
  await nextTick()
  list.value?.scrollTo({ top: list.value.scrollHeight, behavior: 'smooth' })
}
async function loadHistory(showNotice = true) {
  if (!idPattern.test(userId.value) || !idPattern.test(conversationId.value)) return
  error.value = ''
  try {
    messages.value = await memoryHistory(userId.value, conversationId.value)
    if (showNotice) notice.value = `已读取 ${messages.value.length} 条消息`
    await scrollBottom()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}
async function send(stream = false) {
  if (validation.value || loading.value) return
  error.value = ''
  notice.value = ''
  loading.value = true
  streaming.value = stream
  const text = message.value.trim()
  messages.value.push({ type: 'USER', text })
  message.value = ''
  await scrollBottom()
  try {
    if (stream) {
      controller = new AbortController()
      const reply = { type: 'ASSISTANT', text: '' }
      messages.value.push(reply)
      await streamMemory(
        userId.value,
        conversationId.value,
        text,
        (chunk) => {
          reply.text += chunk
          void scrollBottom()
        },
        controller.signal,
      )
    } else {
      const result = await memoryChat(userId.value, conversationId.value, text)
      messages.value.push({ type: 'ASSISTANT', text: result.content })
    }
    await loadHistory(false)
  } catch (e) {
    if ((e as Error).name !== 'AbortError') error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
    streaming.value = false
    controller = null
  }
}
async function clear() {
  if (!idPattern.test(userId.value) || !idPattern.test(conversationId.value) || loading.value)
    return
  error.value = ''
  try {
    await clearMemory(userId.value, conversationId.value)
    messages.value = []
    notice.value = '当前会话记忆已清空'
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}
function stopStream() {
  controller?.abort()
}
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <div class="memory-workspace">
    <article class="memory-doc">
      <div class="eyebrow">MEMORY <span class="stable-tag">可用接口</span></div>
      <h1>Chat Memory</h1>
      <p class="lead">让模型记住同一段对话中的上下文。</p>
      <p class="intro-detail">
        用户 ID 用于隔离不同用户，会话 ID 用于区分同一用户的多个对话。连续发送两条相关问题，即可验证
        MySQL 中保存的聊天记忆。
      </p>
      <div class="memory-endpoints">
        <div>
          <span class="method">POST</span><code>/api/memory/chat</code><small>完整回答</small>
        </div>
        <div>
          <span class="method">POST</span><code>/api/memory/stream</code><small>SSE 流式回答</small>
        </div>
        <div>
          <span class="method get">GET</span><code>/api/memory/conversations/{conversationId}</code
          ><small>查询历史</small>
        </div>
        <div>
          <span class="method delete">DELETE</span
          ><code>/api/memory/conversations/{conversationId}</code><small>清空历史</small>
        </div>
      </div>
      <div class="notice">
        <Database :size="18" />
        <div>
          <strong>记忆保存在 MySQL</strong>
          <p>
            页面请求会携带 <code>X-User-Id</code>。后端将用户 ID 和会话 ID
            合成为内部记忆键，避免不同用户共享历史。
          </p>
        </div>
      </div>
      <section class="doc-section">
        <div class="section-heading"><h2>推荐测试步骤</h2></div>
        <ol class="memory-steps">
          <li>发送“我叫小林，请记住我的名字。”</li>
          <li>接着发送“我叫什么名字？”</li>
          <li>点击“读取历史”查看数据库中的消息，再点击“清空记忆”验证删除接口。</li>
        </ol>
      </section>
    </article>
    <aside class="memory-console">
      <div class="playground-heading">
        <span class="playground-title"><MessageSquare :size="18" /> 对话调试</span
        ><span class="subtle-pill">MYSQL MEMORY</span>
      </div>
      <div class="memory-identity">
        <label>用户 ID<input v-model.trim="userId" :disabled="loading" maxlength="64" /></label>
        <label
          >会话 ID<input v-model.trim="conversationId" :disabled="loading" maxlength="64"
        /></label>
      </div>
      <div ref="list" class="memory-messages">
        <div v-if="!messages.length" class="memory-empty">
          <Database :size="28" /><strong>还没有加载对话</strong><span>发送消息或读取已有历史</span>
        </div>
        <div
          v-for="(item, index) in messages"
          :key="index"
          class="memory-message"
          :class="item.type.toLowerCase()"
        >
          <span>{{ item.type === 'USER' ? '你' : 'AI' }}</span>
          <p>{{ item.text || '正在生成…' }}</p>
        </div>
      </div>
      <div class="memory-composer">
        <textarea
          v-model="message"
          :disabled="loading"
          rows="3"
          maxlength="4000"
          placeholder="输入消息…"
          @keydown.meta.enter.prevent="send(false)"
          @keydown.ctrl.enter.prevent="send(false)"
        ></textarea>
        <p v-if="validation" class="validation-error">{{ validation }}</p>
        <p v-if="error" class="validation-error">{{ error }}</p>
        <p v-if="notice" class="memory-notice">{{ notice }}</p>
        <div class="memory-actions">
          <button class="reset-button labelled" :disabled="loading" @click="loadHistory()">
            <History :size="15" />读取历史
          </button>
          <button class="reset-button labelled danger" :disabled="loading" @click="clear">
            <Trash2 :size="15" />清空记忆
          </button>
          <span></span>
          <button v-if="loading" class="primary-button stop-button" @click="stopStream">
            <LoaderCircle class="spin" :size="15" />停止
          </button>
          <template v-else
            ><button class="reset-button labelled" :disabled="!!validation" @click="send(true)">
              <Radio :size="15" />流式发送</button
            ><button class="primary-button" :disabled="!!validation" @click="send(false)">
              <Send :size="15" />发送
            </button></template
          >
        </div>
      </div>
    </aside>
  </div>
</template>
