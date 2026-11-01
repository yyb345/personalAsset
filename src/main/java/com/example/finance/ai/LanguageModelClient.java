package com.example.finance.ai;

/** Reusable model boundary, independent of video and persistence logic. */
public interface LanguageModelClient {
    String complete(String instruction, String content);
}
