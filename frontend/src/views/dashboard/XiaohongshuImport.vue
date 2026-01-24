<template>
  <div class="youtube-import-container">
    <div class="header">
      <h1>小红书视频管理</h1>
      <p class="subtitle">管理小红书视频内容</p>
    </div>

    <!-- URL 输入区 -->
    <div class="import-section">
      <div class="url-input-card">
        <p class="hint">粘贴小红书视频链接，我们会自动获取视频信息</p>
        
        <div class="input-group">
          <div class="command-input-wrapper">
            <span class="command-prefix">▶</span>
            <input 
              v-model="videoUrl" 
              type="text" 
              placeholder="xiaohongshu.com/explore/..."
              class="url-input"
              @keyup.enter="addVideoToLibrary"
            />
          </div>
        </div>

        <button 
          class="parse-btn" 
          @click="addVideoToLibrary"
          :disabled="!videoUrl || isAdding"
        >
          <span v-if="!isAdding">添加视频</span>
          <span v-else class="ai-processing">
            <span class="processing-text">
              <span class="stage">正在获取视频信息</span>
            </span>
          </span>
        </button>

        <!-- 系统状态检查 -->
        <div v-if="!systemReady" class="warning-box">
          <p>⚠️ 系统未就绪：yt-dlp 未安装</p>
          <p class="hint-text">请在服务器上安装 yt-dlp: <code>pip install yt-dlp</code></p>
        </div>
      </div>

      <!-- 我的视频列表 -->
      <div class="my-videos-section">
        <h2>我的视频库</h2>
        <div v-if="myVideos.length === 0" class="empty-state">
          <p>还没有视频</p>
        </div>
        <div v-else class="video-list">
          <div 
            v-for="video in myVideos" 
            :key="video.id"
            class="video-card"
          >
            <div class="video-thumbnail">
              <img v-if="video.thumbnailUrl" :src="video.thumbnailUrl" alt="thumbnail" />
              <div v-else class="thumbnail-placeholder">📹</div>
            </div>
            <div class="video-info">
              <h3 class="video-title">{{ video.title || 'Loading...' }}</h3>
              <div class="video-meta">
                <span class="channel">{{ video.author }}</span>
                <span class="duration">{{ formatDuration(video.duration) }}</span>
              </div>
              <div class="video-status-row">
                <div class="video-status">
                  <span :class="['status-badge', video.status]">
                    {{ getStatusLabel(video.status) }}
                  </span>
                </div>
                <div class="video-actions">
                  <button 
                    class="action-btn download-btn" 
                    @click.stop="showDownloadOptions(video)"
                    title="下载视频"
                    :disabled="video.status !== 'completed'">
                    <Download :size="16" :stroke-width="2" />
                    <span class="kbd">D</span>
                  </button>
                  <button 
                    class="action-btn delete-btn" 
                    @click.stop="confirmDeleteVideo(video)"
                    title="删除视频">
                    <Trash2 :size="16" :stroke-width="2" />
                    <span class="kbd">⌫</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下载选项弹窗 -->
    <div v-if="showDownloadDialog" class="modal-overlay" @click="closeDownloadDialog">
      <div class="download-dialog" @click.stop>
        <div class="dialog-header">
          <h2>下载视频</h2>
          <button class="close-btn" @click="closeDownloadDialog">×</button>
        </div>
        
        <div class="dialog-content">
          <h3>{{ downloadingVideo?.title }}</h3>
          
          <div class="quality-selector">
            <label>视频质量</label>
            <select v-model="selectedQuality" class="quality-select">
              <option value="best">最佳质量 (Auto)</option>
              <option value="4k">4K (2160p)</option>
              <option value="2k">2K (1440p)</option>
              <option value="1080p">Full HD (1080p)</option>
              <option value="720p">HD (720p)</option>
              <option value="480p">SD (480p)</option>
            </select>
            <p class="quality-hint">提示：选择 1080p 适合学习，最佳质量适合存档</p>
          </div>
          
          <div class="download-options">
            <div class="option-card" @click="downloadWithQuality('video')">
              <div class="option-icon">🎬</div>
              <div class="option-info">
                <h4>下载视频</h4>
                <p>{{ getQualityDescription(selectedQuality) }}</p>
              </div>
            </div>
            
            <div class="option-card" @click="quickDownload('audio')">
              <div class="option-icon">🎵</div>
              <div class="option-info">
                <h4>仅音频 (MP3)</h4>
                <p>提取并转换为 MP3 格式</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下载任务列表弹窗 -->
    <div v-if="showDownloadTasks" class="modal-overlay" @click="showDownloadTasks = false">
      <div class="download-tasks-dialog" @click.stop>
        <div class="dialog-header">
          <h2>我的下载</h2>
          <button class="close-btn" @click="showDownloadTasks = false">×</button>
        </div>
        
        <div class="dialog-content">
          <div v-if="downloadTasks.length === 0" class="empty-state">
            <p>暂无下载任务</p>
          </div>
          
          <div v-else class="task-list">
            <div 
              v-for="task in downloadTasks" 
              :key="task.id"
              class="task-item"
            >
              <div class="task-info">
                <h4>{{ getVideoTitle(task.xiaohongshuVideoId) }}</h4>
                <div class="task-meta">
                  <span :class="['status-badge', task.status.toLowerCase()]">
                    {{ getDownloadStatusLabel(task.status) }}
                  </span>
                  <span class="task-type">{{ getDownloadTypeLabel(task.downloadType) }}</span>
                  <span v-if="task.quality" class="quality">{{ task.quality }}</span>
                </div>
                <div v-if="task.status === 'DOWNLOADING'" class="progress-bar">
                  <div class="progress-fill" :style="{ width: task.progress + '%' }"></div>
                </div>
                <p class="progress-text">{{ task.progressMessage }}</p>
                <p v-if="task.downloadSpeed" class="speed">{{ task.downloadSpeed }}</p>
              </div>
              
              <div class="task-actions">
                <button 
                  v-if="task.status === 'SUCCESS'" 
                  class="btn-download-file"
                  @click="downloadFile(task.id)">
                  💾 下载
                </button>
                <button 
                  class="btn-delete-task"
                  @click="deleteDownloadTask(task.id)">
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 浮动下载按钮 -->
    <button 
      class="fab-download" 
      @click="showDownloadTasks = true"
      title="查看下载任务">
      📥
      <span v-if="activeDownloadCount > 0" class="download-badge">{{ activeDownloadCount }}</span>
    </button>
  </div>
