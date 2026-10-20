/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  // API_PROXY_TARGET 可临时指向其他后端，默认本机 18080。
  server: { proxy: { '/api': { target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:18080', changeOrigin: true } } }
})
