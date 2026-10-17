<template>
  <AppShell title="评论资格问答" back>
    <wd-button v-if="!session.token" block @click="requireLogin">登录后答题</wd-button>
    <view v-else-if="loading" class="center"><wd-loading /></view>
    <wd-status-tip v-else-if="error" url-prefix="/static/status/" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <wd-card v-else-if="qualification?.passed" title="已获得评论资格">
      <wd-text :text="qualification.score + ' 分'" type="success" size="28px" bold />
      <view class="actions"><wd-button block @click="back">{{ dreamId ? '返回评论' : '返回' }}</wd-button></view>
    </wd-card>
    <template v-else-if="exam">
      <view class="subheading section">{{ exam.total }} 题 · 每题 {{ exam.pointsPerQuestion }} 分 · {{ exam.passScore }} 分通过</view>
      <wd-card v-if="score !== null" title="本次成绩"><wd-text :text="score + ' 分，未通过'" type="warning" bold /></wd-card>
      <wd-card v-for="(question, index) in exam.questions" :key="question.id" :title="`${index + 1}. ${question.title}`">
        <wd-radio-group v-model="answers[question.id]" shape="dot" cell icon-placement="right" :disabled="busy">
          <wd-radio v-for="(option, optionIndex) in question.options" :key="optionIndex" :value="optionIndex" max-width="calc(100% - 32px)" custom-style="text-align: left;">{{ option }}</wd-radio>
        </wd-radio-group>
        <view v-if="feedback[question.id]" class="muted section"><wd-tag :type="feedback[question.id].correct ? 'success' : 'warning'">{{ feedback[question.id].correct ? '回答正确' : '再想一想' }}</wd-tag> {{ feedback[question.id].explanation }}</view>
      </wd-card>
      <wd-button block :loading="busy" @click="submit">{{ score === null ? '提交答案' : '重新提交' }}</wd-button>
    </template>
  </AppShell>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import { request, session, requireLogin, notify } from '../../lib/api'
const exam = ref(null), qualification = ref(null), loading = ref(false), busy = ref(false), error = ref(''), score = ref(null)
const answers = reactive({}), feedback = ref({}), dreamId = ref('')
function updateQualification(value) {
  qualification.value = value
  if (session.user) { session.user.commentQualification = value; uni.setStorageSync('dream-user', session.user) }
}
async function load() {
  if (!session.token || loading.value) return
  loading.value = true; error.value = ''
  try {
    const [questions, status] = await Promise.all([request('/admission/questions'), request('/admission/status')])
    if (exam.value?.version !== questions.version) { Object.keys(answers).forEach(key => delete answers[key]); feedback.value = {}; score.value = null }
    exam.value = questions; updateQualification(status)
  } catch(e) { error.value = e.message } finally { loading.value = false }
}
async function submit() {
  if (busy.value || !exam.value || !requireLogin()) return
  if (exam.value.questions.some(question => answers[question.id] === undefined)) return notify('请完成全部 ' + exam.value.total + ' 道题目')
  busy.value = true
  try {
    const result = await request('/admission/check', 'POST', { answers, examVersion: exam.value.version })
    score.value = result.score; feedback.value = Object.fromEntries(result.feedback.map(item => [item.id, item]))
    updateQualification(result.qualification); uni.pageScrollTo({ scrollTop: 0 })
  } catch(e) { if (e.status === 412) await load(); notify(e) } finally { busy.value = false }
}
function back() {
  if (getCurrentPages().length > 1) uni.navigateBack()
  else if (dreamId.value) uni.redirectTo({ url: '/pages/dream/detail?id=' + dreamId.value + '&comments=1' })
  else uni.reLaunch({ url: '/pages/me/index' })
}
onLoad(options => { if (/^\d+$/.test(options.dreamId || '')) dreamId.value = options.dreamId })
onShow(load)
</script>
