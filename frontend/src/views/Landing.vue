<template>
  <div class="landing-page">
    <header class="site-header">
      <nav class="shell nav-bar" :aria-label="tr('主导航')">
        <BrandLogo :aria-label="locale === 'zh-CN' ? 'X Learning 首页' : 'X Learning Home'" />
        <div class="desktop-nav"><router-link data-seo-action="start_learning" data-learning-type="reading" to="/lingq-lab">{{ tr("英语学习") }}</router-link><a href="#directions">{{ tr("学习方向") }}</a><a href="#workspace">{{ tr("学习体验") }}</a><a href="#process">{{ tr("如何开始") }}</a></div>
        <div class="nav-actions"><select class="locale-select" :value="locale" aria-label="Language / 语言" @change="switchLanguage($event.target.value)"><option value="en-US">English</option><option value="zh-CN">中文</option></select><router-link to="/login" class="sign-in">{{ tr("登录") }}</router-link><router-link data-seo-action="start_learning" data-learning-type="video" to="/dashboard/youtube" class="button small">{{ tr("AI 学习") }}<ArrowUpRight :size="15" /></router-link><button class="menu-toggle" :aria-expanded="menuOpen" aria-controls="mobile-nav" :aria-label="tr(menuOpen ? '关闭菜单' : '打开菜单')" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="22"/><Menu v-else :size="22"/></button></div>
      </nav>
      <nav v-if="menuOpen" id="mobile-nav" class="mobile-nav" :aria-label="tr('移动导航')"><a v-for="item in navigation" :key="item.id" :href="`#${item.id}`" @click="menuOpen = false">{{ item.label }}</a><router-link data-seo-action="start_learning" data-learning-type="reading" to="/lingq-lab" @click="menuOpen = false">{{ tr("英语学习") }}</router-link><router-link to="/login">{{ tr("登录账户") }}</router-link></nav>
    </header>

    <main>
      <section class="hero shell">
        <div class="hero-copy">
          <p class="eyebrow"><span class="status-dot"></span> A NEW WAY TO LEARN</p>
          <h1>{{ tr("学 AI，") }}<br>{{ tr("也练") }}<span class="accent">{{ tr("英语。") }}</span></h1>
          <p class="hero-description">{{ tr("AI 学习：通过视频、字幕与笔记，理解人工智能。") }}<br>{{ tr("英语学习：导入文章、点击查词与词卡复习，积累英语能力。") }}</p>
          <div class="hero-actions"><router-link data-seo-action="start_learning" data-learning-type="video" to="/dashboard/youtube" class="button">{{ tr("AI 学习") }}<ArrowUpRight :size="18"/></router-link><router-link data-seo-action="start_learning" data-learning-type="reading" to="/lingq-lab" class="text-link">{{ tr("英语学习") }}<ArrowUpRight :size="16"/></router-link></div>
          <div class="hero-notes"><span><Check :size="14"/>{{ tr("视频驱动学习") }}</span><span><Check :size="14"/>{{ tr("英语 × 人工智能") }}</span><span><Check :size="14"/>{{ tr("支持游客预览") }}</span></div>
        </div>
        <aside class="hero-library" :aria-label="tr('学习路线预览')">
          <div class="library-preview-heading"><span class="preview-icon"><NotebookPen :size="20" /></span><div><strong>{{ tr("给好奇心，一个清晰的方向") }}</strong><p>{{ tr("从感兴趣的专题开始，按照自己的节奏学习") }}</p></div></div>
          <router-link v-for="(item, index) in previewTopics" :key="item.title" :to="item.to" data-seo-action="start_learning" :data-learning-type="item.to === '/lingq-lab' ? 'reading' : 'video'" class="preview-topic">
            <span class="topic-index">0{{ index + 1 }}</span><div><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div><ArrowUpRight :size="17" />
          </router-link>
          <div class="preview-footer"><Check :size="15" />{{ tr("视频与字幕") }}<span>·</span>{{ tr("跟读练习") }}<span>·</span>{{ tr("学习笔记") }}</div>
        </aside>
      </section>



      <section id="directions" class="section shell">
        <div class="section-heading"><div><p class="eyebrow">01 / EXPLORE</p><h2>{{ tr("一个学习过程，两种收获。") }}</h2></div><p>{{ tr("围绕 AI 内容建立知识框架，") }}<br>{{ tr("在真实语境中积累英语表达。") }}</p></div>
        <div class="tracks">
          <article v-for="track in tracks" :key="track.id" class="track" :class="track.id">
            <div class="track-top"><span class="track-number">{{ track.number }} / LEARNING PATH</span><component :is="track.icon" :size="28" :stroke-width="1.3"/></div>
            <p class="track-label">{{ track.label }}</p><h3>{{ track.title }}</h3><p class="track-description">{{ track.description }}</p>
            <div class="track-tags"><span v-for="tag in track.tags" :key="tag">{{ tag }}</span></div>
            <router-link :to="track.to" data-seo-action="start_learning" :data-learning-type="track.id === 'english' ? 'reading' : 'video'" class="track-link">{{ track.action }} <ArrowUpRight :size="20"/></router-link>
          </article>
        </div>
        <p class="library-note"><Layers :size="14"/>{{ tr("从 AI 基础、大模型到智能体与应用，按专题学习；也可先从英语练习开始。") }}</p>
      </section>

      <section id="workspace" class="workspace-section">
        <div class="shell workspace-layout">
          <div class="workspace-copy"><p class="eyebrow">02 / LEARN DEEPLY</p><h2>{{ tr("不止于看过。") }}<br>{{ tr("让知识真正留下来。") }}</h2><p>{{ tr("把视频变成可以反复探索的学习材料。跟着字幕理解内容，定位关键片段，再用笔记整理自己的思考。") }}</p><ul><li><Captions :size="20"/><div><h3>{{ tr("看懂每一个关键点") }}</h3><p>{{ tr("结合视频与字幕，按照自己的节奏理解内容。") }}</p></div></li><li><Search :size="20"/><div><h3>{{ tr("让问题找到上下文") }}</h3><p>{{ tr("搜索字幕，回到知识出现的那一刻。") }}</p></div></li><li><NotebookPen :size="20"/><div><h3>{{ tr("把输入变成自己的知识") }}</h3><p>{{ tr("在学习工作台记录重点，连接新的想法。") }}</p></div></li></ul></div>
          <div class="workspace-demo">
            <div class="demo-toolbar"><div class="window-dots"><i></i><i></i><i></i></div><span>XL / LEARNING WORKSPACE</span><span class="demo-label">{{ tr("体验示意") }}</span></div>
            <div class="demo-tabs" role="group" :aria-label="tr('切换学习示意')"><button v-for="sample in samples" :key="sample.id" :class="{active: activeSample === sample.id}" :aria-pressed="activeSample === sample.id" @click="activeSample = sample.id"><component :is="sample.icon" :size="15"/>{{ sample.label }}</button></div>
            <div class="demo-video"><div class="demo-grid"></div><span class="video-tag">{{ currentSample.category }}</span><div class="demo-center"><component :is="currentSample.icon" :size="42" :stroke-width="1"/><h3>{{ currentSample.title }}</h3><p>{{ currentSample.subtitle }}</p></div><span class="demo-play"><Play :size="12" fill="currentColor"/> VIDEO + TRANSCRIPT</span></div>
            <div class="transcript"><span class="time-code">{{ currentSample.time }}</span><p>{{ currentSample.transcript }}</p><span class="transcript-line"></span></div>
            <div class="demo-note"><NotebookPen :size="16"/><div><span>{{ tr("我的学习笔记") }}</span><p>{{ currentSample.note }}</p></div><span class="note-indicator"></span></div>
          </div>
        </div>
      </section>

      <section id="process" class="section shell">
        <div class="section-heading"><div><p class="eyebrow">03 / YOUR NEXT STEP</p><h2>{{ tr("从好奇，到掌握。") }}</h2></div><router-link data-seo-action="start_learning" data-learning-type="video" to="/dashboard/youtube" class="text-link">{{ tr("现在就开始") }}<ArrowUpRight :size="17"/></router-link></div>
        <div class="steps"><article v-for="step in steps" :key="step.number"><span>{{ step.number }}</span><h3>{{ step.title }}</h3><p>{{ step.description }}</p></article></div>
      </section>

      <section class="closing shell"><p class="eyebrow">STAY CURIOUS. KEEP EVOLVING.</p><h2>{{ tr("下一次突破，") }}<br>{{ tr("从这一次学习开始。") }}</h2><p>{{ tr("通过视频学 AI，在学习中练英语。") }}</p><router-link data-seo-action="start_learning" data-learning-type="video" to="/dashboard/youtube" class="button">{{ tr("开始探索") }}<ArrowUpRight :size="18"/></router-link><span class="preview-note">{{ tr("游客可预览前 6 个视频 · 登录后浏览全部") }}</span></section>
    </main>

    <footer class="shell site-footer"><div><BrandLogo :aria-label="locale === 'zh-CN' ? 'X Learning 首页' : 'X Learning Home'" /><p>{{ tr("学 AI，也练英语。") }}</p></div><div class="footer-links"><router-link :to="locale === 'zh-CN' ? '/zh/articles/jeff-dean-discovery-loop-ai-scientific-discovery' : '/articles/jeff-dean-discovery-loop-ai-scientific-discovery'">{{ locale === 'zh-CN' ? 'AI 科学发现' : 'AI Scientific Discovery' }}</router-link><a href="/guides">{{ tr("英语学习指南") }}</a><a href="/video">{{ tr("视频字幕") }}</a><a href="#directions">{{ tr("学习方向") }}</a><router-link data-seo-action="start_learning" data-learning-type="video" to="/dashboard/youtube">{{ tr("AI 学习") }}</router-link><router-link data-seo-action="start_learning" data-learning-type="reading" to="/lingq-lab">{{ tr("英语学习") }}</router-link><router-link to="/login">{{ tr("登录") }}</router-link></div><span class="copyright">© {{ new Date().getFullYear() }} X Learning</span></footer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { HOME_VARIANTS, applyHomeMetadata } from '@/features/homepage/metadata'
