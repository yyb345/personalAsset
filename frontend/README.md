# FinanceHub Frontend (Vue 3)

这是 FinanceHub 的 Vue 3 前端项目。

## 技术栈

- Vue 3 (Composition API)
- Vue Router 4
- ECharts 5
- Axios
- Vite

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 项目结构

```
frontend/
├── src/
│   ├── assets/         # 静态资源
│   │   └── styles/    # 样式文件
│   ├── router/        # 路由配置
│   ├── utils/         # 工具函数
│   ├── views/         # 页面组件
│   │   └── dashboard/ # Dashboard子页面
│   ├── App.vue        # 根组件
│   └── main.js        # 入口文件
├── index.html         # HTML模板
├── package.json       # 项目配置
└── vite.config.js     # Vite配置
```

## 开发说明

1. 开发时前端运行在 `http://localhost:3000`
2. API请求会代理到后端 `http://localhost:8080`
3. 构建后的文件会输出到 `../src/main/resources/static`

## 部署

运行 `npm run build` 后，构建的文件会自动放入Spring Boot的静态资源目录，可以直接通过Spring Boot访问。


