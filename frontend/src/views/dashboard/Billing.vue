<template>
  <main class="checkout-page">
    <section class="order-summary" :aria-label="t('billing.orderSummary')">
      <div class="summary-inner">
        <router-link to="/dashboard/youtube" class="back-link"><ArrowLeft :size="16" /><span class="brand-mark">X</span><span>X Learning</span></router-link>
        <div class="price-heading">
          <h1>{{ t('billing.title') }}</h1>
          <div class="price"><span>{{ price }}</span><span class="price-caption">{{ t('billing.currencyName') }}<br />{{ t('billing.oneTime') }}</span></div>
        </div>
        <div class="product-row"><div class="product-icon">X<span>+</span></div><div class="product-name"><strong>{{ t('billing.productName') }}</strong><span>{{ t('billing.purchaseNote') }}</span></div><strong>{{ price }}</strong></div>
        <dl class="totals">
          <div><dt>{{ t('billing.subtotal') }}</dt><dd>{{ price }}</dd></div>
          <div class="total-row"><dt>{{ t('billing.total') }}</dt><dd>{{ price }}<span>USD</span></dd></div>
        </dl>
        <p class="summary-note"><Check :size="15" />{{ t('billing.summaryNote') }}</p>
        <router-link class="return-link" to="/dashboard/youtube">{{ t('billing.back') }} <ArrowUpRight :size="13" /></router-link>
      </div>
    </section>
    <section class="payment-panel" :aria-label="t('billing.paymentSection')">
      <div class="payment-inner">
        <select class="billing-language" :value="locale" @change="setLocale($event.target.value)" :aria-label="t('billing.language')"><option value="en-US">English</option><option value="zh-CN">中文</option></select>
        <div class="secure-label"><LockKeyhole :size="14" /> {{ t('billing.secureCheckout') }}</div>
        <h2>{{ active ? t('billing.activeTitle') : t('billing.paymentTitle') }}</h2>
        <p class="intro">{{ active ? t('billing.activeIntro') : t('billing.intro') }}</p>
        <div class="payment-method"><span class="card-symbol"><CreditCard :size="22" /></span><div><strong>{{ t('billing.stripePayment') }}</strong><span>{{ t('billing.methods') }}</span></div><LockKeyhole :size="15" class="method-lock" /></div>
        <div class="payment-details"><span>{{ t('billing.amount') }}</span><strong>{{ price }} <small>USD</small></strong></div>
        <div v-if="active" class="success-message" role="status"><Check :size="18" />{{ t('billing.activeMessage') }}</div>
        <button v-else-if="!hasOpenSubscription" class="pay-button" :disabled="loading || paying || (authenticated && !enabled)" @click="checkout"><LockKeyhole :size="16" />{{ loading ? t('billing.loading') : paying ? t('billing.redirecting') : authenticated ? t('billing.pay', { amount: price }) : t('billing.login') }}<ArrowUpRight :size="16" /></button>
        <p v-if="memberUntil" class="notice">{{ t('billing.memberUntil', { date: formatDate(memberUntil) }) }}</p>
        <p v-if="cancelAtPeriodEnd" class="notice">{{ t('billing.renewalCanceled') }}</p>
        <p v-if="subscriptionStatus === 'past_due'" class="notice">{{ t('billing.pastDue') }}</p>
        <button v-if="canManage" class="pay-button" :disabled="paying || loading" @click="manageSubscription">{{ t('billing.manage') }}</button>
        <p v-if="!loading && !enabled" class="notice">{{ t('billing.unavailable') }}</p>
        <p v-if="route.query.canceled" class="notice" role="status">{{ t('billing.canceled') }}</p>
        <p v-if="route.query.session_id && !active" class="notice" role="status">{{ t('billing.pendingConfirmation') }}</p>
        <p v-if="error" class="error" role="alert">{{ t(error) }}</p>
        <p class="payment-note">{{ t('billing.paymentNote') }}</p>
        <div class="stripe-footer">{{ t('billing.poweredBy') }} <strong>stripe</strong> {{ t('billing.provided') }} <span></span><LockKeyhole :size="12" /> {{ t('billing.encrypted') }}</div>
        <details class="history"><summary>{{ t('billing.history') }} <span>{{ payments.length }}</span></summary><div class="history-content"><button class="refresh" :disabled="loading" @click="load"><RefreshCw :size="13" />{{ t('billing.refresh') }}</button><p v-if="!payments.length">{{ authenticated ? t('billing.noPayments') : t('billing.loginHistory') }}</p><ul v-else><li v-for="payment in payments" :key="payment.id"><span>{{ formatDate(payment.createdAt) }}</span><strong>{{ paymentStatus(payment.status) }}</strong><span v-if="payment.status === 'paid'">{{ formatMoney(payment.amount, payment.currency) }}</span></li></ul></div></details>
      </div>
    </section>
  </main>
