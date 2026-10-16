<template>
  <AppShell title="加入圆梦" back>
    <wd-segmented v-model:value="mode" :options="['登录', '加入社区']" />
    <view class="space" />
    <template v-if="mode === '加入社区' && !admissionToken">
      <wd-status-tip url-prefix="/static/status/" v-if="examError" image="network" :tip="examError"><template #bottom><wd-button @click="loadQuestions">重试</wd-button></template></wd-status-tip>
      <wd-card v-for="(question, index) in questions" :key="question.id" :title="`${index + 1}. ${question.title}`">
        <wd-radio-group v-model="answers[question.id]" shape="button">
          <view v-for="(option, optionIndex) in question.options" :key="optionIndex" class="section"><wd-radio :value="optionIndex">{{ option }}</wd-radio></view>
        </wd-radio-group>
        <view v-if="feedback[question.id]" class="muted"><wd-tag :type="feedback[question.id].correct ? 'success' : 'warning'">{{ feedback[question.id].correct ? '理解一致' : '再想一想' }}</wd-tag> {{ feedback[question.id].explanation }}</view>
      </wd-card>
      <wd-button block :loading="busy" :disabled="!questions.length" @click="checkExam">完成入门问答</wd-button>
    </template>
    <template v-else>
      <wd-cell-group border>
        <wd-input v-model="form.username" label="账号" placeholder="4–32 位字母、数字或下划线" clearable :maxlength="32" />
        <wd-input v-model="form.password" label="密码" placeholder="8–64 位密码" show-password :maxlength="64" />
        <wd-input v-if="mode === '加入社区'" v-model="form.nickname" label="昵称" placeholder="请输入昵称" :maxlength="30" />
      </wd-cell-group>
      <view class="actions"><wd-button block size="large" :loading="busy" @click="authenticate">{{ mode === '登录' ? '登录圆梦' : '成为社区成员' }}</wd-button></view>
      <view v-if="mode === '加入社区'" class="center actions"><wd-button type="text" @click="admissionToken = ''">重新完成入门问答</wd-button></view>
    </template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref, watch } from 'vue'
import AppShell from '../../components/AppShell.vue'
import { request, setSession, notify } from '../../lib/api'
const mode = ref('登录'), busy = ref(false), questions = ref([]), answers = reactive({}), feedback = ref({}), admissionToken = ref(''), examError = ref('')
const form = reactive({ username: '', password: '', nickname: '' })
async function loadQuestions() { try { examError.value = ''; questions.value = (await request('/admission/questions')).questions } catch(e) { examError.value = e.message } }
watch(mode, value => { if (value === '加入社区' && !questions.value.length) loadQuestions() })
async function checkExam() {
  if (busy.value) return
  if (questions.value.some(q => answers[q.id] === undefined)) return notify('请先完成全部 8 道题目')
  busy.value = true
  try {
    const result = await request('/admission/check', 'POST', { answers })
    feedback.value = Object.fromEntries(result.feedback.map(x => [x.id, x]))
    if (result.passed) { admissionToken.value = result.admissionToken; uni.pageScrollTo({ scrollTop: 0 }) }
    else notify('还有一些共识需要了解，看看题目下方的解释再试一次')
  } catch(e) { notify(e) } finally { busy.value = false }
}
async function authenticate() {
  if (busy.value) return
  if (!form.username || !form.password || (mode.value === '加入社区' && !form.nickname.trim())) return notify('请填写完整的账号信息')
  busy.value = true
  try {
    const data = await request(mode.value === '登录' ? '/auth/login' : '/auth/register', 'POST', { ...form, admissionToken: admissionToken.value })
    setSession(data)
    if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/discover/index' })
  } catch(e) { notify(e) } finally { busy.value = false }
}
</script>