import { landingEnglish } from '@/i18n/landing'

const { locale } = useI18n()
const router = useRouter()
watch(locale, value => applyHomeMetadata(value), { immediate: true })
function switchLanguage(value) {
  if (HOME_VARIANTS[value]) router.push({ path: HOME_VARIANTS[value].path, hash: router.currentRoute.value.hash })
}
const tr = (text) => locale.value === 'zh-CN' ? text : (landingEnglish[text] || text)
const localize = (items) => items.map(item => Object.fromEntries(Object.entries(item).map(([key, value]) => [key, typeof value === 'string' ? tr(value) : Array.isArray(value) ? value.map(tr) : value])))
import BrandLogo from '@/components/BrandLogo.vue'
import { ArrowUpRight, Check, Languages, Cpu, Layers, Captions, Search, NotebookPen, Play, Menu, X } from 'lucide-vue-next'

const menuOpen = ref(false)
const previewTopics = computed(() => localize([
  { title: 'AI 基础与大模型', description: '建立概念框架，理解模型与技术表达', to: '/dashboard/youtube' },
  { title: '智能体与应用实践', description: '探索 Agent、工作流和实际应用', to: '/dashboard/youtube' },
  { title: '英语阅读与词汇积累', description: '导入文章、点击查词，复习掌握新词', to: '/lingq-lab' }
]))
const navigation = computed(() => localize([{ id: 'directions', label: '学习方向' }, { id: 'workspace', label: '学习体验' }, { id: 'process', label: '如何开始' }]))
const tracks = computed(() => localize([
  { id: 'english', number: '01', icon: Languages, label: 'EXPRESS YOURSELF', title: '用英语，连接世界。', description: '从感兴趣的英文文章出发，导入网页或文本，点击查词、保存生词，用词卡复习巩固，让每次阅读都有积累。', tags: ['英文阅读', '点击查词', '词卡复习'], action: '英语学习', to: '/lingq-lab' },
  { id: 'ai', number: '02', icon: Cpu, label: 'UNDERSTAND WHAT’S NEXT', title: '学 AI，打开未来。', description: '带着问题探索人工智能。通过视频、字幕和笔记，拆解新概念，理解新思路，把零散的信息整理为自己的知识。', tags: ['人工智能', '概念理解', '学习笔记'], action: 'AI 学习', to: '/dashboard/youtube' }
]))
const samples = computed(() => localize([
  { id: 'english', icon: Languages, label: '英语学习', category: 'ENGLISH / LISTEN & UNDERSTAND', title: 'Ideas worth sharing.', subtitle: '在真实表达中，发现语言的力量', time: '00:24', transcript: 'Every great idea begins with a little curiosity.', note: 'curiosity：好奇心。记录表达，再试着用自己的话复述。' },
  { id: 'ai', icon: Cpu, label: 'AI 探索', category: 'AI / EXPLORE & CONNECT', title: 'How does AI learn?', subtitle: '从一个问题出发，理解人工智能', time: '01:08', transcript: 'Neural networks learn patterns from examples.', note: '神经网络从样本中学习模式。继续思考：数据如何影响结果？' }
]))
const activeSample = ref('english')
const currentSample = computed(() => samples.value.find(sample => sample.id === activeSample.value))
const steps = computed(() => localize([
  { number: '01', title: '找到你的兴趣', description: '进入视频库，选择内容，或通过字幕搜索找到想了解的话题。' },
  { number: '02', title: '按照自己的节奏探索', description: '结合视频和字幕学习，回看关键片段，让难点变得清晰。' },
  { number: '03', title: '沉淀每一次收获', description: '练习表达，记录笔记，把看到的内容变成自己的理解。' }
]))
</script>

<style src="@/assets/styles/landing.css"></style>
