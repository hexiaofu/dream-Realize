/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { reactive, watch } from 'vue'
import { notify, request, requireLogin, session } from './api'
export const favoriteStates = reactive({}), favoriteBusy = reactive({})
watch(() => session.token, () => {
  Object.keys(favoriteStates).forEach(key => delete favoriteStates[key])
  Object.keys(favoriteBusy).forEach(key => delete favoriteBusy[key])
})
export const isFavorite = file => favoriteStates[file.id] ?? !!file.favorite
export function rememberFavorites(files) { files.forEach(file => { favoriteStates[file.id] = !!file.favorite }) }
export async function setFavorite(file, selected) {
  if (!requireLogin() || favoriteBusy[file.id]) return false
  if (session.user?.restriction?.mode === 'READ_ONLY') { notify('账号已限制发布'); return false }
  const token = session.token
  favoriteBusy[file.id] = true
  try {
    await request('/media-favorites/' + file.id, selected ? 'PUT' : 'DELETE')
    if (session.token !== token) return false
    favoriteStates[file.id] = selected
    return true
  } catch (error) { notify(error); return false }
  finally { if (session.token === token) delete favoriteBusy[file.id] }
}
