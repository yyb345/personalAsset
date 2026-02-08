package com.example.finance.followread;

import com.example.finance.User;
import com.example.finance.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/study-notes")
@CrossOrigin(origins = "*")
public class StudyNoteController {

    @Autowired
    private StudyNoteService studyNoteService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createNote(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String videoId = request.get("videoId");
            String title = request.get("title");
            String contentJson = request.get("contentJson");
            String contentHtml = request.get("contentHtml");

            if (videoId == null || videoId.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "videoId is required");
                return ResponseEntity.badRequest().body(error);
            }

            Long userId = getUserId(authentication);
            StudyNote note = studyNoteService.create(videoId, userId, title, contentJson, contentHtml);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("note", note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create note: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String contentJson = request.get("contentJson");
            String contentHtml = request.get("contentHtml");

            StudyNote note = studyNoteService.update(id, title, contentJson, contentHtml);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("note", note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update note: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNote(@PathVariable Long id) {
        try {
            Optional<StudyNote> noteOpt = studyNoteService.getById(id);
            if (!noteOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Note not found");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("note", noteOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<?> getNotesByVideo(@PathVariable String videoId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            List<StudyNote> notes = studyNoteService.getByVideoAndUser(videoId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("notes", notes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotes(Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            List<StudyNote> notes = studyNoteService.getByUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("notes", notes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        try {
            studyNoteService.delete(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Note deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete note: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private Long getUserId(Authentication authentication) {
        Long userId = 0L;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
            }
        }
        return userId;
    }
}
