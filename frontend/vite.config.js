import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ isSsrBuild, mode }) => ({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: loadEnv(mode, process.cwd(), '').DEV_API_PROXY_TARGET || 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    manifest: true,
    outDir: 'dist',
    emptyOutDir: true,
    // 优化构建性能和内存使用
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: isSsrBuild ? undefined : {
          'vue-vendor': ['vue', 'vue-router'],
          'ui-vendor': ['lucide-vue-next'],
          'editor-vendor': ['@tiptap/vue-3', '@tiptap/starter-kit', '@tiptap/core']
        }
      }
    },
    // 使用 esbuild 进行压缩（更快，内存占用更少）
    minify: 'esbuild',
    // 禁用 sourcemap 以节省内存
    sourcemap: false
  }
}))


