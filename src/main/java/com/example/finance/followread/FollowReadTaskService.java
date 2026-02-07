package com.example.finance.followread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class FollowReadTaskService {

    private static final Logger log = LoggerFactory.getLogger(FollowReadTaskService.class);
    
    private static final String AUDIO_DIR = "uploads/audio/user/";
    
    @Autowired
    private FollowReadTaskRepository taskRepository;
    
    @Autowired
    private FollowReadTaskResultRepository resultRepository;
    
    @Autowired
    private FollowReadSentenceRepository sentenceRepository;
    
    public FollowReadTaskService() {
        // 确保目录存在
        new File(AUDIO_DIR).mkdirs();
    }
    
    /**
     * 创建跟读任务
     */
    @Transactional
    public FollowReadTask createTask(Long sentenceId, Long userId) {
        // 验证句子是否存在
        Optional<FollowReadSentence> sentenceOpt = sentenceRepository.findById(sentenceId);
        if (!sentenceOpt.isPresent()) {
            throw new RuntimeException("Sentence not found: " + sentenceId);
        }
        
        FollowReadTask task = new FollowReadTask();
        task.setSentenceId(sentenceId);
        task.setUserId(userId);
        task.setStatus("pending");
        
        return taskRepository.save(task);
    }
    
    /**
     * 提交录音并开始评估
     */
    @Transactional
    public FollowReadTask submitRecording(Long taskId, MultipartFile audioFile, Long userId) {
        Optional<FollowReadTask> taskOpt = taskRepository.findByIdAndUserId(taskId, userId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found or access denied");
        }
        
        FollowReadTask task = taskOpt.get();
        
        try {
            // 保存录音文件
            String fileName = String.format("user_%d_%d.wav", userId, System.currentTimeMillis());
            Path filePath = Paths.get(AUDIO_DIR + fileName);
            Files.copy(audioFile.getInputStream(), filePath);
            
            task.setAudioUrl("/" + AUDIO_DIR + fileName);
            task.setStatus("processing");
            task.setSubmittedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            // 异步处理评估
            evaluateRecordingAsync(taskId);
            
            return task;
        } catch (Exception e) {
            log.error("保存录音文件失败", e);
            task.setStatus("failed");
            task.setErrorMessage("Failed to save recording: " + e.getMessage());
            taskRepository.save(task);
            throw new RuntimeException("Failed to save recording", e);
        }
    }
    
    /**
     * 异步评估录音（目前使用模拟评分，后续可集成真实的语音评估服务）
     */
    @Async
    @Transactional
    public void evaluateRecordingAsync(Long taskId) {
        try {
            Optional<FollowReadTask> taskOpt = taskRepository.findById(taskId);
            if (!taskOpt.isPresent()) {
                log.error("Task not found: {}", taskId);
                return;
            }
            
            FollowReadTask task = taskOpt.get();
            
            // 获取句子信息
            Optional<FollowReadSentence> sentenceOpt = sentenceRepository.findById(task.getSentenceId());
            if (!sentenceOpt.isPresent()) {
                task.setStatus("failed");
                task.setErrorMessage("Sentence not found");
                taskRepository.save(task);
                return;
            }
            
            FollowReadSentence sentence = sentenceOpt.get();
            
            // 模拟评估过程（延迟1-2秒）
            Thread.sleep(1500);
            
            // 生成模拟评分
            Random random = new Random();
            int pronunciationScore = 70 + random.nextInt(25); // 70-95
            int fluencyScore = 65 + random.nextInt(30); // 65-95
            int intonationScore = 68 + random.nextInt(27); // 68-95
            int overallScore = (pronunciationScore + fluencyScore + intonationScore) / 3;
            
            task.setPronunciationScore(pronunciationScore);
            task.setFluencyScore(fluencyScore);
            task.setIntonationScore(intonationScore);
            task.setOverallScore(overallScore);
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            // 生成单词级别的反馈
            generateWordLevelFeedback(task, sentence);
            
            log.info("评估完成: taskId={}, overallScore={}", taskId, overallScore);
        } catch (Exception e) {
            log.error("评估录音失败: taskId={}", taskId, e);
            Optional<FollowReadTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                FollowReadTask task = taskOpt.get();
                task.setStatus("failed");
                task.setErrorMessage("Evaluation failed: " + e.getMessage());
                taskRepository.save(task);
            }
        }
    }
    
    /**
     * 生成单词级别的反馈
     */
    private void generateWordLevelFeedback(FollowReadTask task, FollowReadSentence sentence) {
        String[] words = sentence.getText().split("\\s+");
        Random random = new Random();
        
        List<FollowReadTaskResult> results = new ArrayList<>();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-zA-Z]", ""); // 移除标点
            if (word.isEmpty()) continue;
            
            FollowReadTaskResult result = new FollowReadTaskResult();
            result.setTaskId(task.getId());
            result.setWord(word);
            result.setWordPosition(i);
            
            // 模拟：80%的单词正确，20%需要改进
            boolean isCorrect = random.nextDouble() > 0.2;
            
            if (isCorrect) {
                result.setStatus("correct");
                result.setScore(85 + random.nextInt(15)); // 85-100
                result.setFeedback("Good pronunciation");
            } else {
                result.setStatus("incorrect");
                result.setScore(50 + random.nextInt(30)); // 50-80
                result.setFeedback("Pay attention to the pronunciation of '" + word + "'");
            }
            
            results.add(result);
        }
        
        resultRepository.saveAll(results);
    }
    
    /**
     * 获取任务详情（包含评分结果）
     */
    public FollowReadTask getTaskDetails(Long taskId, Long userId) {
        Optional<FollowReadTask> taskOpt = taskRepository.findByIdAndUserId(taskId, userId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found or access denied");
        }
        return taskOpt.get();
    }
    
    /**
     * 获取任务的单词级别结果
     */
    public List<FollowReadTaskResult> getTaskResults(Long taskId) {
        return resultRepository.findByTaskIdOrderByWordPosition(taskId);
    }
}

