<template><text class="animated-number">{{ shown }}</text></template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { onBeforeUnmount, ref, watch } from 'vue'
const props = defineProps({ value: { type: Number, default: 0 }, duration: { type: Number, default: 900 } })
const shown = ref(0); let timer
// 数字从当前值缓动滚到目标值；setTimeout 在 H5 与小程序都可用。
watch(() => props.value, (target) => {
  clearTimeout(timer)
  const from = shown.value, start = Date.now()
  const step = () => {
    const t = Math.min(1, (Date.now() - start) / props.duration), eased = 1 - Math.pow(1 - t, 3)
    shown.value = Math.round(from + (target - from) * eased)
    if (t < 1) timer = setTimeout(step, 16)
  }
  step()
}, { immediate: true })
onBeforeUnmount(() => clearTimeout(timer))
</script>
<style scoped>
.animated-number { font-variant-numeric: tabular-nums; }
</style>
