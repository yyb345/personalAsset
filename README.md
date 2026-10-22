http://www.xlearning.top


/dashboard/youtube?debug=true

## Google OAuth 配置

在 Google Cloud 创建 Web application 类型的 OAuth 客户端。
本地回调地址为 `http://localhost:8081/login/oauth2/code/google`；
线上回调地址为 `https://你的域名/login/oauth2/code/google`。

后端读取环境变量 `GOOGLE_CLIENT_ID` 和 `GOOGLE_CLIENT_SECRET`。
本地也可以在项目根目录创建已被 Git 忽略的 `application-local.properties`：

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
```

线上通过容器环境变量或服务器上的私密 env 文件注入凭据，修改后重新创建容器。
不要将真实凭据提交到 Git，也不要将本地配置文件打包进镜像。
