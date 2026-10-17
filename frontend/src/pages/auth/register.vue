<template>
  <AuthLayout title="创建账号" alternative-hint="已有账号？" alternative-label="去登录" alternative-url="/pages/auth/index" :busy="busy">
    <wd-cell-group insert>
      <wd-input v-model="form.username" label="账号" prefix-icon="user" placeholder="4–32 位账号" clearable :maxlength="32" :disabled="busy" confirm-type="next" />
    </wd-cell-group>
    <view class="space" />
    <wd-cell-group insert>
      <wd-input v-model="form.password" label="密码" prefix-icon="lock-on" placeholder="8–64 位密码" show-password :maxlength="64" :disabled="busy" confirm-type="next" />
    </wd-cell-group>
    <view class="space" />
    <wd-cell-group insert>
      <wd-input v-model="form.nickname" label="昵称" prefix-icon="edit-outline" placeholder="怎么称呼你" clearable :maxlength="30" :disabled="busy" />
    </wd-cell-group>
    <view class="actions"><RegistrationNotice ref="noticePanel" v-model="noticeAccepted" show-consent :disabled="busy" @loaded="notice = $event" /></view>
    <view class="actions"><wd-button block size="large" :round="false" :loading="busy" :disabled="!notice || !noticeAccepted" @click="register">注册账号</wd-button></view>
  </AuthLayout>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import AuthLayout from '../../components/AuthLayout.vue'
import RegistrationNotice from '../../components/RegistrationNotice.vue'
import { request, setSession, notify } from '../../lib/api'
const busy = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })
const notice = ref(null), noticeAccepted = ref(false), noticePanel = ref(null)
async function register() {
  if (busy.value) return
  if (!notice.value || !noticeAccepted.value) return notify('请阅读并同意注册声明')
  if (!/^[A-Za-z0-9_]{4,32}$/.test(form.username.trim())) return notify('账号需为 4–32 位字母、数字或下划线')
  if (form.password.length < 8) return notify('密码需为 8–64 位')
  if (!form.nickname.trim()) return notify('请填写昵称')
  busy.value = true
  try {
    const data = await request('/auth/register', 'POST', {
      username: form.username.trim(), password: form.password, nickname: form.nickname.trim(),
      noticeAccepted: noticeAccepted.value, noticeVersion: notice.value.version, noticeSha256: notice.value.sha256
    })
    setSession(data)
    if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' })
  } catch(e) { if (e.status === 412) await noticePanel.value?.reload(); notify(e) } finally { busy.value = false }
}
</script>
