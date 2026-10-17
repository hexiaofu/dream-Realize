<template>
  <AuthLayout title="登录圆梦" alternative-hint="还没有账号？" alternative-label="注册账号" alternative-url="/pages/auth/register" :busy="busy">
    <wd-cell-group insert>
      <wd-input v-model="form.username" label="账号" prefix-icon="user" placeholder="请输入账号" clearable :maxlength="32" :disabled="busy" confirm-type="next" />
    </wd-cell-group>
    <view class="space" />
    <wd-cell-group insert>
      <wd-input v-model="form.password" label="密码" prefix-icon="lock-on" placeholder="请输入密码" show-password :maxlength="64" :disabled="busy" confirm-type="go" @confirm="authenticate" />
    </wd-cell-group>
    <view class="actions"><wd-button block size="large" :round="false" :loading="busy" @click="authenticate">登录圆梦</wd-button></view>
  </AuthLayout>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import AuthLayout from '../../components/AuthLayout.vue'
import { request, setSession, notify } from '../../lib/api'
const busy = ref(false)
const form = reactive({ username: '', password: '' })
async function authenticate() {
  if (busy.value) return
  if (!form.username.trim() || !form.password) return notify('请填写账号和密码')
  busy.value = true
  try {
    const data = await request('/auth/login', 'POST', { username: form.username.trim(), password: form.password })
    setSession(data)
    if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' })
  } catch(e) { notify(e) } finally { busy.value = false }
}
</script>
