<template>
  <AppShell active="publish" title="写下我的梦想">
    <view class="publish-intro"><view class="intro-lantern"><view class="lantern-flame" /></view><view class="grow"><view class="intro-title">许下一个心愿</view><view class="muted">写得越真诚，越容易遇见愿意同行的人</view></view></view>
    <wd-form ref="formRef" :model="form" :rules="rules">
      <wd-cell-group border title="我的梦想">
        <wd-input v-model="form.title" label="一句话心愿" prop="title" placeholder="我想……" :maxlength="100" clearable required />
        <wd-textarea v-model="form.story" label="梦想描述" placeholder="补充说明（选填）" :maxlength="5000" show-word-limit auto-height />
        <wd-select-picker type="radio" :show-confirm="false" v-model="form.category" label="梦想分类" :columns="categories.filter(x => x.value)" />
      </wd-cell-group>
      <view class="space" />
      <MediaUploader v-model="form.media" :disabled="saving" @blocked="mediaBlocked = $event" />
      <view class="space" />
      <wd-cell-group border title="我期待的帮助">
        <wd-select-picker type="radio" :show-confirm="false" v-model="form.helpType" label="帮助类型" :columns="helpTypes" />
        <wd-input v-if="form.helpType === 'CASH'" v-model="form.amount" label="期望金额" type="digit" placeholder="选填，单位：元" />
        <wd-select-picker type="radio" :show-confirm="false" v-model="form.rewardType" label="帮助条件" :columns="rewardTypes" />
        <wd-select-picker type="radio" :show-confirm="false" v-model="form.contactMode" label="交流方式" :columns="contactModes" />
        <wd-input v-model="form.city" label="所在城市" placeholder="选填" :maxlength="60" clearable />
      </wd-cell-group>
      <view class="space" />
      <wd-cell-group border title="发布身份">
        <wd-cell title="匿名发布" label="详情与交流均匿名"><wd-switch v-model="form.anonymous" size="22px" /></wd-cell>
        <wd-cell title="对外展示为" :value="form.anonymous ? '匿名梦想家' : session.user?.nickname || '你的昵称'" />
      </wd-cell-group>
    </wd-form>
    <view class="actions publish-submit"><wd-button block size="large" :loading="saving" :disabled="mediaBlocked" @click="submit">{{ mediaBlocked ? '请先完成附件上传' : session.token ? '发布我的梦想' : '加入社区，发布梦想' }}</wd-button></view>
    <view v-if="released" class="release-overlay">
      <view class="release-lantern"><view class="lantern-flame" /></view>
      <view v-for="n in 10" :key="n" class="release-spark" :style="'--i:' + n">✦</view>
      <view class="release-text">梦想已经放飞</view>
      <view class="release-sub">愿它被温柔地看见</view>
    </view>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref, watch } from 'vue'
