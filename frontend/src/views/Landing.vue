<template>
  <div class="landing-page">
    <header class="site-header">
      <nav class="shell nav-bar" aria-label="主导航">
        <router-link to="/" class="brand" aria-label="X Learning 首页"><span class="brand-mark">X<span></span></span>X Learning<span class="brand-dot">.</span></router-link>
        <div class="desktop-nav"><a href="#directions">学习方向</a><a href="#workspace">学习体验</a><a href="#process">如何开始</a></div>
        <div class="nav-actions"><router-link to="/login" class="sign-in">登录</router-link><router-link to="/dashboard/youtube" class="button small">开始学习 <ArrowUpRight :size="15" /></router-link><button class="menu-toggle" :aria-expanded="menuOpen" aria-controls="mobile-nav" :aria-label="menuOpen ? '关闭菜单' : '打开菜单'" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="22"/><Menu v-else :size="22"/></button></div>
      </nav>
      <nav v-if="menuOpen" id="mobile-nav" class="mobile-nav" aria-label="移动导航"><a v-for="item in navigation" :key="item.id" :href="`#${item.id}`" @click="menuOpen = false">{{ item.label }}</a><router-link to="/login">登录账户</router-link></nav>
    </header>

    <main>
      <section class="hero shell">
        <div class="hero-copy">
          <p class="eyebrow"><span class="status-dot"></span> A NEW WAY TO LEARN</p>
          <h1>学语言，<br>也学<span class="accent">未来。</span></h1>
          <p class="hero-description">从英语表达，到 AI 探索。<br>在真实视频里发现知识，把每一次好奇，变成自己的能力。</p>
          <div class="hero-actions"><router-link to="/dashboard/youtube" class="button">开启学习之旅 <ArrowUpRight :size="18"/></router-link><a href="#directions" class="text-link">探索学习方向 <ArrowDown :size="16"/></a></div>
          <div class="hero-notes"><span><Check :size="14"/> 视频驱动学习</span><span><Check :size="14"/> 英语 × 人工智能</span><span><Check :size="14"/> 支持游客预览</span></div>
        </div>
        <div class="knowledge-visual" aria-label="英语与 AI 连接的知识轨道图" role="img">
          <div class="visual-grid"></div><div class="orbit orbit-outer"></div><div class="orbit orbit-middle"></div><div class="orbit orbit-inner"></div>
          <div class="orbital-line line-one"></div><div class="orbital-line line-two"></div>
          <div class="core"><span class="core-cross">X</span><span>LEARNING CORE</span></div>
          <div class="orbit-node node-language"><Languages :size="23"/><div><span>ENGLISH</span><strong>连接更大的世界</strong></div></div>
          <div class="orbit-node node-ai"><Cpu :size="23"/><div><span>ARTIFICIAL INTELLIGENCE</span><strong>探索下一种可能</strong></div></div>
          <span class="orbit-point point-one"></span><span class="orbit-point point-two"></span><span class="visual-caption">CURIOSITY → KNOWLEDGE → POSSIBILITY</span><span class="coordinate">XL / KNOWLEDGE NETWORK</span>
        </div>
      </section>

      <div class="topic-strip"><div class="shell"><span>保持好奇，持续进化</span><span>ENGLISH</span><i>✳</i><span>ARTIFICIAL INTELLIGENCE</span><i>✳</i><span>VIDEO LEARNING</span><i>✳</i><span>CONNECT THE DOTS</span></div></div>

      <section id="directions" class="section shell">
        <div class="section-heading"><div><p class="eyebrow">01 / EXPLORE</p><h2>两个方向，无限可能。</h2></div><p>用语言拓宽视野，用技术打开想象。<br>从你最感兴趣的地方开始。</p></div>
        <div class="tracks">
          <article v-for="track in tracks" :key="track.id" class="track" :class="track.id">
            <div class="track-top"><span class="track-number">{{ track.number }} / LEARNING PATH</span><component :is="track.icon" :size="28" :stroke-width="1.3"/></div>
            <p class="track-label">{{ track.label }}</p><h3>{{ track.title }}</h3><p class="track-description">{{ track.description }}</p>
            <div class="track-tags"><span v-for="tag in track.tags" :key="tag">{{ tag }}</span></div>
            <router-link to="/dashboard/youtube" class="track-link">{{ track.action }} <ArrowUpRight :size="20"/></router-link>
          </article>
        </div>
        <p class="library-note"><Layers :size="14"/> 两个方向共用视频学习库，你可以通过字幕搜索寻找感兴趣的内容。</p>
      </section>

      <section id="workspace" class="workspace-section">
        <div class="shell workspace-layout">
          <div class="workspace-copy"><p class="eyebrow">02 / LEARN DEEPLY</p><h2>不止于看过。<br>让知识真正留下来。</h2><p>把视频变成可以反复探索的学习材料。跟着字幕理解内容，定位关键片段，再用笔记整理自己的思考。</p><ul><li><Captions :size="20"/><div><h3>看懂每一个关键点</h3><p>结合视频与字幕，按照自己的节奏理解内容。</p></div></li><li><Search :size="20"/><div><h3>让问题找到上下文</h3><p>搜索字幕，回到知识出现的那一刻。</p></div></li><li><NotebookPen :size="20"/><div><h3>把输入变成自己的知识</h3><p>在学习工作台记录重点，连接新的想法。</p></div></li></ul></div>
          <div class="workspace-demo">
            <div class="demo-toolbar"><div class="window-dots"><i></i><i></i><i></i></div><span>XL / LEARNING WORKSPACE</span><span class="demo-label">体验示意</span></div>
            <div class="demo-tabs" role="group" aria-label="切换学习示意"><button v-for="sample in samples" :key="sample.id" :class="{active: activeSample === sample.id}" :aria-pressed="activeSample === sample.id" @click="activeSample = sample.id"><component :is="sample.icon" :size="15"/>{{ sample.label }}</button></div>
            <div class="demo-video"><div class="demo-grid"></div><span class="video-tag">{{ currentSample.category }}</span><div class="demo-center"><component :is="currentSample.icon" :size="42" :stroke-width="1"/><h3>{{ currentSample.title }}</h3><p>{{ currentSample.subtitle }}</p></div><span class="demo-play"><Play :size="12" fill="currentColor"/> VIDEO + TRANSCRIPT</span></div>
            <div class="transcript"><span class="time-code">{{ currentSample.time }}</span><p>{{ currentSample.transcript }}</p><span class="transcript-line"></span></div>
            <div class="demo-note"><NotebookPen :size="16"/><div><span>我的学习笔记</span><p>{{ currentSample.note }}</p></div><span class="note-indicator"></span></div>
          </div>
        </div>
      </section>

      <section id="process" class="section shell">
        <div class="section-heading"><div><p class="eyebrow">03 / YOUR NEXT STEP</p><h2>从好奇，到掌握。</h2></div><router-link to="/dashboard/youtube" class="text-link">现在就开始 <ArrowUpRight :size="17"/></router-link></div>
        <div class="steps"><article v-for="step in steps" :key="step.number"><span>{{ step.number }}</span><h3>{{ step.title }}</h3><p>{{ step.description }}</p></article></div>
      </section>

      <section class="closing shell"><div class="closing-orbit" aria-hidden="true"></div><p class="eyebrow">STAY CURIOUS. KEEP EVOLVING.</p><h2>下一次突破，<br>从这一次学习开始。</h2><p>学好英语，探索 AI，构建属于你的知识世界。</p><router-link to="/dashboard/youtube" class="button">开始探索 <ArrowUpRight :size="18"/></router-link><span class="preview-note">游客可预览前 6 个视频 · 登录后浏览全部</span></section>
    </main>

    <footer class="shell site-footer"><div><router-link to="/" class="brand"><span class="brand-mark">X<span></span></span>X Learning<span class="brand-dot">.</span></router-link><p>学语言，也学未来。</p></div><div class="footer-links"><a href="#directions">学习方向</a><router-link to="/dashboard/youtube">视频学习库</router-link><router-link to="/login">登录</router-link></div><span class="copyright">© {{ new Date().getFullYear() }} X Learning</span></footer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowUpRight, ArrowDown, Check, Languages, Cpu, Layers, Captions, Search, NotebookPen, Play, Menu, X } from 'lucide-vue-next'

