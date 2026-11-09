# Stripe 会员收款

当前方案：每年 0.99 美元自动续费会员，可通过 Stripe 客户门户取消续费。入口 `/dashboard/billing`。
会员状态由已验证、已到账的付款记录计算：`GET /api/billing/membership`。
尚未对现有学习功能设置会员限制，退款也不会自动撤销会员；需要按业务规则扩展。

## 服务器配置

在 Stripe Dashboard 创建商品及 **USD 0.99 / year，recurring.interval=year，interval_count=1** 的 Price。后端在创建 Checkout 前验证价格、币种及类型。
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
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.paid`
- `invoice.payment_failed`
- `invoice.payment_action_required`

开发时：

```sh
stripe listen --events checkout.session.completed,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed,checkout.session.expired,customer.subscription.created,customer.subscription.updated,customer.subscription.deleted,invoice.paid,invoice.payment_failed,invoice.payment_action_required --forward-to http://localhost:8081/api/billing/webhook
```

将 CLI 返回的签名密钥设为本地 `STRIPE_WEBHOOK_SECRET`。
Webhook 校验原始请求体的 HMAC 签名及 5 分钟时间窗口，再从 Stripe 查询最新 Session。
查询当前订阅及最新发票，仅已付款且匹配 99 美分 USD 价格的发票行延长会员至 period.end。重复通知不会重复开通，返回页面不作为付款凭证。
新表 `stripe_payments` 由现有 Hibernate `ddl-auto=update` 配置创建。

## 验收

1. 使用测试密钥、测试 Price 及 CLI Webhook，登录后进入会员页。
2. 测试卡 `4242 4242 4242 4242`，未来有效期、任意 CVC，完成付款。
3. 回到网站刷新记录，确认付款成功及会员状态。
4. 测试取消付款、未登录访问、错误签名与重复通知；其他账号不能读取本账号记录。
5. 配置正式凭证及公开 Webhook 后再开放真实收款。

参考：[Checkout API](https://docs.stripe.com/api/checkout/sessions/create)、[Webhook 验证](https://docs.stripe.com/events/manage-webhook-endpoints)。

## 客户门户与权益

在正式环境 Stripe Billing → Customer portal 启用门户，允许更新付款方式、账单查看和账期末取消订阅；关闭套餐切换、数量修改、优惠码入口，保持固定年付价格。付款失败设置中配置重试与最终取消策略。

ECS 保留 /home/data/secrets/finance.env 中原有 OAuth 配置，更新 STRIPE_PRICE_ID 为正式年付价格。重新运行 aliyun-build.sh 创建容器加载配置。部署前备份 /home/data/finance/finance.db。

付费到期时间由发票行 period.end 确定；active / past_due 只在已付费时间内享有会员，未付款不会延长。账期末取消保留已付费期限，立即取消、unpaid、暂停订阅撤销权限。
续费付款按 invoice ID 去重；历史成功一次性订单保留原权益，不自动转为自动扣款。
已有未结束订阅不能重复开通；开放的待付 Checkout 在单进程内复用。多实例部署需要额外的下单并发约束。

新增订阅字段和乐观锁版本字段由 ddl-auto=update 添加。应用在 Hibernate 建表后自动将旧订单 NULL version 回填为 0，保留乐观锁保护，避免旧记录被识别为新实体；新记录由 Hibernate 管理版本值。

测试需覆盖首次付费、跨年续费、扣款失败后到期、重复/乱序回调、取消、门户和账号隔离。使用 Sandbox 与 Test Clocks，不触发正式扣款。单元 mock 测试不等同于 Stripe 全链路验收。
