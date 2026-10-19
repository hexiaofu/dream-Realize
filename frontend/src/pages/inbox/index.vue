<template>
  <AppShell title="消息" active="inbox">
    <GuestPrompt v-if="!session.token" title="登录后查看圆梦交流" hint="你收到和提供的每一份帮助，都会在这里相遇" icon="chat" />
    <template v-else>
      <wd-segmented v-model:value="filter" :options="['全部', '收到的帮助', '我提供的帮助']" custom-class="inbox-segmented" />
      <view class="space" />
      <view v-if="dreamId" class="muted section">当前梦想的消息</view>
      <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
      <wd-status-tip url-prefix="/static/status/" v-else-if="!loading && !filtered.length" image="content" tip="暂无消息" />
      <DreamSkeleton v-if="loading && !items.length" />
      <view class="stagger" :key="filter">
        <wd-card v-for="item in filtered" :key="item.id" :custom-class="'inbox-card' + (item.status === 'PENDING' ? ' is-pending' : '')" @click="go('/pages/chat/index?id=' + item.id)">
          <template #title><view class="row"><view class="inbox-avatar"><UserAvatar :src="item.partnerAvatar" :size="36" /><view v-if="item.status === 'PENDING'" class="pending-dot" /></view><text class="grow">{{ item.partnerName }}</text></view></template>
          <view class="row section"><wd-tag :type="item.status === 'ACCEPTED' ? 'success' : 'primary'" plain>{{ helpStatus(item.status) }}</wd-tag><wd-tag plain type="default">{{ item.isOwner ? '收到的帮助' : '我提供的帮助' }}</wd-tag></view>
          <view class="inbox-title section">{{ item.dreamTitle }}</view><view class="muted clamp">{{ item.offer }}</view>
          <template #footer><view class="row between"><text class="muted">{{ item.status === 'PENDING' ? '等待你们开始交流' : '继续你们的对话' }}</text><view class="more-pill"><text class="more-pill__text">进入交流</text><view class="more-pill__arrow"><wd-icon name="arrow-right" size="12px" color="#fff" /></view></view></view></template>
        </wd-card>
      </view>
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
import DreamSkeleton from '../../components/DreamSkeleton.vue'
import GuestPrompt from '../../components/GuestPrompt.vue'
import { request, session, go, helpStatus } from '../../lib/api'
const items = ref([]), filter = ref('全部'), error = ref(''), loading = ref(false), dreamId = ref('')
let timer
const filtered = computed(() => items.value.filter(x => (!dreamId.value || String(x.dreamId) === dreamId.value) && (filter.value === '全部' || (filter.value === '收到的帮助' ? x.isOwner : !x.isOwner))))
async function load() { if (!session.token || loading.value) return; loading.value = true; try { items.value = await request('/helps'); error.value = '' } catch(e) { error.value = e.message } finally { loading.value = false } }
onLoad(options => dreamId.value = options.dreamId || '')
onShow(() => { load(); clearInterval(timer); timer = setInterval(load, 10000) })
onHide(() => clearInterval(timer)); onUnload(() => clearInterval(timer))
</script>
<style scoped>
/* 待回应的交流带呼吸红点，提醒尽快回复。 */
:deep(.inbox-segmented) { border-radius: 999px !important; animation: dream-rise .5s var(--dream-ease) backwards; }
:deep(.inbox-segmented .wd-segmented__item) { border-radius: 999px !important; transition: color .3s ease; }
:deep(.inbox-segmented .wd-segmented__item--active) { border-radius: 999px !important; box-shadow: 0 4px 12px rgba(110, 86, 248, .18); transition: transform .4s var(--dream-ease) !important; }
.inbox-avatar { position: relative; }
.inbox-title { font-size: 16px; font-weight: 700; line-height: 1.5; }
.pending-dot { position: absolute; top: 0; right: 0; width: 10px; height: 10px; border-radius: 50%; background: var(--dream-pink); border: 2px solid #fff; animation: dream-breathe 1.4s ease-in-out infinite; }
:deep(.inbox-card.is-pending) { border-color: rgba(255, 141, 184, .35); }
</style>