const menuOpen = ref(false)
const navigation = [{ id: 'directions', label: '学习方向' }, { id: 'workspace', label: '学习体验' }, { id: 'process', label: '如何开始' }]
const tracks = [
  { id: 'english', number: '01', icon: Languages, label: 'EXPRESS YOURSELF', title: '用英语，连接世界。', description: '从真实语境出发，听懂表达，练习跟读。在感兴趣的视频里，积累词汇与语感，让语言成为探索世界的工具。', tags: ['听力理解', '口语跟读', '真实语境'], action: '进入视频库，练习英语' },
  { id: 'ai', number: '02', icon: Cpu, label: 'UNDERSTAND WHAT’S NEXT', title: '学 AI，打开未来。', description: '带着问题探索人工智能。通过视频、字幕和笔记，拆解新概念，理解新思路，把零散的信息整理为自己的知识。', tags: ['人工智能', '概念理解', '学习笔记'], action: '进入视频库，探索 AI' }
]
const samples = [
  { id: 'english', icon: Languages, label: '英语学习', category: 'ENGLISH / LISTEN & UNDERSTAND', title: 'Ideas worth sharing.', subtitle: '在真实表达中，发现语言的力量', time: '00:24', transcript: 'Every great idea begins with a little curiosity.', note: 'curiosity：好奇心。记录表达，再试着用自己的话复述。' },
  { id: 'ai', icon: Cpu, label: 'AI 探索', category: 'AI / EXPLORE & CONNECT', title: 'How does AI learn?', subtitle: '从一个问题出发，理解人工智能', time: '01:08', transcript: 'Neural networks learn patterns from examples.', note: '神经网络从样本中学习模式。继续思考：数据如何影响结果？' }
]
const activeSample = ref('english')
const currentSample = computed(() => samples.find(sample => sample.id === activeSample.value))
const steps = [
  { number: '01', title: '找到你的兴趣', description: '进入视频库，选择内容，或通过字幕搜索找到想了解的话题。' },
  { number: '02', title: '按照自己的节奏探索', description: '结合视频和字幕学习，回看关键片段，让难点变得清晰。' },
  { number: '03', title: '沉淀每一次收获', description: '练习表达，记录笔记，把看到的内容变成自己的理解。' }
]
</script>

<style src="@/assets/styles/landing.css"></style>
