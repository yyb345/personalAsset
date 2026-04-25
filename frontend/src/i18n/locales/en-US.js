export default {
  common: {
    login: 'Login',
    logout: 'Logout',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    save: 'Save',
    back: 'Back',
    loading: 'Loading...',
    success: 'Success',
    failed: 'Failed',
    error: 'Error',
    warning: 'Warning',
    tip: 'Tip',
    unknown: 'Unknown',
    guest: 'Guest',
    guestUser: 'Guest User'
  },

  language: {
    zh: '中文',
    en: 'English',
    switchTo: 'Switch Language'
  },

  auth: {
    welcomeBack: 'Welcome Back',
    signInContinue: 'Sign in to continue your learning journey',
    username: 'Username',
    password: 'Password',
    enterUsername: 'Enter your username',
    enterPassword: 'Enter your password',
    signIn: 'Sign In',
    signingIn: 'Signing in...',
    signUp: 'Sign Up',
    orContinueWith: 'or continue with',
    signInWithGoogle: 'Sign in with Google',
    noAccount: "Don't have an account?",
    backToHome: '← Back to Home',
    loginSuccess: 'Login successful! Redirecting...',
    loginFailed: 'Login failed. Please try again.',
    oauth2Failed: 'OAuth2 login failed. Please try again or use username/password.',
    logoutConfirm: 'Are you sure you want to logout?',
    logoutFailed: 'Logout failed, please try again'
  },

  dashboard: {
    studyDays: 'Study Days',
    library: 'Library',
    youtubeVideos: 'YouTube Videos',
    xiaohongshuVideos: 'Xiaohongshu Videos',
    profile: {
      bio: 'Love learning, improve every day ✨',
      gender: '🙋',
      location: '📍 Unknown',
      level: '🌟 Learner'
    }
  },

  youtube: {
    title: 'AI-powered YouTube Shadowing',
    subtitle: 'Turn any YouTube video into intelligent English shadowing practice',
    pasteHint: 'Paste a YouTube link. We\'ll generate shadowing practice instantly.',
    generatePractice: 'Generate Practice',
    analyzing: 'Analyzing video',
    extracting: 'Extracting subtitles',
    generating: 'Generating practice',

    systemNotReady: 'System not ready: yt-dlp not installed',
    installHint: 'Please install yt-dlp on the server',

    myLibrary: 'My Library',
    videoCount: '{count} videos',
    noVideos: 'No videos yet',

    status: {
      added: 'Added',
      parsing: 'Processing',
      completed: 'Ready',
      failed: 'Failed'
    },

    actions: {
      parse: 'Parse Subtitles',
      read: 'Read Transcript',
      study: 'Study Workspace',
      download: 'Download Video',
      pin: 'Pin to top',
      unpin: 'Unpin',
      delete: 'Delete'
    },

    sentences: '{count} sentences',

    pagination: {
      previous: '← Previous',
      next: 'Next →'
    },

    processing: 'Processing video...',
    videoId: 'Video ID',

    parseConfirm: 'Parse subtitles for "{title}"?\nThis may take a few minutes.',
    parseStarted: '✅ Subtitle parsing started!\nYou can check the progress in the list',
    parseFailed: 'Failed to parse. Please try again',
    parseSuccess: 'Success! Generated {count} practice sentences',

    notParsed: 'Subtitles not parsed yet\nClick the 📝 button to parse and generate sentences',
    isParsing: 'Subtitles are being parsed. Please check back later',
    notCompleted: 'Video parsing incomplete. Please check back later',

    deleteConfirm: 'Delete "{title}" {sentenceInfo}?\n\nThis action cannot be undone!',
    andSentences: 'and its {count} practice sentences',
    deleteSuccess: 'Video deleted successfully',
    deleteFailed: 'Delete failed',

    addFailed: 'Failed to add video. Please check the URL',
    enterUrl: 'Please enter a YouTube video link',

    download: {
      title: 'Download Video',
      quality: 'Video Quality',
      best: 'Best (Auto)',
      '4k': '4K (2160p)',
      '2k': '2K (1440p)',
      '1080p': 'Full HD (1080p)',
      '720p': 'HD (720p)',
      '480p': 'SD (480p)',
      qualityHint: 'Tip: Choose 1080p for learning, best quality for archiving',
      downloadVideo: 'Download Video',
      audioOnly: 'Audio Only (MP3)',
      extractMp3: 'Extract and convert to MP3 format',
      taskCreated: '✅ Download task created!\nQuality: {quality}\nCheck progress in the bottom right corner',
      downloadFailed: 'Download failed',
      myDownloads: 'My Downloads',
      noTasks: 'No download tasks',
      deleteTask: 'Delete this download task? The downloaded file will also be deleted.',
      taskDeleted: 'Download task deleted',
      isParsing: 'Video is being parsed. Please wait until completion',
      isFailed: 'Video parsing failed. Cannot download',

      status: {
        init: 'Waiting',
        queued: 'Queued',
        parsing: 'Parsing',
        downloading: 'Downloading',
        merging: 'Merging',
        success: 'Completed',
        failed: 'Failed'
      },
      type: {
        video: 'Video',
        audio: 'Audio',
        videoAudio: 'Video+Audio'
      },
      qualityDesc: {
        best: 'Auto-select highest available quality',
        '4k': 'Ultra HD video, large file size',
        '2k': '2K high definition video',
        '1080p': 'Full HD video, recommended',
        '720p': 'HD video, moderate file size',
        '480p': 'Standard definition, smaller file'
      }
    },

    practice: {
      title: 'Practice Sentences',
      noSentences: 'No sentences available',
      collapse: 'Collapse',
      practice: 'Practice',
      referenceAudio: 'Reference Audio',
      play: 'Play',
      stop: 'Stop',
      yourRecording: 'Your Recording',
      readyToPractice: 'Ready to practice?',
      startRecording: 'Start Recording',
      recording: 'Recording...',
      stopRecording: 'Stop',
      reRecord: 'Re-record',
      score: 'Score',
      pronunciation: 'Pronunciation',
      fluency: 'Fluency',
      intonation: 'Intonation',
      wordFeedback: 'Word-level Feedback',
      correct: 'Correct',
      needImprovement: 'Need improvement',
      suggestions: 'Suggestions',
      allCorrect: '🎉 Perfect! All words are pronounced correctly!',
      playerNotReady: 'Player not ready. Please wait...',
      recordFailed: 'Recording failed. Please check microphone permissions',
      uploadFailed: 'Upload failed. Please try again',
      taskNotCreated: 'Task not created. Please try again.',
      createTaskFailed: 'Failed to create practice task. Please try again.'
    },

    difficulty: {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard'
    },

    notePreview: {
      notes: '{count} note | {count} notes',
      more: '+{count} more'
    }
  },

  settings: {
    title: 'Settings',
    developing: 'Settings feature is under development...'
  }
}