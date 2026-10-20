<template>
  <AppShell title="梦想家的名片" back>
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <DreamLoading v-if="!profile && !error" text="正在翻开这张名片" />
    <template v-if="profile"><wd-card custom-class="profile-card"><template #title><view class="row"><view class="profile-avatar"><UserAvatar :src="profile.avatarUrl" :size="56" /></view><text class="grow profile-name">{{ profile.nickname }}</text></view></template><view v-if="profile.city" class="muted section">{{ profile.city }}</view><view v-if="profile.bio" class="body-text">{{ profile.bio }}</view><view v-if="!isSelf" class="actions"><wd-button block icon="chat" :loading="opening" @click="message">发私信</wd-button></view></wd-card><wd-card v-if="profile.skills" title="我愿意提供的帮助"><view class="body-text">{{ profile.skills }}</view></wd-card><view class="subheading section">公开的梦想</view><view class="stagger"><DreamCard v-for="dream in profile.dreams" :key="dream.id" :dream="dream" /></view><wd-status-tip url-prefix="/static/status/" v-if="!profile.dreams.length" image="content" tip="暂无公开梦想" /></template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import DreamCard from '../../components/DreamCard.vue'
import DreamLoading from '../../components/DreamLoading.vue'
import { go, notify, request, requireLogin, session } from '../../lib/api'
const profile = ref(null), error = ref(''), opening = ref(false); let id
const isSelf = computed(() => session.user && String(session.user.id) === String(id))
// 打开（首次时创建）与该成员的私信会话后进入聊天页。
async function message() {
  if (opening.value || !requireLogin()) return
  opening.value = true
  try { const conversation = await request('/direct/conversations', 'POST', { memberId: Number(id) }); go('/pages/direct/index?id=' + conversation.id) }
  catch (e) { notify(e) } finally { opening.value = false }
}
async function load() { try { error.value = ''; profile.value = await request('/members/' + id) } catch(e) { error.value = e.message } }
onLoad(options => { id = options.id })
onShow(() => { if (id) load() })
</script>
<style scoped>
:deep(.profile-card) { background: linear-gradient(180deg, rgba(236, 229, 255, .42) 0%, rgba(255, 255, 255, .2) 70%) !important; }
.profile-avatar { padding: 3px; border-radius: 50%; background: conic-gradient(#6E56F8, #FF8DB8, #FFB84D, #6E56F8); animation: dream-pop .6s var(--dream-spring) backwards; }
.profile-avatar :deep(.user-avatar) { background: #fff; border-radius: 50%; }
.profile-name { font-size: 19px; font-weight: 700; }
</style>
