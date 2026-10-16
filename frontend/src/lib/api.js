/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive } from 'vue'

export const session = reactive({ token: uni.getStorageSync('dream-token') || '', user: uni.getStorageSync('dream-user') || null })
let base = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:18080/api'
// #ifdef H5
base = import.meta.env.VITE_API_BASE_URL || '/api'
// #endif

export function setSession(data) {
  session.token = data.token; session.user = data.user
  uni.setStorageSync('dream-token', data.token); uni.setStorageSync('dream-user', data.user)
}
export function clearSession() {
  session.token = ''; session.user = null
  uni.removeStorageSync('dream-token'); uni.removeStorageSync('dream-user')
  uni.removeStorageSync('dream-draft')
}
export function request(path, method = 'GET', data = undefined) {
  return new Promise((resolve, reject) => {
    uni.request({ url: base.replace(/\/$/, '') + path, method, data, timeout: 15000,
      header: { 'Content-Type': 'application/json', ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}) },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.data?.code === 0) return resolve(res.data.data)
        if (res.statusCode === 401) clearSession()
        reject(new Error(res.data?.message || '请求未成功，请稍后重试'))
      }, fail() { reject(new Error('暂时连接不到服务，请检查网络后重试')) }
    })
  })
}
// 服务端只返回 /api/media/ 下的相对路径，适配 H5 代理与真机接口域名。
export function mediaUrl(path) {
  return path?.startsWith('/api/media/') ? base.replace(/\/$/, '') + path.slice(4) : ''
}
export function uploadMedia(file, kind, handlers) {
  const token = session.token
  const task = uni.uploadFile({
    url: base.replace(/\/$/, '') + '/media', filePath: file.url, name: 'file',
    header: { Authorization: `Bearer ${token}` }, formData: { kind }, timeout: 180000,
    success(res) {
      let body
      try { body = JSON.parse(res.data) } catch { /* 非 JSON 网关错误使用统一提示。 */ }
      if (res.statusCode === 200 && body?.code === 0) return handlers.success(body.data)
      if (res.statusCode === 401 && session.token === token) clearSession()
      handlers.error(new Error(body?.message || (res.statusCode === 413 ? '文件过大，请压缩后上传' : '上传失败，请重试')))
    },
    fail() { handlers.error(new Error('上传中断，请检查网络后重试')) }
  })
  task.onProgressUpdate?.(handlers.progress)
  return task
}
export function query(values) { return Object.entries(values).filter(([, v]) => v !== '' && v !== undefined).map(([k,v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&') }
export function notify(error) { uni.showToast({ title: error?.message || String(error), icon: 'none', duration: 2500 }) }
export function requireLogin() {
  if (session.token) return true
  uni.navigateTo({ url: '/pages/auth/index' }); return false
}
export function go(url) { uni.navigateTo({ url }) }
export const categories = [{ value: '', label: '全部' }, { value: 'LIFE', label: '生活体验' }, { value: 'GROWTH', label: '学习成长' }, { value: 'FAMILY', label: '家庭心愿' }, { value: 'CREATION', label: '创作表达' }, { value: 'CAREER', label: '事业梦想' }, { value: 'OTHER', label: '其他心愿' }]
export const helpTypes = [{ value: 'EXPERIENCE', label: '经验建议' }, { value: 'SKILL', label: '技能帮助' }, { value: 'CASH', label: '现金帮助' }, { value: 'GOODS', label: '物品支持' }, { value: 'COMPANY', label: '陪伴同行' }, { value: 'RESOURCE', label: '资源对接' }, { value: 'OTHER', label: '其他帮助' }]
export const statuses = [{ value: 'OPEN', label: '等待相遇' }, { value: 'IN_PROGRESS', label: '正在圆梦' }, { value: 'COMPLETED', label: '已经圆梦' }, { value: 'PAUSED', label: '暂时停一停' }]
export const rewardTypes = [{ value: 'FREE', label: '无偿帮助' }, { value: 'NEGOTIABLE', label: '条件可协商' }]
export const contactModes = [{ value: 'BOTH', label: '线上线下均可' }, { value: 'ONLINE', label: '线上交流' }, { value: 'OFFLINE', label: '线下见面' }]
export const label = (list, value) => list.find(x => x.value === value)?.label || value
export const date = value => value ? value.replace('T', ' ').slice(0, 16) : ''
export const helpStatus = value => ({ PENDING: '等待回应', ACCEPTED: '已接受帮助', DECLINED: '已婉拒', WITHDRAWN: '已撤回' }[value] || value)
