<template>
  <view class="user-avatar" :style="{ width: size + 'px', height: size + 'px' }">
    <wd-img v-if="url && !failed" :key="url" :src="url" :width="size" :height="size" round mode="aspectFill" @error="failed = true">
      <template #loading><wd-icon name="user-circle" :size="size + 'px'" color="#21877a" /></template>
    </wd-img>
    <wd-icon v-else name="user-circle" :size="size + 'px'" color="#21877a" />
  </view>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, ref, watch } from 'vue'
import { mediaUrl } from '../lib/api'
const props = defineProps({ src: { type: String, default: '' }, size: { type: Number, default: 40 } })
const url = computed(() => mediaUrl(props.src)), failed = ref(false)
watch(url, () => { failed.value = false })
</script>
<style scoped>
.user-avatar { display: flex; flex-shrink: 0; }
</style>
