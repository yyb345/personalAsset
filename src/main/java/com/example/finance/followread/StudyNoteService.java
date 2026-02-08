package com.example.finance.followread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StudyNoteService {

    @Autowired
    private StudyNoteRepository noteRepository;

    public StudyNote create(String videoId, Long userId, String title, String contentJson, String contentHtml) {
        StudyNote note = new StudyNote();
        note.setVideoId(videoId);
        note.setUserId(userId);
        note.setTitle(title != null ? title : "Untitled Note");
        note.setContentJson(contentJson);
        note.setContentHtml(contentHtml);
        return noteRepository.save(note);
    }

    public StudyNote update(Long id, String title, String contentJson, String contentHtml) {
        Optional<StudyNote> opt = noteRepository.findById(id);
        if (!opt.isPresent()) {
            throw new RuntimeException("Note not found");
        }
        StudyNote note = opt.get();
        if (title != null) note.setTitle(title);
        if (contentJson != null) note.setContentJson(contentJson);
        if (contentHtml != null) note.setContentHtml(contentHtml);
        return noteRepository.save(note);
    }

    public Optional<StudyNote> getById(Long id) {
        return noteRepository.findById(id);
    }

    public List<StudyNote> getByVideoAndUser(String videoId, Long userId) {
        return noteRepository.findByVideoIdAndUserIdOrderByUpdatedAtDesc(videoId, userId);
    }

    public List<StudyNote> getByUser(Long userId) {
        return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public void delete(Long id) {
        noteRepository.deleteById(id);
    }
}
