import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
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
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    // 优化构建性能和内存使用
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router'],
          'ui-vendor': ['lucide-vue-next'],
          'editor-vendor': ['@tiptap/vue-3', '@tiptap/starter-kit', '@tiptap/pm', '@tiptap/core']
        }
      }
    },
    // 使用 esbuild 进行压缩（更快，内存占用更少）
    minify: 'esbuild',
    // 禁用 sourcemap 以节省内存
    sourcemap: false
  }
})


