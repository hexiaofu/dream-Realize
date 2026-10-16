<template>
  <AppShell title="梦想详情" back>
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <view v-else-if="!dream" class="center"><wd-loading /></view>
    <template v-else>
      <view class="row section"><wd-tag plain>{{ label(categories, dream.category) }}</wd-tag><wd-tag type="success" plain>{{ label(statuses, dream.status) }}</wd-tag><wd-tag v-if="dream.isOwner" type="warning" plain>我的梦想</wd-tag></view>
      <view class="heading">{{ dream.title }}</view>
      <wd-cell-group border class="section">
        <wd-cell :title="dream.authorName" :label="date(dream.createdAt) + ' 发布'" :is-link="!dream.anonymous" @click="!dream.anonymous && go('/pages/profile/index?id=' + dream.authorId)"><template #icon><UserAvatar :src="dream.authorAvatar" :size="36" /></template></wd-cell>
      </wd-cell-group>
      <wd-card v-if="dream.story || dream.media?.length" title="梦想描述"><view v-if="dream.story" class="body-text">{{ dream.story }}</view><MediaGallery :media="dream.media" /></wd-card>
      <wd-cell-group border title="帮助信息" class="section">
        <wd-cell title="需要的帮助" :value="label(helpTypes, dream.helpType)" />
        <wd-cell title="帮助条件" :value="dream.rewardType === 'FREE' ? '期待无偿帮助' : '条件由双方协商'" />
        <wd-cell title="交流方式" :value="label(contactModes, dream.contactMode)" />
        <wd-cell v-if="dream.city" title="所在城市" :value="dream.city" />
        <wd-cell title="已接受的帮助" :value="dream.helperCount + ' 份'" />
      </wd-cell-group>
      <wd-card v-if="dream.helpType === 'CASH'" title="现金心愿">
        <view class="row between section"><view><view class="muted">发布者记录已获得</view><wd-text :text="'¥ ' + dream.receivedAmount" type="primary" size="26px" bold /></view><view v-if="dream.targetAmount" class="muted">期望 ¥ {{ dream.targetAmount }}</view></view>
        <wd-progress v-if="dream.targetAmount" :percentage="Math.min(100, Math.round(dream.receivedAmount / dream.targetAmount * 100))" hide-text />
      </wd-card>
      <view class="row between section"><view class="subheading">圆梦足迹</view><wd-button v-if="dream.isOwner" type="text" @click="openProgress">记录新进展</wd-button></view>
      <wd-card v-for="update in dream.updates" :key="update.id"><view class="muted section">{{ date(update.createdAt) }}</view><view class="body-text">{{ update.content }}</view><MediaGallery :media="update.media" /></wd-card>
      <wd-status-tip url-prefix="/static/status/" v-if="!dream.updates.length" image="content" tip="暂无进展" />
      <view class="actions">
        <wd-button v-if="dream.isOwner" block size="large" @click="go('/pages/inbox/index?dreamId=' + dream.id)">查看收到的帮助</wd-button>
        <wd-button v-else-if="dream.myHelpId" block size="large" @click="go('/pages/chat/index?id=' + dream.myHelpId)">查看我的帮助与交流</wd-button>
        <wd-button v-else-if="['OPEN','IN_PROGRESS'].includes(dream.status)" block size="large" icon="heart" @click="openHelp">我愿意帮助</wd-button>
        <wd-button v-else block disabled>{{ dream.status === 'COMPLETED' ? '这个梦想已实现' : '梦想暂时停一停' }}</wd-button>
      </view>
    </template>
    <wd-popup v-model="helpOpen" position="bottom" closable safe-area-inset-bottom>
      <view class="page-body"><view class="heading">提供帮助</view>
        <wd-cell-group border><wd-textarea v-model="help.offer" label="我能提供" placeholder="填写帮助内容与条件" :maxlength="1000" show-word-limit /><wd-select-picker type="radio" :show-confirm="false" v-model="help.rewardType" label="帮助条件" :columns="rewardTypes" /></wd-cell-group>
        <view class="actions"><wd-button block :loading="saving" @click="sendHelp">发送帮助意愿</wd-button></view>
      </view>
    </wd-popup>
    <wd-popup v-model="progressOpen" position="bottom" closable safe-area-inset-bottom>
      <scroll-view scroll-y style="max-height: 85vh"><view class="page-body"><view class="heading">记录进展</view>
        <wd-cell-group border><wd-textarea v-model="progress.content" label="新的进展" placeholder="填写进展内容" :maxlength="2000" show-word-limit /><wd-select-picker type="radio" :show-confirm="false" v-model="progress.status" label="梦想状态" :columns="statuses" /><wd-input v-if="dream?.helpType === 'CASH'" v-model="progress.amount" label="已获得金额" type="digit" placeholder="选填，累计金额" /></wd-cell-group>
        <view class="space" /><MediaUploader v-model="progress.media" :disabled="saving" @blocked="mediaBlocked = $event" />
        <view class="actions"><wd-button block :loading="saving" :disabled="mediaBlocked" @click="sendProgress">{{ mediaBlocked ? '请先完成附件上传' : '保存这段足迹' }}</wd-button></view>
      </view></scroll-view>
    </wd-popup>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import MediaUploader from '../../components/MediaUploader.vue'
import MediaGallery from '../../components/MediaGallery.vue'
import { request, go, notify, requireLogin, label, date, categories, statuses, helpTypes, contactModes, rewardTypes } from '../../lib/api'
const dream = ref(null), error = ref(''), helpOpen = ref(false), progressOpen = ref(false), saving = ref(false)
const mediaBlocked = ref(false)
const help = reactive({ offer: '', rewardType: 'FREE' }), progress = reactive({ content: '', status: 'OPEN', amount: '', media: [] })
let id
onLoad(options => id = options.id)
onShow(load)
async function load() { if (!id) return; try { error.value = ''; dream.value = await request('/dreams/' + id) } catch(e) { error.value = e.message } }
function openHelp() { if (requireLogin()) helpOpen.value = true }
function openProgress() { progress.status = dream.value.status; progress.amount = String(dream.value.receivedAmount); progressOpen.value = true }
async function sendHelp() {
  if (saving.value) return
  if (!help.offer.trim()) return notify('先写下你能提供的帮助')
  saving.value = true
  try { const result = await request(`/dreams/${id}/helps`, 'POST', help); helpOpen.value = false; go('/pages/chat/index?id=' + result.id) }
  catch(e) { notify(e) } finally { saving.value = false }
}
async function sendProgress() {
  if (saving.value || mediaBlocked.value) return
  if (!progress.content.trim()) return notify('写下一点进展吧')
  if (dream.value.helpType === 'CASH' && progress.amount && !/^\d+(\.\d{1,2})?$/.test(progress.amount)) return notify('请填写正确的累计金额')
  saving.value = true
  try { dream.value = await request(`/dreams/${id}/updates`, 'POST', { content: progress.content, status: progress.status, mediaIds: progress.media.map(file => file.id), receivedAmount: dream.value.helpType === 'CASH' && progress.amount !== '' ? Number(progress.amount) : null }); progress.content = ''; progress.media = []; progressOpen.value = false }
  catch(e) { notify(e) } finally { saving.value = false }
}
</script>
