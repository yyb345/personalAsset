<script setup>
import { computed } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { useRoute } from 'vue-router'
import { ARTICLE_VARIANTS, articleForPath } from '@/features/articles'
const route = useRoute()
const article = computed(() => articleForPath(route.path) || ARTICLE_VARIANTS['en-US'])
const zh = computed(() => article.value.lang === 'zh-CN')
const text = (cn, en) => zh.value ? cn : en
</script>

<template>
  <div class="research-page">
    <header class="research-header">
      <BrandLogo :aria-label="text('X Learning 首页', 'X Learning Home')" />
      <nav aria-label="Language / 语言">
        <router-link v-for="(variant, locale) in ARTICLE_VARIANTS" :key="locale" :to="variant.path" :lang="variant.lang" :aria-current="variant.path === article.path ? 'page' : undefined">{{ locale === 'zh-CN' ? '中文' : 'English' }}</router-link>
      </nav>
    </header>
    <main>
      <div class="research-hero">
        <p class="research-eyebrow">AI FOR SCIENCE · STANFORD HAI</p>
        <h1>{{ article.title }}</h1>
        <p class="research-deck">{{ article.description }}</p>
        <div class="research-meta"><span>Jeff Dean × Fei-Fei Li</span><span>{{ text('原视频 48:22 · 演讲解读', '48:22 source video · Talk analysis') }}</span></div>
        <a class="research-watch" href="/transcribe?videoId=ooGKNwJWdb8">{{ text('观看视频与英文字幕', 'Watch the video and read the transcript') }} <span aria-hidden="true">↗</span></a>
      </div>
      <div class="research-layout">
        <aside class="research-toc"><nav :aria-label="text('文章目录', 'Table of contents')"><p>{{ text('文章目录', 'In this article') }}</p><a v-for="section in article.sections" :key="section[0]" :href="`#${section[0]}`">{{ section[1] }}</a><a href="#faq">{{ text('常见问题', 'Frequently asked questions') }}</a></nav></aside>
        <article class="research-body" :lang="article.lang">
          <p class="research-note">{{ text('本文根据视频字幕进行主题整理与中文改写，并非逐字翻译。公司方向与进展按演讲时的表述呈现。', 'This article is a thematic adaptation of the video transcript, rather than a verbatim transcript. Company plans and progress reflect statements made during the talk.') }}</p>
          <p v-for="paragraph in article.intro" :key="paragraph">{{ paragraph }}</p>
          <section v-for="section in article.sections" :id="section[0]" :key="section[0]">
            <h2>{{ section[1] }}</h2>
            <template v-for="(paragraph, index) in section[2]" :key="paragraph">
              <p>{{ paragraph }}</p>
              <ol v-if="index === 0 && section[0] === 'discovery-loop'"><li v-for="item in section[3]" :key="item">{{ item }}</li></ol>
            </template>
            <ul v-if="section[3] && section[0] !== 'discovery-loop'"><li v-for="item in section[3]" :key="item">{{ item }}</li></ul>
          </section>
          <section id="faq"><h2>{{ text('常见问题', 'Frequently asked questions') }}</h2><div v-for="[question, answer] in article.faq" :key="question" class="research-faq"><h3>{{ question }}</h3><p>{{ answer }}</p></div></section>
          <footer class="research-sources"><h2>{{ text('内容来源', 'Sources') }}</h2><p><a href="https://www.youtube.com/watch?v=ooGKNwJWdb8">Stanford HAI — Confronting Our AI Future: Closing Keynote “Discovery Loop” with Jeff Dean and Fei-Fei Li</a></p><p><a href="https://www.xlearning.top/transcribe?videoId=ooGKNwJWdb8">{{ text('X Learning 视频字幕', 'X Learning video transcript') }}</a></p></footer>
        </article>
      </div>
    </main>
    <footer class="research-footer"><router-link :to="zh ? '/zh/' : '/'">X Learning</router-link><span>{{ text('学 AI，也练英语。', 'Explore AI. Build your English.') }}</span></footer>
  </div>
</template>

<style scoped>
.research-page{background:var(--app-surface);color:var(--app-text);min-height:100vh}.research-header,.research-footer{max-width:1200px;margin:auto;padding:25px 32px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid var(--app-border)}.research-brand{font-size:23px;font-weight:650;text-decoration:none;color:var(--app-text)}.research-brand span{display:block;font-size:11px;font-weight:500;letter-spacing:1.5px;margin-top:5px}.research-header nav{display:flex;gap:8px}.research-header nav a{padding:9px 13px;text-decoration:none;color:var(--app-muted);border-radius:6px}.research-header nav a[aria-current=page]{background:var(--app-accent-soft);color:var(--app-accent)}.research-hero{max-width:1000px;margin:auto;padding:72px 32px 52px}.research-eyebrow{font-size:12px;letter-spacing:2px;color:var(--app-accent);font-weight:700}.research-hero h1{font-weight:600;font-size:clamp(30px,4.1vw,50px);line-height:1.3;letter-spacing:-1px;margin:22px 0}.research-deck{font-size:18px;line-height:1.85;color:var(--app-muted);max-width:820px}.research-meta{display:flex;flex-wrap:wrap;gap:12px 25px;font-size:13px;color:var(--app-muted);margin:24px 0}.research-watch{display:inline-flex;gap:24px;background:var(--app-accent);color:white;padding:14px 20px;border-radius:8px;text-decoration:none;font-size:14px}.research-layout{max-width:1200px;margin:0 auto;padding:0 32px 80px;display:grid;grid-template-columns:235px minmax(0,740px);gap:65px}.research-toc nav{position:sticky;top:24px;border-top:2px solid var(--app-border);padding-top:20px}.research-toc p{font-size:13px;font-weight:700;margin-bottom:14px}.research-toc a{display:block;font-size:13px;line-height:1.6;margin:12px 0;color:var(--app-muted);text-decoration:none}.research-toc a:hover,.research-body a:hover{text-decoration:underline}.research-body{font-size:17px;line-height:1.95;overflow-wrap:anywhere}.research-body p{margin:0 0 22px}.research-body section{scroll-margin-top:25px}.research-body h2{font-weight:600;font-size:26px;line-height:1.5;margin:48px 0 20px;letter-spacing:-.4px}.research-body h3{font-size:18px;line-height:1.6;margin-bottom:12px}.research-body ul,.research-body ol{padding-left:25px;margin:18px 0 26px}.research-body li{padding-left:5px;margin-bottom:9px}.research-note{border-radius:0 8px 8px 0;background:var(--app-accent-soft);border-left:3px solid var(--app-accent);padding:17px 20px;font-size:14px;color:var(--app-muted)}.research-faq{border-top:1px solid var(--app-border);padding-top:23px;margin-top:25px}.research-body a{color:var(--app-accent);text-underline-offset:4px}.research-sources{font-size:14px}.research-footer{border-top:1px solid var(--app-border);border-bottom:0;font-size:13px;color:var(--app-muted)}.research-footer a{color:var(--app-accent);font-weight:700;text-decoration:none}.research-watch:hover{background:var(--app-accent-hover)}.research-toc a:hover{color:var(--app-accent)}a:focus-visible{outline:2px solid var(--app-accent);outline-offset:4px}@media(max-width:800px){.research-layout{grid-template-columns:1fr;gap:30px;padding:0 22px 50px}.research-toc nav{position:static}.research-toc a{margin:9px 0}.research-hero{padding:42px 22px 35px}.research-header,.research-footer{padding:20px 22px}.research-brand span{font-size:9px}.research-body{font-size:16px}.research-body h2{font-weight:600;font-size:23px}}
</style>