</template>

<script>
import axios from '../../utils/axios';
import { Trash2, Download } from 'lucide-vue-next';

export default {
  name: 'XiaohongshuImport',
  components: {
    Trash2,
    Download
  },
  data() {
    return {
      videoUrl: '',
      systemReady: true,
      isAdding: false,
      
      myVideos: [],
      
      videoListPollInterval: null,
      
      // 下载相关
      showDownloadDialog: false,
      downloadingVideo: null,
      downloadTasks: [],
      showDownloadTasks: false,
      downloadPollInterval: null,
      activeDownloadCount: 0,
      selectedQuality: 'best'
    };
  },
  mounted() {
    this.checkSystemStatus();
    this.loadMyVideos();
    this.startVideoListPolling();
    this.loadDownloadTasks();
    this.startDownloadPolling();
  },
  beforeUnmount() {
    if (this.videoListPollInterval) {
      clearInterval(this.videoListPollInterval);
    }
    if (this.downloadPollInterval) {
      clearInterval(this.downloadPollInterval);
    }
  },
  methods: {
    async checkSystemStatus() {
      try {
        const response = await axios.get('/api/xiaohongshu/status');
        this.systemReady = response.data.ready;
      } catch (error) {
        console.error('检查系统状态失败:', error);
        this.systemReady = false;
      }
    },
    
    async loadMyVideos() {
      try {
        const response = await axios.get('/api/xiaohongshu/videos');
        this.myVideos = response.data;
      } catch (error) {
        console.error('加载视频列表失败:', error);
      }
    },
    
    startVideoListPolling() {
      // 每3秒轮询一次视频列表，以更新状态
      this.videoListPollInterval = setInterval(() => {
        this.loadMyVideos();
      }, 3000);
    },
    
    async addVideoToLibrary() {
      if (!this.videoUrl) {
        alert('请输入小红书视频链接');
        return;
      }
      
      if (!this.systemReady) {
        alert('系统未就绪，请先安装 yt-dlp');
        return;
      }
      
      this.isAdding = true;
      
      try {
        const response = await axios.post('/api/xiaohongshu/add', {
          url: this.videoUrl,
          difficulty: 'auto'
        });
        
        if (response.data.success) {
          // 刷新视频列表
          this.loadMyVideos();
        }
      } catch (error) {
        console.error('添加视频失败:', error);
        alert(error.response?.data?.error || '添加视频失败，请检查链接');
      } finally {
        this.isAdding = false;
      }
    },
    
    formatDuration(seconds) {
      if (!seconds) return '--:--';
      const mins = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    },
    
    getStatusLabel(status) {
      const labels = {
        'added': '已添加',
        'completed': '已完成',
        'failed': '失败'
      };
      return labels[status] || status;
    },
    
    confirmDeleteVideo(video) {
      const title = video.title || '此视频';
      
      if (confirm(`确定删除 "${title}"?\n\n此操作无法撤销！`)) {
        this.deleteVideo(video.id);
      }
    },
    
    async deleteVideo(videoId) {
      try {
        await axios.delete(`/api/xiaohongshu/videos/${videoId}`);
        
        // 从列表中移除
        this.myVideos = this.myVideos.filter(v => v.id !== videoId);
        
        alert('视频删除成功');
      } catch (error) {
        console.error('删除视频失败:', error);
        alert('删除失败: ' + (error.response?.data?.error || '请重试'));
      }
    },
    
    // ========== 下载相关方法 ==========
    
    showDownloadOptions(video) {
      if (video.status === 'failed') {
        alert('视频解析失败，无法下载');
        return;
      }
      this.downloadingVideo = video;
      this.showDownloadDialog = true;
    },
    
    closeDownloadDialog() {
      this.showDownloadDialog = false;
      this.downloadingVideo = null;
      this.selectedQuality = 'best';
    },
    
    async downloadWithQuality(type) {
      try {
        const response = await axios.post('/api/xiaohongshu/download/start', {
          videoId: this.downloadingVideo.id,
          downloadType: type,
          quality: this.selectedQuality,
          formatId: null
        });
        
        if (response.data.success) {
          const qualityText = this.getQualityText(this.selectedQuality);
          alert(`✅ 下载任务已创建！\n质量: ${qualityText}\n请在右下角查看进度`);
          this.closeDownloadDialog();
          this.loadDownloadTasks();
        }
      } catch (error) {
        console.error('创建下载任务失败:', error);
        alert('下载失败: ' + (error.response?.data?.error || '请重试'));
      }
    },
    
    async quickDownload(type) {
      try {
        const response = await axios.post(`/api/xiaohongshu/download/quick/${this.downloadingVideo.id}`, null, {
          params: { type }
        });
        
        if (response.data.success) {
          alert('下载任务已创建！请在右下角查看进度');
          this.closeDownloadDialog();
          this.loadDownloadTasks();
        }
      } catch (error) {
        console.error('创建下载任务失败:', error);
        alert('下载失败: ' + (error.response?.data?.error || '请重试'));
      }
    },
    
    getQualityText(quality) {
      const labels = {
        'best': '最佳质量 (Auto)',
        '4k': '4K (2160p)',
        '2k': '2K (1440p)',
        '1080p': 'Full HD (1080p)',
        '720p': 'HD (720p)',
        '480p': 'SD (480p)'
      };
      return labels[quality] || quality;
    },
    
    getQualityDescription(quality) {
      const descriptions = {
        'best': '自动选择最高可用质量',
        '4k': '超高清视频，文件较大',
        '2k': '2K 高清视频',
        '1080p': '全高清视频，推荐',
        '720p': '高清视频，文件适中',
        '480p': '标清视频，文件较小'
      };
      return descriptions[quality] || '下载视频文件';
    },
    
    async loadDownloadTasks() {
      try {
        const response = await axios.get('/api/xiaohongshu/download/tasks');
        this.downloadTasks = response.data.tasks || [];
        this.updateActiveDownloadCount();
      } catch (error) {
        console.error('加载下载任务失败:', error);
      }
    },
    
    startDownloadPolling() {
      // 每3秒轮询下载任务状态
      this.downloadPollInterval = setInterval(() => {
        this.loadDownloadTasks();
      }, 3000);
    },
    
    updateActiveDownloadCount() {
      this.activeDownloadCount = this.downloadTasks.filter(
        t => t.status === 'DOWNLOADING' || t.status === 'PARSING'
      ).length;
    },
    
    async downloadFile(taskId) {
      try {
        window.location.href = `/api/xiaohongshu/download/file/${taskId}`;
      } catch (error) {
        console.error('下载文件失败:', error);
        alert('下载失败，请重试');
      }
    },
    
    async deleteDownloadTask(taskId) {
      if (!confirm('确定删除此下载任务？下载的文件也会被删除。')) {
        return;
      }
      
      try {
        await axios.delete(`/api/xiaohongshu/download/tasks/${taskId}`);
        this.loadDownloadTasks();
        alert('下载任务已删除');
      } catch (error) {
        console.error('删除下载任务失败:', error);
        alert('删除失败: ' + (error.response?.data?.error || '请重试'));
      }
    },
    
    getDownloadStatusLabel(status) {
      const labels = {
        'INIT': '等待中',
        'PARSING': '解析中',
        'DOWNLOADING': '下载中',
        'MERGING': '合并中',
        'SUCCESS': '已完成',
        'FAILED': '失败'
      };
      return labels[status] || status;
    },
    
    getDownloadTypeLabel(type) {
      const labels = {
        'video': '视频',
        'audio': '音频',
        'video_audio': '视频+音频'
      };
      return labels[type] || type;
    },
    
    getVideoTitle(videoId) {
      const video = this.myVideos.find(v => v.id === videoId);
      return video ? video.title : '未知视频';
    }
  }
};
</script>

<style>
@import '../../assets/styles/youtube-import.css';
</style>

