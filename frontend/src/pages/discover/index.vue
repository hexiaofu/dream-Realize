<template>
  <AppShell active="discover" title="圆梦">
    <wd-search v-model="keyword" placeholder="搜索梦想" hide-cancel @search="reload" @clear="reload" />
    <wd-tabs v-model="category" slidable @change="reload">
      <wd-tab v-for="item in categories" :key="item.value" :name="item.value" :title="item.label" />
    </wd-tabs>
    <view class="space" />
    <view class="row between section">
      <view class="subheading">梦想广场</view>
      <wd-button type="text" size="small" icon="filter" @click="showFilters = !showFilters">筛选</wd-button>
    </view>
    <wd-cell-group v-if="showFilters" border class="section">
      <wd-select-picker type="radio" :show-confirm="false" v-model="helpType" label="我能提供" :columns="[{ value: '', label: '所有帮助' }, ...helpTypes]" @confirm="reload" />
      <wd-select-picker type="radio" :show-confirm="false" v-model="status" label="梦想状态" :columns="[{ value: '', label: '所有状态' }, ...statuses]" @confirm="reload" />
    </wd-cell-group>
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button size="small" @click="reload">重新加载</wd-button></template></wd-status-tip>
    <wd-status-tip url-prefix="/static/status/" v-else-if="!loading && !items.length" image="search" tip="暂无匹配的梦想" />
    <DreamCard v-for="dream in items" :key="dream.id" :dream="dream" />
    <view class="center section"><wd-loading v-if="loading" /><wd-button v-else-if="hasMore" plain block @click="loadMore">加载更多</wd-button></view>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import DreamCard from '../../components/DreamCard.vue'
import { request, query, categories, helpTypes, statuses } from '../../lib/api'
const items = ref([]), keyword = ref(''), category = ref(''), helpType = ref(''), status = ref('')
const showFilters = ref(false), loading = ref(false), hasMore = ref(false), error = ref('')
let page = 1, generation = 0
async function load(reset = false) {
  if (!reset && loading.value) return
  const current = ++generation
  loading.value = true; error.value = ''
  const requestedPage = reset ? 1 : page + 1
  try {
    const result = await request('/dreams?' + query({ page: requestedPage, keyword: keyword.value, category: category.value, helpType: helpType.value, status: status.value }))
    if (current !== generation) return
    items.value = reset ? result.items : [...items.value, ...result.items]; hasMore.value = result.hasMore; page = requestedPage
  } catch (e) { if (current === generation) error.value = e.message }
  finally { if (current === generation) loading.value = false }
}
function reload() { load(true) }
function loadMore() { if (hasMore.value && !loading.value) load() }
onShow(reload)
onReachBottom(loadMore)
</script>
