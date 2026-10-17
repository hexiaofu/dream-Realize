<template>
  <el-config-provider :locale="zhCn">
    <main v-if="checking" class="login" v-loading="checking" />
    <main v-else-if="!admin" class="login">
      <el-card class="login-card" shadow="never">
        <template #header><h2>圆梦管理后台</h2></template>
        <el-form label-position="top" @submit.prevent="login">
          <el-form-item label="管理员账号"><el-input v-model="credentials.username" autocomplete="username" maxlength="32" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="credentials.password" type="password" show-password autocomplete="current-password" maxlength="64" /></el-form-item>
          <el-button type="primary" native-type="submit" :loading="saving" class="full">登录后台</el-button>
        </el-form>
      </el-card>
    </main>
    <el-container v-else class="app">
      <el-aside width="180px" class="sidebar">
        <h2 class="brand">圆梦 · 后管</h2>
        <el-menu :default-active="section" @select="changeSection">
          <el-menu-item index="contents">内容管理</el-menu-item>
          <el-menu-item index="members">账号管理</el-menu-item>
          <el-menu-item index="logs">操作日志</el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="toolbar"><h2>{{ titles[section] }}</h2><el-space><el-tag type="info">{{ admin.nickname }}</el-tag><el-button text @click="logout">退出登录</el-button></el-space></el-header>
        <el-main>
          <el-row :gutter="16" class="summary">
            <el-col v-for="item in metrics" :key="item.key" :xs="12" :sm="6"><el-card shadow="never"><el-statistic :title="item.label" :value="overview[item.key] || 0" /></el-card></el-col>
          </el-row>
          <el-card shadow="never">
            <template #header>
              <el-form inline class="filters" @submit.prevent="search">
                <template v-if="section !== 'logs'">
                  <el-form-item v-if="section === 'contents'" label="内容"><el-radio-group v-model="filter.type" @change="search"><el-radio-button value="DREAM">梦想</el-radio-button><el-radio-button value="UPDATE">进展</el-radio-button><el-radio-button value="COMMENT">评论</el-radio-button></el-radio-group></el-form-item>
                  <el-form-item label="关键词"><el-input v-model="filter.keyword" placeholder="内容、账号或昵称" clearable maxlength="100" @clear="search" /></el-form-item>
                  <el-form-item label="状态"><el-select v-model="filter.state" style="width: 140px" @change="search"><el-option v-for="item in stateOptions" :key="item.value" v-bind="item" /></el-select></el-form-item>
                </template>
                <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button :loading="loading" @click="load">刷新</el-button></el-form-item>
              </el-form>
            </template>
            <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="summary" />
            <el-table v-if="section === 'contents'" v-loading="loading" :data="items" stripe empty-text="暂无内容" row-key="id">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="梦想" min-width="200" show-overflow-tooltip />
              <el-table-column v-if="filter.type !== 'DREAM'" prop="body" :label="filter.type === 'COMMENT' ? '评论' : '进展'" min-width="180" show-overflow-tooltip />
              <el-table-column label="发布者" min-width="150"><template #default="{ row }"><div>{{ row.nickname }}</div><el-text size="small" type="info">{{ row.username }}</el-text><el-tag v-if="row.anonymous" type="info" size="small">匿名发布</el-tag></template></el-table-column>
              <el-table-column label="状态" width="140"><template #default="{ row }"><el-tag :type="row.hidden ? 'danger' : row.parentHidden ? 'warning' : 'success'">{{ row.deleted ? '作者已删除' : row.hidden ? '已下架' : row.parentHidden ? '梦想已下架' : '可见' }}</el-tag></template></el-table-column>
              <el-table-column prop="reason" label="处理原因" min-width="160" show-overflow-tooltip />
              <el-table-column label="发布时间" width="160"><template #default="{ row }">{{ date(row.createdAt) }}</template></el-table-column>
              <el-table-column label="操作" width="180" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="inspect(row)">查看</el-button><el-button v-if="!row.deleted" link :type="row.hidden ? 'success' : 'danger'" @click="openContentAction(row)">{{ row.hidden ? '恢复' : '下架' }}</el-button></template></el-table-column>
            </el-table>
            <el-table v-else-if="section === 'members'" v-loading="loading" :data="items" stripe empty-text="暂无账号" row-key="id">
              <el-table-column prop="id" label="ID" width="80" /><el-table-column prop="username" label="账号" min-width="160" /><el-table-column prop="nickname" label="昵称" min-width="150" />
              <el-table-column label="状态" width="130"><template #default="{ row }"><el-tag :type="row.mode === 'NORMAL' ? 'success' : 'danger'">{{ row.admin ? '管理员' : modes[row.mode] }}</el-tag></template></el-table-column>
              <el-table-column label="限制截止" width="160"><template #default="{ row }">{{ row.mode === 'NORMAL' ? '—' : date(row.expiresAt) }}</template></el-table-column>
              <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="180" fixed="right"><template #default="{ row }"><el-button v-if="!row.admin" link type="primary" @click="openMemberAction(row)">设置限制</el-button><el-button v-if="!row.admin && row.mode !== 'NORMAL'" link type="success" @click="openMemberAction(row, true)">解除</el-button></template></el-table-column>
            </el-table>
            <el-table v-else v-loading="loading" :data="items" stripe empty-text="暂无操作记录" row-key="id">
              <el-table-column prop="id" label="ID" width="80" /><el-table-column prop="admin" label="管理员" min-width="130" />
              <el-table-column label="对象" width="120"><template #default="{ row }">{{ types[row.targetType] }} #{{ row.targetId }}</template></el-table-column>
              <el-table-column label="操作" width="130"><template #default="{ row }">{{ actions[row.action] }}</template></el-table-column>
              <el-table-column prop="reason" label="原因" min-width="220" /><el-table-column label="限制截止" width="160"><template #default="{ row }">{{ ['READ_ONLY', 'BLOCKED'].includes(row.action) ? date(row.expiresAt) : '—' }}</template></el-table-column>
              <el-table-column label="操作时间" width="160"><template #default="{ row }">{{ date(row.createdAt) }}</template></el-table-column>
            </el-table>
            <el-pagination class="pagination" v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" />
          </el-card>
        </el-main>
      </el-container>
    </el-container>
    <el-drawer v-model="detailOpen" title="内容详情" size="min(720px, 96vw)" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="梦想">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="发布者">{{ detail.nickname }}（{{ detail.username }} / #{{ detail.memberId }}）<el-tag v-if="detail.anonymous" type="info">匿名发布</el-tag></el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ date(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="内容状态">{{ detail.deleted ? '作者已删除' : detail.hidden ? '已下架' : detail.parentHidden ? '所属梦想已下架' : '可见' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.reason" label="处理原因">{{ detail.reason }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">{{ modes[detail.restriction.mode] }}</el-descriptions-item>
          </el-descriptions>
          <p class="content">{{ detail.body }}</p>
          <el-space wrap><el-image v-for="file in detail.media.filter(item => item.type === 'IMAGE')" :key="file.id" class="thumbnail" :src="mediaUrl(file.url)" fit="cover" :preview-src-list="detail.media.filter(item => item.type === 'IMAGE').map(item => mediaUrl(item.url))" :initial-index="detail.media.filter(item => item.type === 'IMAGE').indexOf(file)" preview-teleported /></el-space>
          <video v-for="file in detail.media.filter(item => item.type === 'VIDEO')" :key="file.id" :src="mediaUrl(file.url)" controls preload="metadata" class="video" />
          <el-divider />
          <el-space><el-button v-if="!detail.deleted" :type="detail.hidden ? 'success' : 'danger'" @click="openContentAction(detail)">{{ detail.hidden ? '恢复内容' : '下架内容' }}</el-button><el-button @click="findMember(detail.username)">管理该账号</el-button></el-space>
        </template>
      </div>
    </el-drawer>
    <el-dialog v-model="actionOpen" :title="actionTitle" width="min(480px, 94vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form label-position="top" @submit.prevent="saveAction">
        <el-form-item label="处理对象"><el-text>{{ target?.title || target?.username }}（#{{ target?.id }}）</el-text></el-form-item>
        <template v-if="actionKind === 'MEMBER' && action.mode !== 'NORMAL'">
          <el-form-item label="限制类型"><el-radio-group v-model="action.mode"><el-radio value="READ_ONLY">限制发布</el-radio><el-radio value="BLOCKED">封禁登录</el-radio></el-radio-group></el-form-item>
          <el-form-item label="时长"><el-select v-model="action.durationHours" class="full"><el-option label="1 天" :value="24" /><el-option label="7 天" :value="168" /><el-option label="30 天" :value="720" /><el-option label="长期" :value="0" /></el-select></el-form-item>
          <el-form-item><el-text size="small" type="info">{{ action.mode === 'READ_ONLY' ? '限制发布、上传、帮助、评论、表态、发消息和修改资料。' : '禁止登录，并使已有用户会话失效。' }}</el-text></el-form-item>
        </template>
        <el-form-item label="处理原因" required><el-input v-model="action.reason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写处理原因" /></el-form-item>
        <el-space><el-button :disabled="saving" @click="actionOpen = false">取消</el-button><el-button type="primary" native-type="submit" :loading="saving">确认处理</el-button></el-space>
      </el-form>
    </el-dialog>
  </el-config-provider>
