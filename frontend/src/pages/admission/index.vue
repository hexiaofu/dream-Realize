<template>
  <AppShell title="评论资格问答" back>
    <wd-button v-if="!session.token" block @click="requireLogin">登录后答题</wd-button>
    <DreamLoading v-else-if="loading" text="正在准备题目" />
    <wd-status-tip v-else-if="error" url-prefix="/static/status/" image="network" :tip="error"><template #bottom><wd-button @click="load">重试</wd-button></template></wd-status-tip>
    <wd-card v-else-if="qualification?.passed" title="已获得评论资格" custom-class="pass-card">
      <view class="pass-medal"><view class="medal-ring" /><wd-icon name="check" size="34px" color="#fff" /><view v-for="n in 8" :key="n" class="medal-spark" :style="'--i:' + n">✦</view></view>
      <view class="center"><wd-text :text="qualification.score + ' 分'" type="success" size="28px" bold /></view>
      <view class="actions"><wd-button block @click="back">{{ dreamId ? '返回评论' : '返回' }}</wd-button></view>
    </wd-card>
    <template v-else-if="exam">
      <view class="exam-progress section"><view class="exam-progress-bar" :style="{ width: answeredCount / exam.total * 100 + '%' }" /></view>
      <view class="subheading section">{{ exam.total }} 题 · 每题 {{ exam.pointsPerQuestion }} 分 · {{ exam.passScore }} 分通过</view>
      <wd-card v-if="score !== null" title="本次成绩" custom-class="score-card"><wd-text :text="score + ' 分，未通过'" type="warning" bold /></wd-card>
      <wd-card v-for="(question, index) in exam.questions" :key="question.id" :title="`${index + 1}. ${question.title}`" :custom-class="'question-card' + (answers[question.id] !== undefined ? ' is-answered' : '') + (feedback[question.id] ? (feedback[question.id].correct ? ' is-correct' : ' is-wrong') : '')">
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
import DreamLoading from '../../components/DreamLoading.vue'
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppShell from '../../components/AppShell.vue'
import { request, session, requireLogin, notify } from '../../lib/api'
const exam = ref(null), qualification = ref(null), loading = ref(false), busy = ref(false), error = ref(''), score = ref(null)
const answers = reactive({}), feedback = ref({}), dreamId = ref('')
const answeredCount = computed(() => exam.value ? exam.value.questions.filter(question => answers[question.id] !== undefined).length : 0)
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
<style scoped>
/* 答题：顶部进度条随作答增长；答错的题轻轻摇晃；通过后勋章弹出并散开星芒。 */
.exam-progress { position: sticky; top: 0; z-index: 5; height: 6px; border-radius: 6px; background: var(--dream-line); overflow: hidden; }
.exam-progress-bar { height: 100%; border-radius: 6px; background: var(--dream-gradient); transition: width .6s var(--dream-spring); }
:deep(.question-card) { transition: border-color .3s ease, transform .35s var(--dream-ease), box-shadow .35s ease; }
:deep(.question-card.is-answered) { border-color: rgba(110, 86, 248, .25); }
:deep(.question-card.is-correct) { border-color: rgba(32, 180, 134, .45); }
:deep(.question-card.is-wrong) { border-color: rgba(255, 184, 77, .6); animation: shake .5s ease; }
@keyframes shake { 0%, 100% { transform: translateX(0); } 20% { transform: translateX(-6px); } 40% { transform: translateX(5px); } 60% { transform: translateX(-3px); } 80% { transform: translateX(2px); } }
:deep(.score-card) { animation: dream-pop .5s var(--dream-spring) backwards; }
.pass-medal { position: relative; display: flex; align-items: center; justify-content: center; width: 84px; height: 84px; margin: 6px auto 14px; border-radius: 50%;
  background: linear-gradient(135deg, #2DD4A0, #20B486); box-shadow: 0 14px 30px rgba(32, 180, 134, .35); animation: dream-pop .7s var(--dream-spring) backwards; }
.medal-ring { position: absolute; top: -8px; right: -8px; bottom: -8px; left: -8px; border-radius: 50%; border: 2px dashed rgba(32, 180, 134, .4); animation: dream-spin 8s linear infinite; }
.medal-spark { position: absolute; color: #FFB84D; font-size: 14px; animation: medal-spark 1.2s var(--dream-ease) .3s both; --a: calc(var(--i) * 45deg); }
@keyframes medal-spark { 0% { opacity: 0; transform: rotate(var(--a)) translateY(0) scale(.4); } 30% { opacity: 1; } 100% { opacity: 0; transform: rotate(var(--a)) translateY(-70px) scale(1); } }
</style>
