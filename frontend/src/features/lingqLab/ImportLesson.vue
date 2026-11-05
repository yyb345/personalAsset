<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { newWordPercent } from './model'
import { extractWebArticle, safeSourceUrl } from './webImport'
const props = defineProps({ vocabulary: Object })
const emit = defineEmits(['import'])
const step = ref(1), source = ref('text'), title = ref(''), text = ref(''), url = ref(''), sourceUrl = ref(''), extracted = ref(false), loading = ref(false), error = ref('')
let controller, requestId = 0
const ready = computed(() => source.value === 'text' || extracted.value)
function cancel() { requestId++; controller?.abort(); controller = null; loading.value = false }
function reset(kind = 'text') { cancel(); source.value = kind; title.value = ''; text.value = ''; url.value = ''; sourceUrl.value = ''; extracted.value = false; error.value = '' }
function choose(kind) { reset(kind); step.value = 2 }
function backToSources() { reset(); step.value = 1 }
function changeUrl() { cancel(); extracted.value = false; sourceUrl.value = ''; title.value = ''; text.value = ''; error.value = '' }
async function extract() {
  cancel(); error.value = ''; extracted.value = false; sourceUrl.value = ''; title.value = ''; text.value = ''
  if (!safeSourceUrl(url.value)) { error.value = '请输入有效的 http:// 或 https:// 网页链接。'; return }
  controller = new AbortController(); const requestController = controller; const current = requestId; loading.value = true
  const timeout = setTimeout(() => requestController.abort(), 30000)
  try {
    const article = await extractWebArticle(url.value, { signal: requestController.signal })
    if (current !== requestId) return
    title.value = article.title; text.value = article.text; sourceUrl.value = article.sourceUrl; extracted.value = true
  } catch (cause) {
    if (current !== requestId) return
    error.value = cause.name === 'AbortError' ? '提取超时，请重试或改用粘贴文本。' : cause instanceof TypeError ? '暂时无法连接提取服务，请检查网络后重试。' : cause.message || '提取失败，请稍后重试。'
  } finally { clearTimeout(timeout); if (current === requestId) { loading.value = false; controller = null } }
}
function validate() {
  if (!ready.value) { error.value = '请先成功提取网页正文。'; return false }
  if (!title.value.trim() || !/[a-zA-Z]/.test(text.value)) { error.value = '请填写标题和包含英文的正文。'; return false }
  if (text.value.length > 50000) { error.value = '正文不能超过 50,000 字符。'; return false }
  error.value = ''; return true
}
function preview() { if (validate()) step.value = 3 }
function confirm() { if (!validate() || loading.value) return; emit('import', {title:title.value.trim(), text:text.value.trim(), ...(source.value === 'web' ? {sourceUrl:sourceUrl.value} : {})}) }
onBeforeUnmount(cancel)
</script>
<template>
  <div class="import-shell"><p class="eyebrow">BRING YOUR OWN CURIOSITY</p><h1>把喜欢的内容，<br>变成你的下一课。</h1><p class="muted">从你关心的内容开始，创建自己的阅读材料。</p><ol class="import-steps"><li :class="{active:step===1}">1 选择来源</li><li :class="{active:step===2}">2 添加内容</li><li :class="{active:step===3}">3 预览并导入</li></ol>
  <div v-if="step===1" class="source-grid"><button class="source-choice" @click="choose('text')"><strong>▤ 粘贴文本</strong><span>把英文正文变成可交互阅读材料 →</span></button><button class="source-choice" @click="choose('web')"><strong>↗ 网页链接</strong><span>提取公开网页正文，编辑后再导入 →</span></button><button v-for="item in ['音频转写','文件 / 电子书','扫描文字','流媒体']" :key="item" class="source-choice" disabled><strong>{{ item }}</strong><span>尚未接入</span></button></div>
  <div v-else-if="step===2"><form v-if="source==='web'" class="web-extract" @submit.prevent="extract"><label for="web-url">公开网页链接</label><div class="url-row"><input id="web-url" v-model="url" @input="changeUrl" type="url" required placeholder="https://example.com/article" autocomplete="url"><button class="primary" type="submit" :disabled="loading">{{ loading?'正在提取…':extracted?'重新提取':error?'重试提取':'提取正文' }}</button></div><p class="fine">仅提取公开网页文本。需要登录、动态加载或不允许抓取的页面可能无法读取，可改用粘贴正文。</p><p v-if="loading" class="loading" role="status" aria-live="polite">正在读取网页，请稍候…你可以返回选择其他来源。</p><p v-if="extracted" class="success" role="status">已提取正文。请检查并编辑内容，确认后再导入。</p></form>
  <form @submit.prevent="preview"><template v-if="ready"><label for="import-title">课程标题</label><input id="import-title" v-model="title" maxlength="160" placeholder="例如：The future of AI"><label for="import-text">英文正文 <small>{{ text.length.toLocaleString() }} / 50,000 字符</small></label><textarea id="import-text" v-model="text" rows="9" maxlength="50000" placeholder="Paste something you are curious about…"></textarea><p class="fine">材料与学习进度保存在当前浏览器，请使用有权使用的内容。</p></template><p v-if="error" class="error" role="alert">{{ error }}</p><div class="actions"><button class="secondary" type="button" @click="backToSources">← 返回来源</button><button v-if="ready" class="primary green" type="submit" :disabled="loading">预览内容 →</button></div></form></div>
  <template v-else><article class="import-preview"><p class="eyebrow">REVIEW YOUR LESSON</p><h2>{{ title }}</h2><p class="fine">{{ text.trim().split(/\s+/).length }} 词 · {{ newWordPercent(text, props.vocabulary) }}% 新词</p><a v-if="sourceUrl" :href="sourceUrl" target="_blank" rel="noopener noreferrer" class="source-link">查看原网页 ↗</a><p class="preview-body">{{ text }}</p></article><p v-if="error" class="error" role="alert">{{ error }}</p><div class="actions"><button class="secondary" @click="step=2">← 返回编辑</button><button class="primary green" @click="confirm">确认导入并阅读 →</button></div></template></div>
