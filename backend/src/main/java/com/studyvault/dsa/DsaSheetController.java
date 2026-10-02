package com.studyvault.dsa;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.security.SecurityUser;
import com.studyvault.security.SecurityUtils;
import com.studyvault.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dsa-sheet")
@RequiredArgsConstructor
public class DsaSheetController {

    private final DsaTopicRepository topics;
    private final DsaQuestionRepository questions;
    private final DsaProgressRepository progress;
    private final UserRepository users;

    public record QuestionDto(Integer id, int position, String title, String slug,
                              String difficulty, String leetcodeUrl) {
    }

    public record TopicDto(Integer id, String name, String slug, List<QuestionDto> questions) {
    }

    public record SheetDto(int total, List<TopicDto> topics) {
    }

    /** The full sheet - public, no auth needed. */
    @GetMapping
    public ApiResponse<SheetDto> sheet() {
        List<TopicDto> topicDtos = topics.findAllByOrderBySortOrderAsc().stream()
                .map(t -> new TopicDto(t.getId(), t.getName(), t.getSlug(),
                        questions.findAllByTopicIdOrderByPositionAsc(t.getId()).stream()
                                .map(q -> new QuestionDto(q.getId(), q.getPosition(), q.getTitle(),
                                        q.getSlug(), q.getDifficulty().name(), q.getLeetcodeUrl()))
                                .toList()))
                .toList();
        int total = topicDtos.stream().mapToInt(t -> t.questions().size()).sum();
        return ApiResponse.ok(new SheetDto(total, topicDtos));
    }

    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> myProgress() {
        SecurityUser su = SecurityUtils.requireUser();
        List<Integer> done = progress.findDoneQuestionIds(su.id());
        long total = questions.count();
        Map<String, Object> result = new HashMap<>();
        result.put("doneIds", done);
        result.put("done", done.size());
        result.put("total", total);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{questionId}/toggle")
    @Transactional
    public ApiResponse<Map<String, Object>> toggle(@PathVariable Integer questionId) {
        SecurityUser su = SecurityUtils.requireUser();
        questions.findById(questionId).orElseThrow(() -> new NotFoundException("Question not found."));
        DsaProgress row = progress.findByUserIdAndQuestionId(su.id(), questionId).orElse(null);
        boolean nowDone;
        if (row == null) {
            DsaProgress created = new DsaProgress();
            created.setUser(users.getReferenceById(su.id()));
            created.setQuestionId(questionId);
            created.setDone(true);
            progress.save(created);
            nowDone = true;
        } else {
            row.setDone(!row.isDone());
            progress.save(row);
            nowDone = row.isDone();
        }
        List<Integer> done = progress.findDoneQuestionIds(su.id());
        Map<String, Object> result = new HashMap<>();
        result.put("questionId", questionId);
        result.put("done", nowDone);
        result.put("doneCount", done.size());
        return ApiResponse.ok(result);
    }

    // ------------------------------------------------------------------ admin

    public record AddQuestionRequest(@NotBlank @Size(max = 200) String title,
                                     @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String slug,
                                     @NotBlank String difficulty,
                                     @NotNull Integer topicId,
                                     String leetcodeUrl) {
    }

    @PostMapping("/admin/questions")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestionDto> addQuestion(@Valid @RequestBody AddQuestionRequest request) {
        DsaTopic topic = topics.findById(request.topicId())
                .orElseThrow(() -> new BadRequestException("Topic not found."));
        if (questions.findBySlug(request.slug()).isPresent()) {
            throw new BadRequestException("A question with this slug already exists.");
        }
        DsaQuestion q = new DsaQuestion();
        q.setTopic(topic);
        q.setPosition(questions.findAllByTopicIdOrderByPositionAsc(topic.getId()).size() + 1);
        q.setTitle(request.title().trim());
        q.setSlug(request.slug());
        try {
            q.setDifficulty(DsaQuestion.Difficulty.valueOf(request.difficulty().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("difficulty must be EASY, MEDIUM or HARD.");
        }
        q.setLeetcodeUrl(request.leetcodeUrl());
        DsaQuestion saved = questions.save(q);
        return ApiResponse.ok(new QuestionDto(saved.getId(), saved.getPosition(), saved.getTitle(),
                saved.getSlug(), saved.getDifficulty().name(), saved.getLeetcodeUrl()), "Question added to the sheet.");
    }

    @DeleteMapping("/admin/questions/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteQuestion(@PathVariable Integer id) {
        if (!questions.existsById(id)) {
            throw new NotFoundException("Question not found.");
        }
        questions.deleteById(id);
        return ApiResponse.message("Question removed from the sheet.");
    }
}
