package com.obby.demo;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.obby.demo.model.Question;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class FetchLeetcodeProblemsetApplication {
    private static final Logger logger = LoggerFactory.getLogger(FetchLeetcodeProblemsetApplication.class);

    private final RestTemplate restTemplate = new RestTemplateBuilder().build();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        SpringApplication.run(FetchLeetcodeProblemsetApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() throws Exception {
        return args -> {
            logger.info("Starting...");
            logger.info("Question slugs fetching...");

            List<String> questionSlugs = fetchQuestionSlugs();

            logger.info("Question slugs fetched.");
            logger.info("Questions count: " + questionSlugs.size() + ".");

            for (int i = 0; i < questionSlugs.size(); i++) {
                logger.info("Question: " + (i + 1) + "/" + questionSlugs.size() + ".");

                String questionSlug = questionSlugs.get(i);

                logger.info("Question " + "[" + questionSlug + "] fetching...");

                Question question = fetchQuestion(questionSlug);

                logger.info("Question " + "[" + questionSlug + "] fetched.");
                logger.info("Question " + "[" + questionSlug + "] solution slugs fetching...");

                List<String> solutionSlugs = fetchSolutionSlugs(questionSlug);

                logger.info("Question " + "[" + questionSlug + "] solution slugs fetched.");
                logger.info("Question " + "[" + questionSlug + "] solutions count: " + solutionSlugs.size() + ".");

                question.setSolutions(new ArrayList<>());

                for (int j = 0; j < solutionSlugs.size(); j++) {
                    logger.info("Solution: " + (j + 1) + "/" + solutionSlugs.size() + "."
                            + "    Question: " + (i + 1) + "/" + questionSlugs.size() + ".");

                    String solutionSlug = solutionSlugs.get(j);

                    logger.info("Solution [" + solutionSlug + "] fetching...");

                    Question.Solution solution = fetchSolution(questionSlug, solutionSlug);

                    logger.info("Solution [" + solutionSlug + "] fetched.");

                    question.getSolutions().add(solution);
                }

                Files.writeString(
                        basePath("result/" + questionSlug + ".json"),
                        gson.toJson(question)
                );
            }

            logger.info("Finished.");
        };
    }

    /**
     * Fetch all question slugs.
     *
     * @return all question slugs
     * @throws Exception exception
     */
    List<String> fetchQuestionSlugs() throws Exception {
        JsonObject body = gson.fromJson(
                restTemplate.getForEntity("https://leetcode-cn.com/api/problems/all/", String.class)
                        .getBody(),
                JsonObject.class
        );

        Files.writeString(
                basePath("response/questions.json"),
                gson.toJson(body)
        );

        JsonArray data = body.getAsJsonArray("stat_status_pairs");
        List<String> slugs = new ArrayList<>();

        for (JsonElement obj : data) {
            if (obj.getAsJsonObject().get("paid_only").getAsBoolean()) {
                continue;
            }

            slugs.add(obj.getAsJsonObject().getAsJsonObject("stat").get("question__title_slug").getAsString());
        }

        return slugs;
    }

    /**
     * Fetch a question by the slug.
     *
     * @param slug question slug
     * @return question
     * @throws Exception exception
     */
    Question fetchQuestion(String slug) throws Exception {
        JsonObject body = gson.fromJson(
                restTemplate.exchange(
                        RequestEntity.post("https://leetcode-cn.com/graphql/")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .header(HttpHeaders.ORIGIN, "https://leetcode-cn.com")
                                .header(HttpHeaders.REFERER, "https://leetcode-cn.com/problems/" + slug + "/")
                                .body(ImmutableMap.builder()
                                        .put("operationName", "questionData")
                                        .put("query", "query questionData($titleSlug: String!) {\n" +
                                                "  question(titleSlug: $titleSlug) {\n" +
                                                "    questionId\n" +
                                                "    questionFrontendId\n" +
                                                "    boundTopicId\n" +
                                                "    title\n" +
                                                "    titleSlug\n" +
                                                "    content\n" +
                                                "    translatedTitle\n" +
                                                "    translatedContent\n" +
                                                "    isPaidOnly\n" +
                                                "    difficulty\n" +
                                                "    likes\n" +
                                                "    dislikes\n" +
                                                "    isLiked\n" +
                                                "    similarQuestions\n" +
                                                "    contributors {\n" +
                                                "      username\n" +
                                                "      profileUrl\n" +
                                                "      avatarUrl\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    langToValidPlayground\n" +
                                                "    topicTags {\n" +
                                                "      name\n" +
                                                "      slug\n" +
                                                "      translatedName\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    companyTagStats\n" +
                                                "    codeSnippets {\n" +
                                                "      lang\n" +
                                                "      langSlug\n" +
                                                "      code\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    stats\n" +
                                                "    hints\n" +
                                                "    solution {\n" +
                                                "      id\n" +
                                                "      canSeeDetail\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    status\n" +
                                                "    sampleTestCase\n" +
                                                "    metaData\n" +
                                                "    judgerAvailable\n" +
                                                "    judgeType\n" +
                                                "    mysqlSchemas\n" +
                                                "    enableRunCode\n" +
                                                "    envInfo\n" +
                                                "    book {\n" +
                                                "      id\n" +
                                                "      bookName\n" +
                                                "      pressName\n" +
                                                "      source\n" +
                                                "      shortDescription\n" +
                                                "      fullDescription\n" +
                                                "      bookImgUrl\n" +
                                                "      pressImgUrl\n" +
                                                "      productUrl\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    isSubscribed\n" +
                                                "    isDailyQuestion\n" +
                                                "    dailyRecordStatus\n" +
                                                "    editorType\n" +
                                                "    ugcQuestionId\n" +
                                                "    style\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "}")
                                        .put("variables", ImmutableMap.of("titleSlug", slug))
                                        .build()),
                        String.class
                ).getBody(),
                JsonObject.class
        );

        Files.writeString(
                basePath("response/questions/" + slug + "/question.json"),
                gson.toJson(body)
        );

        JsonObject data = body.getAsJsonObject("data").getAsJsonObject("question");

        data.add("similarQuestions", gson.fromJson(data.get("similarQuestions").getAsString(), JsonArray.class));
        data.add("stats", gson.fromJson(data.get("stats").getAsString(), JsonObject.class));

        return gson.fromJson(data, Question.class);
    }

    /**
     * Fetch all solution slugs by the question slug.
     *
     * @param questionSlug question slug
     * @return all solution slugs
     * @throws Exception exception
     */
    List<String> fetchSolutionSlugs(String questionSlug) throws Exception {
        JsonObject body = gson.fromJson(
                restTemplate.exchange(
                        RequestEntity.post("https://leetcode-cn.com/graphql/")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .header(HttpHeaders.ORIGIN, "https://leetcode-cn.com")
                                .header(
                                        HttpHeaders.REFERER,
                                        "https://leetcode-cn.com/problems/" + questionSlug + "/solution/"
                                )
                                .body(ImmutableMap.builder()
                                        .put("operationName", "questionSolutionArticles")
                                        .put("query", "query questionSolutionArticles($questionSlug: String!, $skip: Int, $first: Int, $orderBy: SolutionArticleOrderBy, $userInput: String, $tagSlugs: [String!]) {\n" +
                                                "  questionSolutionArticles(questionSlug: $questionSlug, skip: $skip, first: $first, orderBy: $orderBy, userInput: $userInput, tagSlugs: $tagSlugs) {\n" +
                                                "    totalNum\n" +
                                                "    edges {\n" +
                                                "      node {\n" +
                                                "        ...solutionArticle\n" +
                                                "        __typename\n" +
                                                "      }\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "}\n" +
                                                "\n" +
                                                "fragment solutionArticle on SolutionArticleNode {\n" +
                                                "  uuid\n" +
                                                "  title\n" +
                                                "  slug\n" +
                                                "  sunk\n" +
                                                "  chargeType\n" +
                                                "  status\n" +
                                                "  identifier\n" +
                                                "  canEdit\n" +
                                                "  reactionType\n" +
                                                "  reactionsV2 {\n" +
                                                "    count\n" +
                                                "    reactionType\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  tags {\n" +
                                                "    name\n" +
                                                "    nameTranslated\n" +
                                                "    slug\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  createdAt\n" +
                                                "  thumbnail\n" +
                                                "  author {\n" +
                                                "    username\n" +
                                                "    profile {\n" +
                                                "      userAvatar\n" +
                                                "      userSlug\n" +
                                                "      realName\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  summary\n" +
                                                "  topic {\n" +
                                                "    id\n" +
                                                "    commentCount\n" +
                                                "    viewCount\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  byLeetcode\n" +
                                                "  isMyFavorite\n" +
                                                "  isMostPopular\n" +
                                                "  isEditorsPick\n" +
                                                "  hitCount\n" +
                                                "  videosInfo {\n" +
                                                "    videoId\n" +
                                                "    coverUrl\n" +
                                                "    duration\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  __typename\n" +
                                                "}")
                                        .put("variables", ImmutableMap.builder()
                                                .put("questionSlug", questionSlug)
                                                // .put("first", 10).put("skip", 0)
                                                .put("orderBy", "DEFAULT").build())
                                        .build()),
                        String.class
                ).getBody(),
                JsonObject.class
        );

        Files.writeString(
                basePath("response/questions/" + questionSlug + "/solutions.json"),
                gson.toJson(body)
        );

        JsonArray data = body.getAsJsonObject("data")
                .getAsJsonObject("questionSolutionArticles")
                .getAsJsonArray("edges");
        List<String> slugs = new ArrayList<>();

        for (JsonElement obj : data) {
            slugs.add(obj.getAsJsonObject().getAsJsonObject("node").get("slug").getAsString());
        }

        return slugs;
    }

    /**
     * Fetch a solution by the question slug and the solution slug.
     *
     * @param questionSlug question slug
     * @param slug         solution slug
     * @return solution
     * @throws Exception exception
     */
    Question.Solution fetchSolution(String questionSlug, String slug) throws Exception {
        JsonObject body = gson.fromJson(
                restTemplate.exchange(
                        RequestEntity.post("https://leetcode-cn.com/graphql/")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .header(HttpHeaders.ORIGIN, "https://leetcode-cn.com")
                                .header(
                                        HttpHeaders.REFERER,
                                        "https://leetcode-cn.com/problems/" + questionSlug + "/solution/" + slug + "/"
                                )
                                .body(ImmutableMap.builder()
                                        .put("operationName", "solutionDetailArticle")
                                        .put("query", "query solutionDetailArticle($slug: String!, $orderBy: SolutionArticleOrderBy!) {\n" +
                                                "  solutionArticle(slug: $slug, orderBy: $orderBy) {\n" +
                                                "    ...solutionArticle\n" +
                                                "    content\n" +
                                                "    question {\n" +
                                                "      questionTitleSlug\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    position\n" +
                                                "    next {\n" +
                                                "      slug\n" +
                                                "      title\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    prev {\n" +
                                                "      slug\n" +
                                                "      title\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "}\n" +
                                                "\n" +
                                                "fragment solutionArticle on SolutionArticleNode {\n" +
                                                "  uuid\n" +
                                                "  title\n" +
                                                "  slug\n" +
                                                "  sunk\n" +
                                                "  chargeType\n" +
                                                "  status\n" +
                                                "  identifier\n" +
                                                "  canEdit\n" +
                                                "  reactionType\n" +
                                                "  reactionsV2 {\n" +
                                                "    count\n" +
                                                "    reactionType\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  tags {\n" +
                                                "    name\n" +
                                                "    nameTranslated\n" +
                                                "    slug\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  createdAt\n" +
                                                "  thumbnail\n" +
                                                "  author {\n" +
                                                "    username\n" +
                                                "    profile {\n" +
                                                "      userAvatar\n" +
                                                "      userSlug\n" +
                                                "      realName\n" +
                                                "      __typename\n" +
                                                "    }\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  summary\n" +
                                                "  topic {\n" +
                                                "    id\n" +
                                                "    commentCount\n" +
                                                "    viewCount\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  byLeetcode\n" +
                                                "  isMyFavorite\n" +
                                                "  isMostPopular\n" +
                                                "  isEditorsPick\n" +
                                                "  hitCount\n" +
                                                "  videosInfo {\n" +
                                                "    videoId\n" +
                                                "    coverUrl\n" +
                                                "    duration\n" +
                                                "    __typename\n" +
                                                "  }\n" +
                                                "  __typename\n" +
                                                "}")
                                        .put("variables", ImmutableMap.builder()
                                                .put("slug", slug)
                                                .put("orderBy", "DEFAULT")
                                                .build())
                                        .build()),
                        String.class
                ).getBody(),
                JsonObject.class
        );

        Files.writeString(
                basePath("response/questions/" + questionSlug + "/solutions/" + slug + ".json"),
                gson.toJson(body)
        );

        JsonObject data = body.getAsJsonObject("data").getAsJsonObject("solutionArticle");

        data.add("questionTitleSlug", data.getAsJsonObject("question").get("questionTitleSlug"));

        return gson.fromJson(data, Question.Solution.class);
    }

    Path basePath(String path) throws Exception {
        File file = new File("storage/" + path);

        FileUtils.forceMkdirParent(file);

        return file.toPath();
    }

}