</template>
<style scoped>
.import-shell{max-width:600px;margin:auto}.eyebrow{font-size:10px;letter-spacing:2px;font-weight:700;color:#879474;margin-bottom:22px}.import-shell h1{font-size:36px;line-height:1.4;letter-spacing:-1px;margin-bottom:15px}.muted{font-size:13px;line-height:1.9;color:#7f8a97}.import-steps{display:flex;list-style:none;gap:15px;justify-content:space-between;font-size:11px;color:#9aa3ae;border-bottom:1px solid #e2e7ec;padding:28px 0 18px;margin-bottom:25px}.import-steps .active{color:#314d22;font-weight:700}.source-grid{display:grid;grid-template-columns:1fr 1fr;gap:15px}.source-choice{padding:25px 20px;border:1px solid #dce4d3;border-radius:9px;background:white;text-align:left;color:#40582f;font:inherit;cursor:pointer}.source-choice:nth-child(-n+2){background:#eff7e7;border-color:#b4d095}.source-choice strong{display:block;font-size:15px}.source-choice span{display:block;font-size:10px;margin-top:15px;color:#87977a;line-height:1.8}.source-choice:disabled{opacity:.4;cursor:default}.import-shell label{display:block;font-size:12px;font-weight:600;margin:23px 0 10px}.import-shell label small{font-size:10px;color:#94a086;font-weight:400;margin-left:10px}.import-shell input,.import-shell textarea{width:100%;font:inherit;font-size:14px;border:1px solid #d7dfd2;border-radius:7px;padding:12px 15px;background:white;color:#24374d}.import-shell textarea{resize:vertical;line-height:1.7}.primary,.secondary{font:inherit;font-size:12px;border:0;border-radius:7px;padding:14px 18px;background:#142340;color:white;cursor:pointer;white-space:nowrap}.secondary{background:transparent;border:1px solid #d8dfd1;color:#6c7b5c}.green{background:#baf56b;color:#30511d}.primary:disabled{opacity:.5;cursor:default}.url-row{display:flex;gap:10px;align-items:stretch}.url-row input{min-width:0}.fine{font-size:10px;line-height:1.9;color:#8c9783;margin-top:12px}.actions{display:flex;gap:15px;margin-top:25px}.actions>button{flex:1}.error{font-size:12px;color:#a44434;background:#fff0ed;border:1px solid #f0d1c9;padding:12px 15px;line-height:1.9;border-radius:7px;margin-top:18px;overflow-wrap:anywhere}.loading,.success{font-size:12px;line-height:1.9;color:#667d45;margin-top:15px}.import-preview{background:white;border:1px solid #dce3d4;border-radius:12px;padding:28px;margin:24px 0}.import-preview h2{font-size:26px;line-height:1.5;overflow-wrap:anywhere}.preview-body{white-space:pre-wrap;font-family:Georgia,serif;line-height:1.9;font-size:17px;margin-top:20px;max-height:400px;overflow:auto}.source-link{font-size:11px;color:#527cb1;display:inline-block;margin-top:14px;overflow-wrap:anywhere}button:focus-visible,input:focus-visible,textarea:focus-visible,a:focus-visible{outline:3px solid #498bed;outline-offset:3px}@media(max-width:600px){.import-shell h1{font-size:30px}.url-row{flex-direction:column}.import-preview{padding:22px}.source-choice{padding:23px 17px}.import-steps{font-size:10px;gap:10px}.actions button{font-size:11px;padding:14px 12px}}
</style>
