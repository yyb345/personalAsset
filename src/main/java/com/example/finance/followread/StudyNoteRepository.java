package com.example.finance.followread;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {
    List<StudyNote> findByVideoIdAndUserIdOrderByUpdatedAtDesc(String videoId, Long userId);
    List<StudyNote> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
