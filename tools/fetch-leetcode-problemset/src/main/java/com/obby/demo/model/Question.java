package com.obby.demo.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Question {
    private String titleSlug;
    private String title;
    private String content;
    private String translatedTitle;
    private String translatedContent;
    private Boolean isPaidOnly;
    private String difficulty;
    private List<Question> similarQuestions;
    private List<TopicTag> topicTags;
    private List<CodeSnippet> codeSnippets;
    private Stats stats;
    private List<String> hints;
    private List<Solution> solutions;

    @Data
    public static class TopicTag {
        private String name;
        private String slug;
        private String translatedName;
    }

    @Data
    public static class CodeSnippet {
        private String lang;
        private String langSlug;
        private String code;
    }

    @Data
    public static class Stats {
        private String totalAccepted;
        private String totalSubmission;
        private Long totalAcceptedRaw;
        private Long totalSubmissionRaw;
        private String acRate;
    }

    @Data
    public static class Solution {
        private String uuid;
        private String title;
        private String slug;
        private String chargeType;
        private String identifier;
        private List<Tag> tags;
        private Date createdAt;
        private String summary;
        private Boolean byLeetcode;
        private String content;
        private String questionTitleSlug;

        @Data
        public static class Tag {
            private String name;
            private String nameTranslated;
            private String slug;
        }
    }
}