</template>
<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { ArrowLeft, LockKeyhole, ArrowUpRight, Check, RefreshCw, CreditCard } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { track, confirmedPurchase } from '@/utils/learningAnalytics'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'
const { t, locale } = useI18n()
const formatMoney = (amount, currency = 'usd') => new Intl.NumberFormat(locale.value, { style: 'currency', currency: currency.toUpperCase() }).format(amount / 100)
const price = computed(() => formatMoney(99))
const formatDate = value => new Date(value).toLocaleString(locale.value)
const paymentStatus = status => ['pending', 'paid', 'failed', 'expired'].includes(status) ? t(`billing.${status}`) : status
const route = useRoute()
const router = useRouter()
const authenticated = ref(false)
const loading = ref(true), paying = ref(false), enabled = ref(false), active = ref(false)
const payments = ref([]), error = ref('')
const hasOpenSubscription = ref(false), canManage = ref(false), memberUntil = ref(''), cancelAtPeriodEnd = ref(false), subscriptionStatus = ref('')
let confirmationTimer, confirmationAttempts = 0, disposed = false
async function load() {
  clearTimeout(confirmationTimer)
  loading.value = true
  error.value = ''
  try {
    const [config, auth] = await Promise.all([axios.get('/api/billing/config'), axios.get('/api/auth/check')])
    enabled.value = config.data.enabled
    authenticated.value = auth.data.authenticated
    window.xLearningAnalytics?.setAuthenticated(authenticated.value)
    if (!authenticated.value) return
    const [history, membership] = await Promise.all([axios.get('/api/billing/payments'), axios.get('/api/billing/membership')])
    payments.value = history.data
    active.value = membership.data.active
    hasOpenSubscription.value = membership.data.hasOpenSubscription
    canManage.value = membership.data.canManage
    memberUntil.value = membership.data.memberUntil || ''
    cancelAtPeriodEnd.value = membership.data.cancelAtPeriodEnd
    subscriptionStatus.value = membership.data.subscriptionStatus || ''
    if (!disposed && typeof route.query.session_id === 'string') {
      // Status and ownership come from the authenticated backend history, never the return URL.
      const confirmed = confirmedPurchase(payments.value, route.query.session_id)
      if (confirmed) track('purchase', { transaction_id: confirmed.id })
      else if (confirmationAttempts++ < 20) confirmationTimer = setTimeout(load, 3000)
    }
  } catch { error.value = 'billing.loadError' }
  finally { loading.value = false }
}
async function checkout() {
  if (!authenticated.value) { router.push('/login'); return }
  paying.value = true
  error.value = ''
  try {
    const { data } = await axios.post('/api/billing/checkout', {}, { timeout: 30000 })
    const url = new URL(data.url)
    if (url.protocol !== 'https:' || url.hostname !== 'checkout.stripe.com') throw new Error('Invalid checkout URL')
    track('begin_checkout')
    window.location.assign(url.href)
  } catch { error.value = 'billing.checkoutError'; paying.value = false }
}
async function manageSubscription() {
  paying.value = true
  error.value = ''
  try {
    const { data } = await axios.post('/api/billing/portal')
    const url = new URL(data.url)
    if (url.protocol !== 'https:' || url.hostname !== 'billing.stripe.com') throw new Error('Invalid portal URL')
    window.location.assign(url.href)
  } catch { error.value = 'billing.portalError'; paying.value = false }
}
onMounted(load)
onBeforeUnmount(() => { disposed = true; clearTimeout(confirmationTimer) })
</script>
<style scoped>
.billing-language { display: block; margin: 0 0 20px auto; padding: 6px 10px; border: 1px solid #d9dce2; border-radius: 6px; background: white; color: #505563; }
.checkout-page { min-height: 100dvh; display: grid; grid-template-columns: 1fr 1fr; background: #fff; color: #30313d; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; font-size: 14px; -webkit-font-smoothing: antialiased; }
.checkout-page * { box-sizing: border-box; }
.order-summary { padding: 58px 48px 64px; background: #fafbfc; box-shadow: inset -1px 0 #edf0f3; }
.summary-inner { max-width: 380px; margin-left: auto; margin-right: 32px; }
a { text-decoration: none; }
.back-link { display: flex; gap: 12px; align-items: center; color: #30313d; font-size: 14px; }
.back-link > svg { color: #959ba4; margin-left: -28px; }
.brand-mark { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 7px; background: #30313d; color: white; font-weight: 700; }
.price-heading { margin-top: 42px; }
h1 { font-size: 16px; font-weight: 500; color: #6b7078; margin: 0 0 8px; }
.price { display: flex; align-items: center; gap: 12px; }
.price > span:first-child { font-size: 40px; line-height: 1.2; letter-spacing: -1.8px; font-weight: 600; }
.price-caption { color: #757980; font-size: 12px; line-height: 1.6; }
.product-row { display: flex; gap: 12px; align-items: center; margin-top: 48px; padding-bottom: 27px; }
.product-icon { width: 44px; height: 44px; display: grid; place-items: center; background: #f0edff; color: #6553bc; border-radius: 10px; font-size: 25px; font-weight: 650; position: relative; }
.product-icon span { position: absolute; font-size: 13px; top: 3px; right: 4px; }
.product-name { display: flex; flex: 1; flex-direction: column; gap: 6px; }
.product-name span { font-size: 12px; color: #838790; }
strong { font-weight: 550; }
.totals { margin: 0; border-top: 1px solid #e3e6e9; }
.totals > div { display: flex; justify-content: space-between; padding: 21px 0; }
dd { margin: 0; }
.totals .total-row { border-top: 1px solid #e3e6e9; font-weight: 600; }
.total-row dd span { margin-left: 7px; font-size: 11px; color: #838790; font-weight: 400; }
.summary-note { display: flex; gap: 7px; font-size: 12px; color: #7d838b; margin: 16px 0 0; line-height: 1.6; }
.summary-note svg { flex-shrink: 0; margin-top: 2px; }
.return-link { display: inline-flex; align-items: center; gap: 4px; color: #858b94; font-size: 12px; margin-top: 64px; }
.payment-panel { padding: 88px 48px 64px; }
.payment-inner { max-width: 380px; margin-left: 32px; }
.secure-label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6e747e; }
h2 { font-size: 21px; font-weight: 600; margin: 16px 0 10px; letter-spacing: -.5px; }
.intro { color: #7c818a; font-size: 13px; line-height: 1.7; margin: 0 0 30px; }
.payment-method { border: 1px solid #d9dce3; box-shadow: 0 1px 3px #18244408; border-radius: 9px; padding: 17px 14px; display: flex; align-items: center; gap: 12px; }
.card-symbol { display: grid; place-items: center; color: #5f6570; }
.payment-method > div { display: flex; flex-direction: column; gap: 6px; }
.payment-method strong { font-size: 13px; }
.payment-method div span { font-size: 11px; color: #858993; }
.method-lock { margin-left: auto; color: #949aa3; }
.payment-details { display: flex; justify-content: space-between; margin: 28px 0 20px; font-size: 13px; }
.payment-details > span { color: #737985; }
small { font-size: 11px; font-weight: 400; color: #8d929b; margin-left: 4px; }
.pay-button { display: flex; justify-content: center; align-items: center; gap: 8px; width: 100%; height: 46px; padding: 0 16px; background: #635bff; border: 0; border-radius: 7px; color: white; font: inherit; font-weight: 550; cursor: pointer; box-shadow: 0 2px 4px #2016b615; transition: background .15s; }
.pay-button:hover:enabled { background: #5148e5; }
.pay-button:disabled { background: #b2aef5; cursor: not-allowed; }
.pay-button svg:last-child { margin-left: auto; }
.pay-button svg:first-child { margin-right: auto; }
button:focus-visible, a:focus-visible, summary:focus-visible { outline: 3px solid #b3afff; outline-offset: 4px; }
.notice { font-size: 12px; color: #8a7554; line-height: 1.6; margin: 12px 0; }
.error { color: #b52c3b; font-size: 12px; line-height: 1.6; }
.payment-note { color: #9297a0; font-size: 11px; text-align: center; line-height: 1.8; margin: 16px 0 0; }
.stripe-footer { display: flex; align-items: center; justify-content: center; gap: 5px; color: #9398a0; font-size: 11px; margin-top: 44px; }
.stripe-footer strong { font-size: 19px; letter-spacing: -.8px; color: #666d78; font-weight: 750; }
.stripe-footer > span { height: 14px; width: 1px; background: #d9dce2; margin: 0 9px; }
.history { border-top: 1px solid #eceef1; margin-top: 34px; padding-top: 18px; }
.history summary { cursor: pointer; font-size: 12px; color: #7d838c; }
.history summary > span { float: right; color: #a1a5ad; }
.history-content { padding-top: 16px; font-size: 12px; color: #9096a0; }
.refresh { display: inline-flex; align-items: center; gap: 5px; border: none; background: transparent; color: #635bff; font: inherit; cursor: pointer; padding: 0; }
ul { padding: 0; list-style: none; } li { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 0; border-top: 1px solid #eee; }
.success-message { display: flex; align-items: center; justify-content: center; gap: 8px; background: #edf8f1; border-radius: 7px; padding: 16px; color: #26744a; }
@media (max-width: 1050px) { .summary-inner { margin-right: 0; } .payment-inner { margin-left: 0; } .order-summary, .payment-panel { padding-left: 32px; padding-right: 32px; } .back-link > svg { margin-left: 0; } }
@media (max-width: 700px) { .checkout-page { display: block; } .order-summary { padding: 28px 28px 32px; box-shadow: inset 0 -1px #edf0f3; } .summary-inner, .payment-inner { max-width: 420px; margin: 0 auto; } .price-heading { margin-top: 30px; } .price > span:first-child { font-size: 36px; } .product-row { margin-top: 28px; padding-bottom: 22px; } .totals > div { padding: 17px 0; } .return-link { display: none; } .summary-note { margin-top: 6px; } .payment-panel { padding: 30px 28px 40px; } .intro { margin-bottom: 22px; } .stripe-footer { margin-top: 30px; } }
</style>
