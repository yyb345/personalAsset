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
    youtubeVideos: 'AI & English Learning',
    xiaohongshuVideos: 'Xiaohongshu Videos',
    profile: {
      bio: 'Love learning, improve every day ✨',
      gender: '🙋',
      location: '📍 Unknown',
      level: '🌟 Learner'
    }
  },

  youtube: {
    library: {
      "pathsTitle": "Curated topics · Learn in sequence",
      "pathsHint": "Hand-picked from the production library in a suggested order. Only ready videos are recommended.",
      "pathCount": "{count} videos · Filter topic",
      "category": "Category",
      "difficulty": "Content difficulty",
      "duration": "Duration",
      "reset": "View all / Reset",
      "loading": "Loading video library…",
      "loadFailed": "Unable to load the video library. Please retry.",
      "retry": "Retry",
      "noMatches": "No videos match these filters yet. Reset filters or explore another topic.",
      "metadataHint": "Existing videos are classified from production titles; new videos use title-based rules. Unconfirmed difficulty stays unlabelled. Goals are based on titles.",
      "learnGoal": "Learning goal:",
      "suggestedGoal": "Suggested goal:",
      "categories": {
            "all": "All",
            "basics": "AI fundamentals",
            "models": "Language models",
            "agents": "AI agents",
            "applications": "Applications",
            "english": "English practice",
            "other": "Uncategorised",
            "business": "Business & startups",
            "technology": "Programming & computing",
            "japanese": "Japanese practice"
      },
      "levels": {
            "all": "All levels",
            "easy": "Beginner",
            "medium": "Intermediate",
            "hard": "Advanced",
            "unknown": "Unlabelled"
      },
      "durations": {
            "all": "Any duration",
            "short": "Up to 10 minutes",
            "medium": "10–20 minutes",
            "long": "Over 20 minutes"
      },
      "paths": {
            "foundations": {
                  "title": "From AI fundamentals to models",
                  "description": "Build a conceptual foundation, then explore language models and technical English."
            },
            "building": {
                  "title": "From agents to applications",
                  "description": "Explore agents before workflows and applications. Capture implementation ideas in notes."
            },
            "english": {
                  "title": "Build listening and everyday English",
                  "description": "Practise listening, shadowing and retelling with everyday videos before technical content."
            }
      },
      "objectives": {
            "basics": "Map key concepts, collect English terms and explain them in your own words.",
            "models": "Organise model concepts and use transcripts to understand technical expressions.",
            "agents": "Identify the main parts of an agent and note questions and implementation ideas.",
            "applications": "Record the steps and consider how to apply them in your own work.",
            "english": "Collect everyday expressions, shadow key sentences and retell the ideas.",
            "other": "Note three key ideas and collect vocabulary and questions from the transcript.",
            "business": "Map business ideas and cases, and note assumptions to test.",
            "technology": "Record key computing concepts and questions using the transcript.",
            "japanese": "Collect Japanese vocabulary and everyday expressions, then practise retelling."
      }
},
    guestPreview: 'Guests can preview the first {count} videos. Sign in to view all videos.',
    viewMore: 'View more',
    title: 'Curated YouTube AI Videos',
    subtitle: 'Learn AI through quality videos. Build your English with transcripts, shadowing and study notes.',
    pasteHint: 'Paste a YouTube link. We\'ll generate shadowing practice instantly.',
    generatePractice: 'Generate Practice',
    analyzing: 'Analyzing video',
    extracting: 'Extracting subtitles',
    generating: 'Generating practice',

    systemNotReady: 'System not ready: yt-dlp not installed',
    installHint: 'Please install yt-dlp on the server',

    myLibrary: 'Learning Library',
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
    },

    search: {
      title: 'Search Videos',
      placeholder: 'Search keywords in video subtitles...',
      search: 'Search',
      searching: 'Searching...',
      results: 'Found {count} results',
      noResults: 'No matching results found',
      close: 'Close',
      esNotEnabled: 'Search service is not enabled. Please configure Elasticsearch first.',
      searchFailed: 'Search failed'
    }
  },

  settings: {
    title: 'Settings',
    developing: 'Settings feature is under development...'
  }
}