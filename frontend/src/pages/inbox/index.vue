<template>
  <AppShell title="消息" active="inbox">
    <template v-if="!session.token"><wd-button block @click="go('/pages/auth/index')">登录 / 加入社区</wd-button></template>
    <template v-else>
      <wd-segmented v-model:value="filter" :options="['全部', '收到的帮助', '我提供的帮助']" />
      <view class="space" />
      <view v-if="dreamId" class="muted section">当前梦想的消息</view>
      <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
      <wd-status-tip url-prefix="/static/status/" v-else-if="!loading && !filtered.length" image="content" tip="暂无消息" />
      <wd-card v-for="item in filtered" :key="item.id" @click="go('/pages/chat/index?id=' + item.id)">
        <template #title><view class="row"><UserAvatar :src="item.partnerAvatar" :size="36" /><text class="grow">{{ item.partnerName }}</text></view></template>
        <view class="row section"><wd-tag :type="item.status === 'ACCEPTED' ? 'success' : 'primary'" plain>{{ helpStatus(item.status) }}</wd-tag><wd-tag plain type="default">{{ item.isOwner ? '收到的帮助' : '我提供的帮助' }}</wd-tag></view>
        <view class="subheading section">{{ item.dreamTitle }}</view><view class="muted clamp">{{ item.offer }}</view>
        <template #footer><wd-text text="进入交流 →" type="primary" /></template>
      </wd-card>
      <view class="center"><wd-loading v-if="loading" /></view>
      <view class="muted center" v-if="items.length >= 100">展示最近的 100 条帮助交流</view>
    </template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref, computed } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import { request, session, go, helpStatus } from '../../lib/api'
const items = ref([]), filter = ref('全部'), error = ref(''), loading = ref(false), dreamId = ref('')
let timer
const filtered = computed(() => items.value.filter(x => (!dreamId.value || String(x.dreamId) === dreamId.value) && (filter.value === '全部' || (filter.value === '收到的帮助' ? x.isOwner : !x.isOwner))))
async function load() { if (!session.token || loading.value) return; loading.value = true; try { items.value = await request('/helps'); error.value = '' } catch(e) { error.value = e.message } finally { loading.value = false } }
onLoad(options => dreamId.value = options.dreamId || '')
onShow(() => { load(); clearInterval(timer); timer = setInterval(load, 10000) })
onHide(() => clearInterval(timer)); onUnload(() => clearInterval(timer))
</script>