</template>
<script setup>
/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { actions, date, mediaUrl, modes, query, request, types } from './api'
const checking = ref(true), admin = ref(null), section = ref('contents'), overview = ref({})
const credentials = reactive({ username: '', password: '' }), filter = reactive({ type: 'DREAM', keyword: '', state: 'ALL' })
const items = ref([]), total = ref(0), page = ref(1), loading = ref(false), saving = ref(false), error = ref('')
const detailOpen = ref(false), detailLoading = ref(false), detail = ref(null), actionOpen = ref(false), actionKind = ref('CONTENT'), target = ref(null)
const action = reactive({ mode: 'READ_ONLY', durationHours: 168, reason: '' })
const titles = { contents: '内容管理', members: '账号管理', logs: '操作日志' }
const metrics = [{ key: 'dreams', label: '梦想总数' }, { key: 'hidden', label: '已下架内容' }, { key: 'members', label: '社区成员' }, { key: 'restricted', label: '受限账号' }]
const stateOptions = computed(() => [{ label: '全部', value: 'ALL' }, ...(section.value === 'contents' ? [{ label: '可见', value: 'VISIBLE' }, { label: '不可见', value: 'HIDDEN' }] : Object.entries(modes).map(([value, label]) => ({ value, label })))])
const actionTitle = computed(() => actionKind.value === 'MEMBER' ? action.mode === 'NORMAL' ? '解除账号限制' : '设置账号限制' : target.value?.hidden ? '恢复内容' : '下架内容')
let sequence = 0, detailSequence = 0
function failed(e) {
  if (e.status === 401) { admin.value = null; detailOpen.value = false; actionOpen.value = false; items.value = []; sequence++; detailSequence++ }
  ElMessage.error(e.message || '网络连接失败')
}
async function login() {
  if (saving.value) return
  if (!credentials.username.trim() || !credentials.password) return ElMessage.warning('请输入账号和密码')
  saving.value = true
  try { admin.value = await request('/login', 'POST', credentials); credentials.password = ''; await load() } catch(e) { failed(e) } finally { saving.value = false }
}
async function logout() {
  try { await request('/logout', 'POST'); admin.value = null; items.value = []; detailOpen.value = false; actionOpen.value = false; sequence++; detailSequence++ } catch(e) { failed(e) }
}
async function load() {
  if (!admin.value) return
  const version = ++sequence; loading.value = true; error.value = ''
  const params = { page: page.value, size: 20, ...filter }
  try {
    const [data, stats] = await Promise.all([request('/' + section.value + query(params)), request('/overview')])
    if (version !== sequence) return
    items.value = data.items; total.value = data.total; overview.value = stats
  } catch(e) { if (version === sequence) { error.value = e.message; failed(e) } } finally { if (version === sequence) loading.value = false }
}
function search() { page.value = 1; load() }
function changeSection(value) { section.value = value; filter.keyword = ''; filter.state = 'ALL'; items.value = []; page.value = 1; load() }
async function inspect(row) {
  const version = ++detailSequence; detailOpen.value = true; detail.value = null; detailLoading.value = true
  try { const data = await request(`/contents/${row.type}/${row.id}`); if (version === detailSequence) detail.value = data } catch(e) { detailOpen.value = false; failed(e) } finally { if (version === detailSequence) detailLoading.value = false }
}
function openContentAction(row) { target.value = { ...row }; actionKind.value = 'CONTENT'; action.reason = ''; actionOpen.value = true }
function openMemberAction(row, restore = false) { target.value = { ...row }; actionKind.value = 'MEMBER'; Object.assign(action, { mode: restore ? 'NORMAL' : row.mode === 'NORMAL' ? 'READ_ONLY' : row.mode, durationHours: 168, reason: '' }); actionOpen.value = true }
function findMember(username) { detailOpen.value = false; section.value = 'members'; filter.state = 'ALL'; filter.keyword = username; page.value = 1; load() }
async function saveAction() {
  if (saving.value) return
  if (!action.reason.trim()) return ElMessage.warning('请填写处理原因')
  saving.value = true
  try {
    const row = target.value
    if (actionKind.value === 'MEMBER') await request(`/members/${row.id}/restriction`, 'PATCH', { mode: action.mode, durationHours: action.mode === 'NORMAL' || !action.durationHours ? null : action.durationHours, reason: action.reason, expectedRevision: row.revision })
    else {
      const updated = await request(`/contents/${row.type}/${row.id}`, 'PATCH', { hidden: !row.hidden, expectedHidden: row.hidden, reason: action.reason })
      if (detail.value?.id === row.id && detail.value?.type === row.type) detail.value = updated
    }
    actionOpen.value = false; ElMessage.success('处理成功'); await load()
  } catch(e) { failed(e); if (e.status === 409) { actionOpen.value = false; await load() } } finally { saving.value = false }
}
onMounted(async () => { try { admin.value = await request('/me'); await load() } catch(e) { if (e.status !== 401) failed(e) } finally { checking.value = false } })
</script>
<style>
/* Element Plus 负责控件外观；以下仅为布局与品牌色。 */
:root { --el-color-primary: #238478; --el-color-primary-light-3: #65a99f; --el-color-primary-light-5: #91c2bb; --el-color-primary-light-7: #bde0d9; --el-color-primary-light-9: #edf6f4; --el-color-primary-dark-2: #1c6a60; font-family: -apple-system, BlinkMacSystemFont, 'Microsoft YaHei', sans-serif; color: #263e39; background: #f5f7f6; }
body { margin: 0; } h2 { margin: 0; font-size: 20px; } .app { min-height: 100vh; }
.sidebar { background: white; border-right: 1px solid var(--el-border-color-light); } .brand { padding: 24px 20px; font-size: 18px; } .sidebar .el-menu { border-right: 0; }
.toolbar { background: white; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--el-border-color-light); }
.summary { margin-bottom: 20px; } .summary .el-col { margin-bottom: 12px; } .filters .el-form-item { margin-bottom: 8px; } .pagination { margin-top: 20px; justify-content: flex-end; }
.login { display: grid; place-items: center; min-height: 100vh; } .login-card { width: min(400px, 90vw); } .full { width: 100%; }
.content { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; } .thumbnail { width: 120px; height: 120px; border-radius: 8px; } .video { width: 100%; max-height: 400px; margin-top: 16px; }
@media (max-width: 700px) { .sidebar { width: 110px !important; } .brand { padding: 20px 10px; font-size: 15px; } .el-main { padding: 12px; } }
</style>
