<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { BookOpen, LoaderCircle, Send, Upload } from '@lucide/vue'
import { askKnowledge, uploadKnowledge, KnowledgeApiError } from '../lib/knowledgeApi'
import type {
  KnowledgeVisibility,
  KnowledgeUploadResponse,
  RagAnswerResponse,
} from '../lib/knowledgeApi'

const tenantId = ref('tenant-001')
const departmentId = ref('department-001')
const visibility = ref<KnowledgeVisibility>('DEPARTMENT')
const file = ref<File | null>(null)
const question = ref('请根据知识库介绍公司的休假制度。')
const uploadResult = ref<KnowledgeUploadResponse | null>(null)
const answer = ref<RagAnswerResponse | null>(null)
const uploadError = ref('')
const askError = ref('')
const uploadDetail = ref('')
const askDetail = ref('')
const pending = ref<'upload' | 'ask' | null>(null)
const busy = computed(() => pending.value !== null)
const scopeError = computed(() =>
  /^[A-Za-z0-9_-]{1,64}$/.test(tenantId.value) && /^[A-Za-z0-9_-]{1,64}$/.test(departmentId.value)
    ? ''
    : '租户和部门 ID 仅支持字母、数字、下划线、短横线，长度 1–64。',
)
const fileError = computed(() => {
  if (!file.value) return '请选择需要上传的文档。'
  if (!/\.(pdf|doc|docx|txt|md)$/i.test(file.value.name))
    return '仅支持 PDF、Word、TXT 和 Markdown 文件。'
  if (!file.value.size) return '不能上传空文件。'
  if (file.value.size >= 20 * 1024 * 1024) return '文件需小于 20 MB（请求还包含表单信息）。'
  return ''
})
const questionError = computed(() =>
  !question.value.trim()
    ? '请输入问题。'
    : question.value.length > 1000
      ? '问题最多 1,000 个字符。'
      : '',
)
let controller: AbortController | undefined
onBeforeUnmount(() => controller?.abort())
watch([tenantId, departmentId], () => {
  uploadResult.value = null
  answer.value = null
  uploadError.value = askError.value = uploadDetail.value = askDetail.value = ''
})
function chooseFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0] ?? null
  uploadResult.value = null
  uploadError.value = uploadDetail.value = ''
}
async function run(kind: 'upload' | 'ask') {
  if (busy.value || scopeError.value || (kind === 'upload' ? fileError.value : questionError.value))
    return
  pending.value = kind
  controller = new AbortController()
  const active = controller
  const timeout = setTimeout(() => active.abort(), 120_000)
  const scope = { tenantId: tenantId.value, departmentId: departmentId.value }
  if (kind === 'upload') {
    uploadResult.value = null
    uploadError.value = uploadDetail.value = ''
  } else {
    answer.value = null
    askError.value = askDetail.value = ''
  }
  try {
    if (kind === 'upload')
      uploadResult.value = await uploadKnowledge(
        scope,
        file.value!,
        visibility.value,
        active.signal,
      )
    else answer.value = await askKnowledge(scope, question.value.trim(), active.signal)
  } catch (error) {
    const message = active.signal.aborted
      ? '已停止等待响应，请检查后端处理结果后再决定是否重试。'
      : error instanceof KnowledgeApiError
        ? error.message
        : '无法连接服务，请确认后端已启动。'
    const detail = error instanceof KnowledgeApiError ? error.detail : ''
    if (kind === 'upload') {
      uploadError.value = message
      uploadDetail.value = detail
    } else {
      askError.value = message
      askDetail.value = detail
    }
  } finally {
    clearTimeout(timeout)
    pending.value = null
  }
}
</script>

