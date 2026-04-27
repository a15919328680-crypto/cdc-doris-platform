import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8081,
    host: '0.0.0.0',
    allowedHosts: [
      '18b558eccbf688d5.monkeycode-ai.online',
      '8081-18b558eccbf688d5.monkeycode-ai.online',
      '.monkeycode-ai.online'
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
