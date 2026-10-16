<template>
  <AppShell active="publish" title="写下我的梦想">
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
    <view class="actions"><wd-button block size="large" :loading="saving" :disabled="mediaBlocked" @click="submit">{{ mediaBlocked ? '请先完成附件上传' : session.token ? '发布我的梦想' : '加入社区，发布梦想' }}</wd-button></view>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref, watch } from 'vue'
import AppShell from '../../components/AppShell.vue'
import MediaUploader from '../../components/MediaUploader.vue'
import { request, session, requireLogin, notify, categories, helpTypes, rewardTypes, contactModes } from '../../lib/api'
const formRef = ref(), saving = ref(false), mediaBlocked = ref(false)
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
    uni.redirectTo({ url: '/pages/dream/detail?id=' + dream.id })
  } catch(e) { notify(e) } finally { saving.value = false }
}
</script>
