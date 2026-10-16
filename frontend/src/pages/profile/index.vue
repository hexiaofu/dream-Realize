<template>
  <AppShell title="梦想家的名片" back>
    <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <template v-if="profile"><wd-card><template #title><view class="row"><UserAvatar :src="profile.avatarUrl" :size="56" /><text class="grow">{{ profile.nickname }}</text></view></template><view v-if="profile.city" class="muted section">{{ profile.city }}</view><view v-if="profile.bio" class="body-text">{{ profile.bio }}</view></wd-card><wd-card v-if="profile.skills" title="我愿意提供的帮助"><view class="body-text">{{ profile.skills }}</view></wd-card><view class="subheading section">公开的梦想</view><DreamCard v-for="dream in profile.dreams" :key="dream.id" :dream="dream" /><wd-status-tip url-prefix="/static/status/" v-if="!profile.dreams.length" image="content" tip="暂无公开梦想" /></template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import DreamCard from '../../components/DreamCard.vue'
import { request } from '../../lib/api'
const profile = ref(null), error = ref(''); let id
async function load() { try { error.value = ''; profile.value = await request('/members/' + id) } catch(e) { error.value = e.message } }
onLoad(options => { id = options.id })
onShow(() => { if (id) load() })
</script>
