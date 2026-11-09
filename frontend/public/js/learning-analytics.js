/* Shared by server-rendered public pages and Vue. Never send URL queries, private material IDs or user content. */
(function (window, document) {
  'use strict';
  if (window.xLearningAnalytics) return;
  var measurementId = 'G-6MKRR655RZ';
  var storageKey = 'xlearning.acquisition.v1';
  var timeout = 30 * 60 * 1000;
  var lastPage = null;
  var context = null;
  var authState = 'unknown';
  var sentPurchases = Object.create(null);
  var guideSlugs = ['how-to-get-youtube-transcript', 'english-shadowing-for-beginners', 'active-listening-with-youtube', 'learn-english-with-subtitles', 'english-dictation-practice', 'build-vocabulary-from-videos', 'choose-english-learning-videos', 'take-notes-from-english-videos', 'shadowing-vs-listen-and-repeat', 'seven-day-english-listening-plan'];
  var fixedPages = ['/', '/guides', '/video', '/login', '/register', '/lingq-lab', '/dashboard', '/dashboard/youtube', '/dashboard/xiaohongshu', '/dashboard/billing', '/shadowing', '/transcribe'];
  function pagePath(input) {
    var path = String(input || '/').split(/[?#]/)[0].replace(/\/$/, '') || '/';
    if (path === '/index.html') return '/';
    if (path === '/zh') return '/zh/';
    if (fixedPages.indexOf(path) !== -1) return path;
    if (path.indexOf('/guides/') === 0 && guideSlugs.indexOf(path.slice(8)) !== -1) return path;
    if (/^\/video\/[A-Za-z0-9_-]{11}$/.test(path)) return path;
    if (/^\/dashboard\/study-workspace\/[^/]+$/.test(path)) return '/dashboard/study-workspace/:videoId';
    return '/other';
  }
  function publicPage(path) { return path === '/' || path === '/zh/' || path === '/video' || path === '/guides' || path.indexOf('/guides/') === 0 || /^\/video\/[A-Za-z0-9_-]{11}$/.test(path); }
  function safeReferrer() {
    try {
      var referrer = new URL(document.referrer);
      return ['http:', 'https:'].indexOf(referrer.protocol) !== -1 ? referrer.origin + '/' : '';
    } catch (_) { return ''; }
  }
  function entrySource() {
    if (!document.referrer) return 'direct_or_unknown';
    try {
      var host = new URL(document.referrer).hostname;
      if (host === window.location.hostname) return 'internal';
      if (/(^|\.)(google\.[a-z.]+|bing\.com|baidu\.com|duckduckgo\.com|search\.yahoo\.com)$/.test(host)) return 'search_referrer';
    } catch (_) { return 'direct_or_unknown'; }
    return 'external_referrer';
  }
  function readContext() {
    try {
      var stored = JSON.parse(window.sessionStorage.getItem(storageKey));
      if (stored && stored.expires > Date.now() && publicPage(stored.landing_page) && pagePath(stored.landing_page) === stored.landing_page && ['direct_or_unknown', 'internal', 'search_referrer', 'external_referrer'].indexOf(stored.entry_source) !== -1) return stored;
    } catch (_) { /* Storage can be unavailable. Analytics must never block learning. */ }
    return null;
  }
  function saveContext() {
    if (!context) return;
    context.expires = Date.now() + timeout;
    try { window.sessionStorage.setItem(storageKey, JSON.stringify(context)); } catch (_) { /* In-memory attribution still works. */ }
  }
  window.dataLayer = window.dataLayer || [];
  window.gtag = window.gtag || function () { window.dataLayer.push(arguments); };
  function send(name, parameters) {
    if (context && context.expires <= Date.now()) context = null;
    window.gtag('event', name, Object.assign({
      send_to: measurementId,
      transport_type: 'beacon',
      ui_language: document.documentElement && document.documentElement.lang === 'zh-CN' ? 'zh-CN' : 'en-US',
      auth_state: authState,
      page_location: window.location.origin + pagePath(window.location.pathname),
      page_referrer: safeReferrer(),
      page_title: 'X Learning',
      landing_page: context ? context.landing_page : 'none',
      entry_source: context ? context.entry_source : 'none'
    }, parameters));
    saveContext();
  }
  function page(input) {
    var path = pagePath(input);
    // The server document and initial Vue route describe the same page.
    if (path === lastPage) return;
    lastPage = path;
    context = readContext() || (context && context.expires > Date.now() ? context : null);
    var newLanding = !context && publicPage(path);
    if (newLanding) context = { landing_page: path, entry_source: entrySource(), expires: Date.now() + timeout };
    send('page_view', { page_path: path, page_location: window.location.origin + path });
    if (newLanding) send('seo_landing', { page_path: path });
  }
  function materialOpened(kind) {
    if (['video', 'reading'].indexOf(kind) === -1) return;
    send('material_opened', { material_type: kind });
  }
  function startLearning(kind) {
    if (['video', 'reading', 'workspace'].indexOf(kind) === -1) kind = 'workspace';
    send('start_learning', { learning_type: kind, source_page: pagePath(window.location.pathname) });
  }
  function choice(value, values, fallback) { return values.indexOf(value) !== -1 ? value : fallback; }
  function details(input) {
    input = input || {};
    return {
      content_language: choice(input.content_language, ['en', 'zh'], 'unknown'),
      content_category: choice(input.content_category, ['basics', 'models', 'agents', 'applications', 'english', 'other'], 'other'),
      entry_point: choice(input.entry_point, ['library', 'learning_path', 'search', 'homepage', 'direct'], 'direct')
    };
  }
  function event(name, input) {
    input = input || {};
    if (['video_selected', 'video_start', 'learning_engaged', 'learning_milestone'].indexOf(name) !== -1) {
      var params = details(input);
      if (name === 'learning_milestone') {
        if ([180, 600].indexOf(input.learning_seconds) === -1) return;
        params.learning_seconds = input.learning_seconds;
      }
      if (name === 'learning_engaged') params.learning_seconds = 60;
      send(name, params);
    } else if (name === 'sign_up') {
      send(name, { method: 'email' });
    } else if (name === 'upgrade_clicked') {
      send(name, { entry_point: 'dashboard_header' });
    } else if (name === 'begin_checkout' || name === 'purchase') {
      var commerce = { currency: 'CNY', value: 99, items: [{ item_id: 'membership', item_name: 'X Learning membership', price: 99, quantity: 1 }] };
      if (name === 'purchase') {
        // Only an opaque internal order UUID is allowed; never send Stripe sessions or user IDs.
        if (!/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i.test(input.transaction_id || '')) return;
        var key = 'xlearning.purchase.' + input.transaction_id;
        if (sentPurchases[key]) return;
        try { if (window.localStorage.getItem(key)) return; } catch (_) { /* Memory dedupe remains available. */ }
        commerce.transaction_id = input.transaction_id;
        send(name, commerce);
        sentPurchases[key] = true;
        try { window.localStorage.setItem(key, '1'); } catch (_) { /* GA also deduplicates transaction_id. */ }
        return;
      }
      send(name, commerce);
    }
  }
  window.xLearningAnalytics = {
    page: page, materialOpened: materialOpened, startLearning: startLearning, event: event,
    setAuthenticated: function (value) { authState = value === true ? 'logged_in' : value === false ? 'guest' : 'unknown'; }
  };
  window.gtag('js', new Date());
  window.gtag('config', measurementId, {
    send_page_view: false,
    page_location: window.location.origin + pagePath(window.location.pathname),
    page_referrer: safeReferrer(), page_title: 'X Learning', allow_google_signals: false
  });
  // Only our explicit events are used. No user properties or DOM text are collected here.
  // Keep local development/test events in dataLayer without sending them to production GA.
  if (['www.xlearning.top', 'xlearning.top'].indexOf(window.location.hostname) !== -1) {
    var tag = document.createElement('script');
    tag.async = true;
    tag.src = 'https://www.googletagmanager.com/gtag/js?id=' + measurementId;
    document.head.appendChild(tag);
  }
  document.addEventListener('click', function (event) {
    if (event.defaultPrevented || event.button > 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    var target = event.target.closest && event.target.closest('[data-seo-action="start_learning"]');
    if (target) startLearning(target.getAttribute('data-learning-type'));
  }, true);
  page(window.location.pathname);
})(window, document);
