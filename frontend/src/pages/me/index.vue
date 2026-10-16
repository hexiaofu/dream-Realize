<template>
  <AppShell active="me" title="我的圆梦空间">
    <template v-if="!session.token"><wd-button block @click="go('/pages/auth/index')">登录 / 加入社区</wd-button></template>
    <template v-else>
      <wd-card v-if="session.user?.restriction?.mode === 'READ_ONLY'" title="账号已限制发布">
        <view class="body-text">{{ session.user.restriction.reason }}</view>
        <view class="muted">{{ session.user.restriction.expiresAt ? '截止 ' + session.user.restriction.expiresAt.replace('T', ' ').slice(0, 16) : '长期限制' }}</view>
      </wd-card>
      <wd-card>
        <template #title><view class="row"><UserAvatar :src="session.user?.avatarUrl" :size="56" /><text class="grow">{{ session.user?.nickname || '梦想家' }}</text></view></template>
        <view class="row section"><wd-tag type="success" plain>社区成员</wd-tag><wd-text v-if="session.user?.city" :text="session.user.city" type="info" /></view>
        <view v-if="session.user?.bio" class="body-text">{{ session.user.bio }}</view>
        <template #footer><wd-button size="small" plain @click="edit">编辑资料</wd-button></template>
      </wd-card>
      <wd-cell-group border class="section"><wd-cell title="我愿意提供的帮助" :label="session.user?.skills || '未填写'" is-link @click="edit" /><wd-cell title="我的帮助与交流" is-link @click="go('/pages/inbox/index')" /><wd-cell title="查看公开名片" label="不展示匿名梦想" is-link @click="go('/pages/profile/index?id=' + session.user.id)" /></wd-cell-group>
      <view class="row between section"><view class="subheading">我的梦想 · {{ total }}</view><wd-button type="text" @click="go('/pages/publish/index')">发布梦想</wd-button></view>
      <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load(true)">重试</wd-button></template></wd-status-tip>
      <DreamCard v-for="dream in dreams" :key="dream.id" :dream="dream" />
      <wd-status-tip url-prefix="/static/status/" v-if="!loading && !error && !dreams.length" image="content" tip="暂无梦想" />
      <wd-button v-if="hasMore" block plain :loading="loading" @click="load(false)">更多梦想</wd-button>
      <view class="actions center"><wd-button type="text" @click="logout">退出登录</wd-button></view>
    </template>
    <wd-popup v-model="editing" position="bottom" :z-index="100" :closable="!saving" :close-on-click-modal="!saving" safe-area-inset-bottom>
      <scroll-view scroll-y style="max-height: 85vh"><view class="page-body">
        <view class="heading">编辑资料</view>
        <wd-cell-group border>
          <AvatarEditor v-if="editing" v-model="avatar" :disabled="saving" @blocked="avatarBlocked = $event" />
          <wd-input v-model="profile.nickname" label="昵称" :maxlength="30" :disabled="saving" />
          <wd-input v-model="profile.city" label="城市" :maxlength="60" :disabled="saving" />
          <wd-textarea v-model="profile.bio" label="关于我" placeholder="个人简介" :maxlength="500" :disabled="saving" auto-height fixed />
          <wd-textarea v-model="profile.skills" label="我能帮助" placeholder="技能与资源" :maxlength="200" :disabled="saving" auto-height fixed />
        </wd-cell-group>
        <view class="actions"><wd-button block :loading="saving" :disabled="avatarBlocked" @click="save">保存</wd-button></view>
      </view></scroll-view>
    </wd-popup>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import DreamCard from '../../components/DreamCard.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import AvatarEditor from '../../components/AvatarEditor.vue'
import { request, session, clearSession, go, notify } from '../../lib/api'
const dreams = ref([]), total = ref(0), editing = ref(false), saving = ref(false), loading = ref(false), hasMore = ref(false), error = ref('')
const profile = reactive({ nickname: '', bio: '', city: '', skills: '' }); let page = 1
const avatar = ref({ id: '', url: '' }), avatarBlocked = ref(false)
async function load(reset) {
  if (!session.token || loading.value) return
  loading.value = true; error.value = ''; const next = reset ? 1 : page + 1
  try { const [me, result] = await Promise.all([request('/me'), request(`/dreams?mine=true&page=${next}`)]); session.user = me; uni.setStorageSync('dream-user', me); dreams.value = reset ? result.items : [...dreams.value, ...result.items]; total.value = result.total; hasMore.value = result.hasMore; page = next }
  catch(e) { error.value = e.message } finally { loading.value = false }
}
function edit() {
  if (session.user?.restriction?.mode === 'READ_ONLY') return notify('账号已限制修改资料')
  for (const key of Object.keys(profile)) profile[key] = session.user?.[key] || ''
  avatar.value = { id: session.user?.avatarId || '', url: session.user?.avatarUrl || '' }
  avatarBlocked.value = false; editing.value = true
}
async function save() { if (saving.value || avatarBlocked.value) return; if (!profile.nickname.trim()) return notify('请填写昵称'); saving.value = true; try { session.user = await request('/me', 'PUT', { ...profile, avatarId: avatar.value.id }); uni.setStorageSync('dream-user', session.user); editing.value = false; await load(true); notify('已保存') } catch(e) { notify(e) } finally { saving.value = false } }
function logout() { uni.showModal({ title: '退出登录', content: '确定退出当前账号吗？', success: async result => { if (!result.confirm) return; try { await request('/auth/logout', 'POST'); clearSession(); dreams.value = [] } catch(e) { notify(e) } } }) }
onShow(() => load(true))
</script>
