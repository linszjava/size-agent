<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ code: string }>()
const tokens = computed(() =>
  props.code
    .split(
      /("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|\b(?:return|new|const|true|false|null|import|from)\b|\b\d+(?:\.\d+)?\b)/g,
    )
    .map((text, index, all) => ({
      text,
      style:
        text.startsWith('"') || text.startsWith("'")
          ? all[index + 1]?.trimStart().startsWith(':')
            ? 'code-key'
            : 'code-string'
          : /^(return|new|const|true|false|null|import|from)$/.test(text)
            ? 'code-keyword'
            : /^\d/.test(text)
              ? 'code-number'
              : '',
    })),
)
</script>

<template>
  <pre
    class="code-block"
  ><code><span v-for="(token, index) in tokens" :key="index" :class="token.style">{{ token.text }}</span></code></pre>
</template>