<template>
  <div class="tool-workspace">
    <article class="tool-doc">
      <div class="eyebrow">KNOWLEDGE BASE <span class="stable-tag">可用接口</span></div>
      <h1>知识库问答</h1>
      <p class="lead">上传文档，让回答有据可查。</p>
      <p class="intro-detail">
        文档解析、分块并写入向量索引后，可通过自然语言检索知识，查看模型回答及引用来源。
      </p>
      <div class="memory-endpoints tool-endpoints">
        <div>
          <span class="method">POST</span><code>/api/knowledge/documents</code
          ><small>上传文档</small>
        </div>
        <div>
          <span class="method">POST</span><code>/api/knowledge/ask</code><small>知识问答</small>
        </div>
      </div>
      <section class="doc-section">
        <h2>使用流程</h2>
        <div class="parameter-list">
          <div class="parameter-row">
            <strong>01 · 设置知识范围</strong>
            <p>两个接口均通过 X-Tenant-Id、X-Department-Id 请求头传入租户和部门。</p>
          </div>
          <div class="parameter-row">
            <strong>02 · 上传参考文档</strong>
            <p>
              使用 multipart/form-data 提交 file 和 visibility。支持
              PDF、DOC、DOCX、TXT、MD，文件需小于 20 MB。
            </p>
          </div>
          <div class="parameter-row">
            <strong>03 · 提问并核对来源</strong>
            <p>
              以 JSON 提交 question，最多 1,000 字符。响应包含 answer 和
              sources；来源提供文件名、文档 ID，以及可用的页码和相关性分数。
            </p>
          </div>
        </div>
      </section>
      <div class="notice">
        <BookOpen :size="18" />
        <div>
          <strong>可见范围</strong>
          <p>
            部门文档仅供同租户、同部门检索；租户内公开文档可供同租户的其他部门检索。切换租户或部门会清空当前展示结果。
          </p>
        </div>
      </div>
    </article>
    <aside class="tool-console knowledge-console" aria-label="知识库调试">
      <div class="playground-heading">
        <span class="playground-title"><BookOpen :size="18" /> 知识库调试</span
        ><span class="subtle-pill">RAG</span>
      </div>
      <div class="tool-form">
        <div class="knowledge-scope">
          <label class="tool-field"
            >租户 ID<input v-model.trim="tenantId" :disabled="busy" maxlength="64"
          /></label>
          <label class="tool-field"
            >部门 ID<input v-model.trim="departmentId" :disabled="busy" maxlength="64"
          /></label>
        </div>
        <p v-if="scopeError" class="validation-error" role="alert">{{ scopeError }}</p>
      </div>
      <form class="tool-form" :aria-busy="pending === 'upload'" @submit.prevent="run('upload')">
        <div class="section-heading">
          <h2>上传文档</h2>
          <span class="subtle-pill">01</span>
        </div>
        <label class="tool-field"
          >可见范围<select v-model="visibility" :disabled="busy">
            <option value="DEPARTMENT">本部门可见</option>
            <option value="PUBLIC">租户内公开</option>
          </select></label
        >
        <div class="document-picker-field">
          <span class="document-picker-label">选择文档</span>
          <label class="document-picker" :class="{ selected: file, disabled: busy }">
            <input
              type="file"
              accept=".pdf,.doc,.docx,.txt,.md"
              :disabled="busy"
              aria-label="选择或更换文档"
              aria-describedby="file-hint"
              @change="chooseFile"
            />
            <span class="picker-icon"><Upload :size="22" /></span>
            <span class="picker-copy"
              ><strong :title="file?.name">{{ file ? file.name : '点击选择文档' }}</strong
              ><span>{{
                file ? `${(file.size / 1024).toFixed(1)} KB` : '将参考资料添加到知识库'
              }}</span></span
            >
            <span class="picker-action">{{ file ? '更换' : '浏览文件' }}</span>
          </label>
          <p id="file-hint" class="picker-hint">PDF / Word / TXT / Markdown · 小于 20 MB</p>
        </div>
        <p v-if="file && fileError" class="validation-error" role="alert">{{ fileError }}</p>
        <button class="primary-button tool-send" :disabled="busy || !!scopeError || !!fileError">
          <LoaderCircle v-if="pending === 'upload'" class="spin" :size="15" /><Upload
            v-else
            :size="15"
          />{{ pending === 'upload' ? '正在解析并建立索引…' : '上传并建立索引' }}
        </button>
        <p v-if="uploadError" class="validation-error" role="alert">{{ uploadError }}</p>
        <details v-if="uploadDetail" class="knowledge-raw">
          <summary>错误详情</summary>
          <pre>{{ uploadDetail }}</pre>
        </details>
        <div v-if="uploadResult" class="swap-result" role="status">
          <strong>{{ uploadResult.fileName }}</strong
          ><span>状态：{{ uploadResult.status }} · {{ uploadResult.chunkCount }} 个分块</span
          ><span>文档 ID：{{ uploadResult.documentId }}</span>
        </div>
      </form>
      <form class="tool-form" @submit.prevent="run('ask')">
        <div class="section-heading">
          <h2>知识问答</h2>
          <span class="subtle-pill">02</span>
        </div>
        <label class="tool-field"
          >你的问题<textarea
            v-model="question"
            :disabled="busy"
            rows="4"
            maxlength="1000"
            @keydown.meta.enter.prevent.stop="run('ask')"
            @keydown.ctrl.enter.prevent.stop="run('ask')"
          ></textarea>
        </label>
        <p class="section-description">{{ question.length }} / 1000 · 可直接查询已上传的知识</p>
        <p v-if="questionError" class="validation-error">{{ questionError }}</p>
        <button
          class="primary-button tool-send"
          :disabled="busy || !!scopeError || !!questionError"
        >
          <LoaderCircle v-if="pending === 'ask'" class="spin" :size="15" /><Send
            v-else
            :size="15"
          />{{ pending === 'ask' ? '正在检索并生成回答…' : '发送问题' }}
        </button>
      </form>
      <section class="tool-output" aria-live="polite" :aria-busy="pending === 'ask'">
        <div class="section-heading">
          <h2>回答与来源</h2>
          <span v-if="answer" class="response-code">200 OK</span>
        </div>
        <p v-if="askError" class="validation-error" role="alert">{{ askError }}</p>
        <details v-if="askDetail">
          <summary>错误详情</summary>
          <pre>{{ askDetail }}</pre>
        </details>
        <template v-if="answer">
          <div class="tool-answer">{{ answer.answer }}</div>
          <div v-for="(source, index) in answer.sources" :key="index" class="knowledge-source">
            <strong>来源 {{ index + 1 }} · {{ source.fileName }}</strong
            ><span>文档 ID：{{ source.documentId }}</span
            ><span
              >页码：{{ source.pageNumber ?? '未提供' }} · 相关性：{{
                source.score == null ? '未提供' : source.score.toFixed(4)
              }}</span
            >
          </div>
          <p v-if="!answer.sources.length" class="section-description">本次回答未返回引用来源。</p>
          <details>
            <summary>查看原始 JSON</summary>
            <pre>{{ JSON.stringify(answer, null, 2) }}</pre>
          </details>
        </template>
        <div v-else-if="!askError" class="tool-empty">
          <BookOpen :size="25" /><span>{{
            pending === 'ask' ? '正在检索知识库…' : '发送问题后，在这里查看回答与引用来源'
          }}</span>
        </div>
      </section>
    </aside>
  </div>
