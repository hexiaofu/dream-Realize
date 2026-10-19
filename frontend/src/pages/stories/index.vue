<template>
  <AppShell active="stories" title="圆梦故事">
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load(true)">重试</wd-button></template></wd-status-tip>
    <wd-status-tip url-prefix="/static/status/" v-else-if="!loading && !items.length" image="content" tip="暂无圆梦故事" />
    <DreamSkeleton v-if="loading && !items.length" />
    <view class="stagger" :key="listVersion"><DreamCard v-for="item in items" :key="item.id" :dream="item" /></view>
    <view class="center"><DreamLoading v-if="loading && items.length" inline text="翻开下一页故事" /><wd-button v-else-if="hasMore" plain @click="load(false)">更多故事</wd-button></view>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import DreamCard from '../../components/DreamCard.vue'
import DreamLoading from '../../components/DreamLoading.vue'
import DreamSkeleton from '../../components/DreamSkeleton.vue'
import { request } from '../../lib/api'
const items = ref([]), loading = ref(false), error = ref(''), hasMore = ref(false), listVersion = ref(0)
let page = 1
async function load(reset) {
  if (loading.value) return
  loading.value = true; error.value = ''
  const next = reset ? 1 : page + 1
  try { const data = await request(`/dreams?status=COMPLETED&page=${next}`); if (reset) listVersion.value++; items.value = reset ? data.items : [...items.value, ...data.items]; hasMore.value = data.hasMore; page = next }
  catch(e) { error.value = e.message } finally { loading.value = false }
}
onShow(() => load(true))
</script>
