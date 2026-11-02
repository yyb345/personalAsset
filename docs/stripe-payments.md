# Stripe 会员收款

当前方案：一次性人民币 99 元购买会员，无自动续费。入口 `/dashboard/billing`。
会员状态由已验证、已到账的付款记录计算：`GET /api/billing/membership`。
尚未对现有学习功能设置会员限制，退款也不会自动撤销会员；需要按业务规则扩展。

## 服务器配置

在 Stripe Dashboard 创建商品及 **一次性 CNY 99.00** 的 Price。后端在创建 Checkout 前验证价格、币种及类型。
通过部署平台的环境变量注入，不能写入 Git 或前端：

```sh
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PRICE_ID=price_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_BASE_URL=http://localhost:3000
```

生产环境使用对应正式密钥、正式 Price，并把 `STRIPE_BASE_URL` 设置为网站 HTTPS 域名。
未配置完整时付款按钮不可用。托管 Checkout 不需要前端 publishable key。

## Webhook

公开端点：`POST /api/billing/webhook`。Stripe Dashboard 注册网站的完整 HTTPS URL，订阅：

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`

开发时：

```sh
stripe listen --forward-to localhost:8081/api/billing/webhook
```

将 CLI 返回的签名密钥设为本地 `STRIPE_WEBHOOK_SECRET`。
Webhook 校验原始请求体的 HMAC 签名及 5 分钟时间窗口，再从 Stripe 查询最新 Session。
仅确认 `payment_status=paid` 且人民币 9900 分时开通会员。重复通知不会重复开通，返回页面不作为付款凭证。
新表 `stripe_payments` 由现有 Hibernate `ddl-auto=update` 配置创建。

## 验收

1. 使用测试密钥、测试 Price 及 CLI Webhook，登录后进入会员页。
2. 测试卡 `4242 4242 4242 4242`，未来有效期、任意 CVC，完成付款。
3. 回到网站刷新记录，确认付款成功及会员状态。
4. 测试取消付款、未登录访问、错误签名与重复通知；其他账号不能读取本账号记录。
5. 配置正式凭证及公开 Webhook 后再开放真实收款。

参考：[Checkout API](https://docs.stripe.com/api/checkout/sessions/create)、[Webhook 验证](https://docs.stripe.com/events/manage-webhook-endpoints)。