</template>

<style scoped>
.document-picker-field {
  margin-bottom: 18px;
}
.document-picker-label {
  display: block;
  margin-bottom: 8px;
  color: #66756c;
  font-size: 0.76rem;
  font-weight: 650;
}
.document-picker {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border: 1px dashed #b9d0c3;
  border-radius: 10px;
  background: #f7faf8;
  cursor: pointer;
  transition:
    border-color 0.18s,
    background 0.18s;
}
.document-picker:hover:not(.disabled) {
  border-color: var(--green);
  background: #eef6f1;
}
.document-picker:focus-within {
  outline: 2px solid var(--green);
  outline-offset: 3px;
}
.document-picker.selected {
  border-style: solid;
  background: #f3f8f5;
}
.document-picker.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.document-picker input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: inherit;
}
.picker-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  color: var(--green);
  background: #e6f0ea;
  border-radius: 11px;
}
.picker-copy {
  display: grid;
  gap: 6px;
  flex: 1;
  min-width: 0;
}
.picker-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #354b3e;
  font-size: 0.84rem;
  font-weight: 600;
}
.picker-copy > span {
  font-size: 0.73rem;
  color: var(--muted);
}
.picker-action {
  flex-shrink: 0;
  border: 1px solid #d7e4dc;
  border-radius: 6px;
  background: white;
  padding: 7px 10px;
  font-size: 0.73rem;
  font-weight: 600;
  color: var(--green);
}
.picker-hint {
  margin: 9px 0 0;
  font-size: 0.71rem;
  color: var(--muted);
  line-height: 1.6;
}
@media (max-width: 380px) {
  .document-picker {
    gap: 8px;
    padding: 16px 10px;
  }
  .picker-action {
    padding: 6px;
  }
}

.knowledge-console {
  position: static;
  max-height: none;
}
.knowledge-scope {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.tool-field select {
  width: 100%;
  margin-top: 7px;
  padding: 10px;
  border: 1px solid #dfe7e2;
  border-radius: 7px;
  background: white;
  color: #35463c;
}
.knowledge-source {
  display: grid;
  gap: 7px;
  padding: 14px;
  margin-top: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.78rem;
  overflow-wrap: anywhere;
}
.knowledge-source span {
  color: var(--muted);
}
.knowledge-raw {
  margin-top: 12px;
}
.knowledge-raw pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.swap-result {
  overflow-wrap: anywhere;
}
@media (max-width: 480px) {
  .knowledge-scope {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