import AppShell from '../../components/AppShell.vue'
import MediaUploader from '../../components/MediaUploader.vue'
import { request, session, requireLogin, notify, categories, helpTypes, rewardTypes, contactModes } from '../../lib/api'
const formRef = ref(), saving = ref(false), mediaBlocked = ref(false), released = ref(false)
const draft = uni.getStorageSync('dream-draft') || {}
const form = reactive({ title: '', story: '', category: 'LIFE', helpType: 'EXPERIENCE', rewardType: 'FREE', contactMode: 'BOTH', city: '', anonymous: false, amount: '', media: [], ...draft })
const rules = { title: [{ required: true, message: '请先写下一句话心愿' }] }
watch(form, () => uni.setStorageSync('dream-draft', { ...form }), { deep: true })
async function submit() {
  if (!requireLogin() || saving.value || mediaBlocked.value) return
  const { valid } = await formRef.value.validate(); if (!valid) return
  if (form.helpType === 'CASH' && form.amount && (!/^\d+(\.\d{1,2})?$/.test(form.amount) || Number(form.amount) <= 0)) return notify('请填写大于零、最多两位小数的金额')
  saving.value = true
  try {
    const { amount, media, ...body } = form
    const dream = await request('/dreams', 'POST', { ...body, mediaIds: media.map(file => file.id), targetAmount: form.helpType === 'CASH' && amount ? Number(amount) : null })
    Object.assign(form, { title: '', story: '', amount: '', anonymous: false, media: [] }); uni.removeStorageSync('dream-draft')
    released.value = true
    setTimeout(() => uni.redirectTo({ url: '/pages/dream/detail?id=' + dream.id }), 1100)
  } catch(e) { notify(e) } finally { saving.value = false }
}
</script>
<style scoped>
/* 发布页：顶部小孔明灯摇曳；发布成功后孔明灯升空、星芒散开，再进入详情。 */
.publish-intro { display: flex; align-items: center; gap: 14px; padding: 16px 18px; margin-bottom: 16px; border-radius: 20px; background: var(--dream-glass); border: 1px solid var(--dream-glass-line); backdrop-filter: var(--dream-glass-blur); -webkit-backdrop-filter: var(--dream-glass-blur); box-shadow: 0 10px 26px rgba(84, 62, 190, .08); animation: dream-rise .6s var(--dream-ease) backwards; }
.intro-title { font-size: 17px; font-weight: 700; }
.intro-lantern, .release-lantern { position: relative; flex-shrink: 0; width: 38px; height: 46px; border-radius: 14px 14px 10px 10px; background: linear-gradient(180deg, #FFD58A, #FF9E5E); box-shadow: 0 0 22px rgba(255, 184, 77, .6); animation: lantern-sway 3s ease-in-out infinite; transform-origin: 50% 0; }
.lantern-flame { position: absolute; left: 50%; bottom: 8px; width: 10px; height: 14px; margin-left: -5px; border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%; background: #FFF6DD; box-shadow: 0 0 12px #FFF2C6; animation: dream-breathe .9s ease-in-out infinite; }
@keyframes lantern-sway { 0%, 100% { transform: rotate(-5deg); } 50% { transform: rotate(5deg); } }
:deep(.wd-cell-group__title) { font-weight: 650; color: var(--dream-ink) !important; }
.publish-submit { position: relative; }
.release-overlay { position: fixed; z-index: 200; top: 0; right: 0; bottom: 0; left: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff;
  background: radial-gradient(circle at 50% 40%, rgba(142, 98, 255, .92), rgba(36, 23, 94, .96)); animation: overlay-in .35s ease backwards; }
.release-lantern { width: 64px; height: 78px; border-radius: 22px 22px 16px 16px; animation: lantern-rise 1.1s var(--dream-ease) both; }
.release-lantern .lantern-flame { width: 16px; height: 22px; margin-left: -8px; bottom: 12px; }
.release-spark { position: absolute; top: 46%; left: 50%; color: #FFD58A; font-size: 14px; animation: spark-out 1s var(--dream-ease) both; animation-delay: calc(var(--i) * 30ms);
  --a: calc(var(--i) * 36deg); }
.release-text { margin-top: 26px; font-size: 22px; font-weight: 700; letter-spacing: 4px; animation: dream-rise .6s var(--dream-ease) .2s backwards; }
.release-sub { margin-top: 6px; font-size: 13px; opacity: .8; animation: dream-rise .6s var(--dream-ease) .35s backwards; }
@keyframes overlay-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes lantern-rise { 0% { transform: translateY(80px) scale(.6); opacity: 0; } 40% { opacity: 1; } 100% { transform: translateY(-40px) scale(1); } }
@keyframes spark-out { 0% { opacity: 0; transform: rotate(var(--a)) translateY(0) scale(.4); } 30% { opacity: 1; } 100% { opacity: 0; transform: rotate(var(--a)) translateY(-120px) scale(1); } }
</style>
