export default {
  common: {
    login: '登录',
    logout: '退出登录',
    cancel: '取消',
    confirm: '确认',
    delete: '删除',
    save: '保存',
    back: '返回',
    loading: '加载中...',
    success: '成功',
    failed: '失败',
    error: '错误',
    warning: '警告',
    tip: '提示',
    unknown: '未知',
    guest: '游客',
    guestUser: '游客用户'
  },

  language: {
    zh: '中文',
    en: 'English',
    switchTo: '切换语言'
  },

  auth: {
    welcomeBack: '欢迎回来',
    signInContinue: '登录以继续您的学习之旅',
    username: '用户名',
    password: '密码',
    enterUsername: '请输入用户名',
    enterPassword: '请输入密码',
    signIn: '登录',
    signingIn: '登录中...',
    signUp: '注册',
    orContinueWith: '或使用以下方式登录',
    signInWithGoogle: '使用 Google 登录',
    noAccount: '还没有账号？',
    backToHome: '← 返回首页',
    loginSuccess: '登录成功！正在跳转...',
    loginFailed: '登录失败，请重试',
    oauth2Failed: 'OAuth2 登录失败，请重试或使用用户名密码登录',
    logoutConfirm: '确定要退出登录吗？',
    logoutFailed: '退出失败，请重试'
  },

  dashboard: {
    studyDays: '学习天数',
    library: '收藏',
    youtubeVideos: 'YouTube 视频',
    xiaohongshuVideos: '小红书视频',
    profile: {
      bio: '热爱学习，每天进步 ✨',
      gender: '🙋',
      location: '📍 中国',
      level: '🌟 初学者'
    }
  },

  youtube: {
    title: 'AI 驱动的 YouTube 跟读练习',
    subtitle: '将任何 YouTube 视频转化为智能英语跟读练习',
    pasteHint: '粘贴 YouTube 链接，我们将立即生成跟读练习',
    generatePractice: '生成练习',
    analyzing: '分析视频中',
    extracting: '提取字幕中',
    generating: '生成练习中',

    systemNotReady: '系统未就绪：yt-dlp 未安装',
    installHint: '请在服务器上安装 yt-dlp',

    myLibrary: '我的视频库',
    videoCount: '{count} 个视频',
    noVideos: '暂无视频',

    status: {
      added: '已添加',
      parsing: '处理中',
      completed: '就绪',
      failed: '失败'
    },

    actions: {
      parse: '解析字幕',
      read: '阅读文稿',
      study: '学习笔记',
      download: '下载视频',
      pin: '置顶',
      unpin: '取消置顶',
      delete: '删除'
    },

    sentences: '{count} 句',

    pagination: {
      previous: '← 上一页',
      next: '下一页 →'
    },

    processing: '处理视频中...',
    videoId: '视频 ID',

    parseConfirm: '解析 "{title}" 的字幕？\n这可能需要几分钟时间。',
    parseStarted: '✅ 字幕解析已开始！\n您可以在列表中查看进度',
    parseFailed: '解析失败，请重试',
    parseSuccess: '成功！生成了 {count} 个练习句子',

    notParsed: '字幕尚未解析\n点击 📝 按钮解析并生成句子',
    isParsing: '字幕正在解析中，请稍后查看',
    notCompleted: '视频解析未完成，请稍后查看',

    deleteConfirm: '删除 "{title}" {sentenceInfo}？\n\n此操作无法撤销！',
    andSentences: '及其 {count} 个练习句子',
    deleteSuccess: '视频删除成功',
    deleteFailed: '删除失败',

    addFailed: '添加视频失败，请检查链接',
    enterUrl: '请输入 YouTube 视频链接',

    download: {
      title: '下载视频',
      quality: '视频质量',
      best: '最佳（自动）',
      '4k': '4K (2160p)',
      '2k': '2K (1440p)',
      '1080p': '全高清 (1080p)',
      '720p': '高清 (720p)',
      '480p': '标清 (480p)',
      qualityHint: '提示：学习推荐 1080p，存档推荐最佳质量',
      downloadVideo: '下载视频',
      audioOnly: '仅音频 (MP3)',
      extractMp3: '提取并转换为 MP3 格式',
      taskCreated: '✅ 下载任务已创建！\n质量：{quality}\n在右下角查看进度',
      downloadFailed: '下载失败',
      myDownloads: '我的下载',
      noTasks: '暂无下载任务',
      deleteTask: '删除此下载任务？下载的文件也将被删除。',
      taskDeleted: '下载任务已删除',
      isParsing: '视频正在解析中，请等待完成',
      isFailed: '视频解析失败，无法下载',

      status: {
        init: '等待中',
        queued: '排队中',
        parsing: '解析中',
        downloading: '下载中',
        merging: '合并中',
        success: '已完成',
        failed: '失败'
      },
      type: {
        video: '视频',
        audio: '音频',
        videoAudio: '视频+音频'
      },
      qualityDesc: {
        best: '自动选择最高可用质量',
        '4k': '超高清视频，文件较大',
        '2k': '2K 高清视频',
        '1080p': '全高清视频，推荐',
        '720p': '高清视频，文件适中',
        '480p': '标清视频，文件较小'
      }
    },

    practice: {
      title: '练习句子',
      noSentences: '暂无句子',
      collapse: '收起',
      practice: '练习',
      referenceAudio: '参考音频',
      play: '播放',
      stop: '停止',
      yourRecording: '你的录音',
      readyToPractice: '准备好练习了吗？',
      startRecording: '开始录音',
      recording: '录音中...',
      stopRecording: '停止',
      reRecord: '重新录音',
      score: '得分',
      pronunciation: '发音',
      fluency: '流利度',
      intonation: '语调',
      wordFeedback: '单词级反馈',
      correct: '正确',
      needImprovement: '需改进',
      suggestions: '建议',
      allCorrect: '🎉 完美！所有单词发音正确！',
      playerNotReady: '播放器未就绪，请稍候...',
      recordFailed: '录音失败，请检查麦克风权限',
      uploadFailed: '上传失败，请重试',
      taskNotCreated: '任务未创建，请重试',
      createTaskFailed: '创建练习任务失败，请重试'
    },

    difficulty: {
      easy: '简单',
      medium: '中等',
      hard: '困难'
    },

    notePreview: {
      notes: '{count} 条笔记',
      more: '+{count} 更多'
    },

    search: {
      title: '搜索字幕',
      placeholder: '在视频字幕中搜索关键词...',
      search: '搜索',
      searching: '搜索中...',
      results: '找到 {count} 个结果',
      noResults: '未找到匹配的结果',
      close: '关闭',
      esNotEnabled: '搜索服务未启用，请先配置 Elasticsearch',
      searchFailed: '搜索失败'
    }
  },

  settings: {
    title: '设置',
    developing: '设置功能开发中...'
  }
}