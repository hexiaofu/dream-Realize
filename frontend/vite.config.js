/** @author 沙里飞 @email 1499058156@qq.com @company 青海老铁软件有限公司 */
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: { proxy: { '/api': { target: 'http://127.0.0.1:18080', changeOrigin: true } } }
})
