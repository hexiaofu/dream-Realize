/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
export async function request(path, method = 'GET', data) {
  const response = await fetch('/api/admin' + path, {
    method, credentials: 'same-origin', cache: 'no-store',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Request': '1' },
    body: data === undefined ? undefined : JSON.stringify(data)
  })
  let body
  try { body = await response.json() } catch { throw new Error('服务响应异常，请稍后重试') }
  if (response.ok && body.code === 0) return body.data
  const error = new Error(body.message || '请求失败'); error.status = response.status; throw error
}
export const query = values => '?' + new URLSearchParams(values).toString()
export const date = value => value ? value.replace('T', ' ').slice(0, 16) : '长期'
export const modes = { NORMAL: '正常', READ_ONLY: '限制发布', BLOCKED: '封禁登录' }
export const actions = { HIDE: '下架', RESTORE: '恢复', NORMAL: '解除限制', READ_ONLY: '限制发布', BLOCKED: '封禁登录' }
export const types = { DREAM: '梦想', UPDATE: '进展', MEMBER: '账号' }
export function mediaUrl(url) { return url?.startsWith('/api/media/') ? url.replace('/api/media/', '/api/admin/media/') : '' }
