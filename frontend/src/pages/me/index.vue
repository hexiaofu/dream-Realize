<template>
  <AppShell active="me" title="我的圆梦空间">
    <GuestPrompt v-if="!session.token" title="开启你的圆梦空间" hint="写下梦想、记录足迹，也把你的温柔分享给别人" icon="user" />
    <template v-else>
      <wd-card v-if="session.user?.restriction?.mode === 'READ_ONLY'" title="账号已限制发布">
        <view class="body-text">{{ session.user.restriction.reason }}</view>
        <view class="muted">{{ session.user.restriction.expiresAt ? '截止 ' + session.user.restriction.expiresAt.replace('T', ' ').slice(0, 16) : '长期限制' }}</view>
      </wd-card>
      <wd-card custom-class="profile-card">
        <template #title>
          <view class="profile-head">
            <view class="profile-avatar"><UserAvatar :src="session.user?.avatarUrl" :size="56" /></view>
            <view class="profile-meta">
              <text class="profile-name">{{ session.user?.nickname || '梦想家' }}</text>
              <text class="profile-bio" :class="{ 'is-empty': !session.user?.bio }">{{ session.user?.bio || '写一句话介绍自己吧' }}</text>
            </view>
            <wd-button size="small" plain @click="edit">编辑资料</wd-button>
          </view>
        </template>
        <view class="profile-stats">
          <view class="profile-stat"><view class="stat-num"><AnimatedNumber :value="total" /></view><view class="muted">颗梦想</view></view>
          <view class="profile-stat"><view class="stat-num">{{ session.user?.commentQualification?.passed ? '✓' : '—' }}</view><view class="muted">评论资格</view></view>
          <view class="profile-stat"><view class="stat-num">{{ session.user?.city || '—' }}</view><view class="muted">所在城市</view></view>
        </view>
      </wd-card>
      <wd-cell-group border class="section"><wd-cell title="我愿意提供的帮助" :label="session.user?.skills || '未填写'" is-link @click="edit" /><wd-cell title="我的帮助与交流" is-link @click="go('/pages/inbox/index')" /><wd-cell title="查看公开名片" label="不展示匿名梦想" is-link @click="go('/pages/profile/index?id=' + session.user.id)" /></wd-cell-group>
      <view class="row between section"><view class="subheading">我的梦想 · {{ total }}</view><wd-button type="text" @click="go('/pages/publish/index')">发布梦想</wd-button></view>
      <wd-status-tip url-prefix="/static/status/" v-if="error" image="network" :tip="error"><template #bottom><wd-button @click="load(true)">重试</wd-button></template></wd-status-tip>
      <DreamSkeleton v-if="loading && !dreams.length" :count="1" />
      <view class="stagger"><DreamCard v-for="dream in dreams" :key="dream.id" :dream="dream" /></view>
      <wd-status-tip url-prefix="/static/status/" v-if="!loading && !error && !dreams.length" image="content" tip="暂无梦想" />
      <wd-button v-if="hasMore" block plain :loading="loading" @click="load(false)">更多梦想</wd-button>
    </template>
    <wd-cell-group v-if="session.token" border class="actions">
      <wd-cell title="评论资格" :value="session.user?.commentQualification?.passed ? '已通过' : '去答题'" is-link @click="go('/pages/admission/index')" />
      <wd-cell title="意见反馈" value="帮我们变得更好" is-link @click="go('/pages/feedback/index')" />
    </wd-cell-group>
    <view class="legal-section actions">
      <view class="legal-heading">协议与说明</view>
      <view v-for="item in legalLinks" :key="item.url" class="legal-row" hover-class="is-pressed" @click="go(item.url)">
        <view class="legal-icon" :class="item.tone"><wd-icon :name="item.icon" size="18px" color="#fff" /></view>
        <text class="legal-title">{{ item.title }}</text>
        <wd-icon name="arrow-right" size="16px" color="#B4AFCB" />
      </view>
    </view>
    <view v-if="session.token" class="logout-wrap"><wd-button block plain custom-class="logout-btn" @click="logout">退出登录</wd-button></view>
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
import DreamSkeleton from '../../components/DreamSkeleton.vue'
import GuestPrompt from '../../components/GuestPrompt.vue'
import AnimatedNumber from '../../components/AnimatedNumber.vue'
import { request, session, clearSession, go, notify } from '../../lib/api'
const dreams = ref([]), total = ref(0), editing = ref(false), saving = ref(false), loading = ref(false), hasMore = ref(false), error = ref('')
const profile = reactive({ nickname: '', bio: '', city: '', skills: '' }); let page = 1
const avatar = ref({ id: '', url: '' }), avatarBlocked = ref(false)
const legalLinks = [
  { title: '免责声明与线下交流风险告知', icon: 'warning', tone: 'is-amber', url: '/pages/legal/notice' },
  { title: '隐私政策', icon: 'lock-on', tone: 'is-violet', url: '/pages/legal/privacy' }
]
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
<style scoped>
/* 个人卡片：顶部星夜渐变，头像带旋转光环。 */
:deep(.profile-card) { background: linear-gradient(180deg, rgba(236, 229, 255, .42) 0%, rgba(255, 255, 255, .2) 70%) !important; }
:deep(.profile-card)::after { content: ''; position: absolute; top: -50px; right: -40px; width: 150px; height: 150px; border-radius: 50%; background: radial-gradient(circle, rgba(255, 141, 184, .35), rgba(255, 141, 184, 0) 70%); animation: dream-float 6s ease-in-out infinite; pointer-events: none; }
.profile-avatar { position: relative; padding: 3px; border-radius: 50%; background: conic-gradient(#6E56F8, #FF8DB8, #FFB84D, #6E56F8); animation: dream-pop .6s var(--dream-spring) backwards; }
.profile-avatar :deep(.user-avatar) { background: #fff; border-radius: 50%; }
/* 资料卡头部：头像、昵称与简介、编辑按钮同一行，简介最多两行。 */
.profile-head { display: flex; align-items: center; gap: 12px; }
.profile-avatar { flex-shrink: 0; }
.profile-meta { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.profile-name { font-size: 19px; font-weight: 700; line-height: 1.4; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.profile-bio { margin-top: 2px; font-size: 13px; font-weight: 400; line-height: 1.6; color: var(--dream-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.profile-bio.is-empty { color: #B4AFCB; }
.profile-stats { display: flex; padding: 12px 0; border-radius: 16px; background: rgba(255, 255, 255, .28); box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .6); }
.profile-stat { flex: 1; min-width: 0; text-align: center; }
.profile-stat + .profile-stat { border-left: 1px solid var(--dream-line); }
/* 协议入口：整行展示，标题不再被压缩换行；按下轻微下沉。 */
.legal-section { padding: 6px 0; border-radius: 18px; background: rgba(255, 255, 255, .3); border: 1px solid var(--dream-glass-line); }
.legal-heading { padding: 10px 16px 4px; font-size: 13px; font-weight: 650; color: var(--dream-muted); letter-spacing: 1px; }
.legal-row { display: flex; align-items: center; gap: 12px; padding: 12px 16px; transition: background-color .25s ease, transform .3s var(--dream-spring); }
.legal-row + .legal-row { border-top: 1px solid rgba(110, 86, 248, .08); }
.legal-row.is-pressed { background: rgba(110, 86, 248, .08); transform: scale(.98); }
.legal-icon { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 36px; height: 36px; border-radius: 12px; }
.legal-icon.is-amber { background: linear-gradient(135deg, #FFC66B, #FF9E5E); box-shadow: 0 6px 14px rgba(255, 158, 94, .3); }
.legal-icon.is-violet { background: var(--dream-gradient); box-shadow: 0 6px 14px rgba(110, 86, 248, .3); }
.legal-title { flex: 1; min-width: 0; font-size: 15px; font-weight: 600; color: var(--dream-ink); }
/* 退出登录放在页面最底部，柔和的红色文字区分于其他操作。 */
.logout-wrap { margin-top: 24px; }
.logout-wrap :deep(.logout-btn) { color: #E5546A !important; border-color: rgba(229, 84, 106, .3) !important; background: rgba(255, 255, 255, .35) !important; }
.stat-num { font-size: 18px; font-weight: 700; color: var(--dream-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding: 0 6px; }
</style>
