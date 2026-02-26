package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.QuestionGroupResponse;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.QuestionGroupItem;
import pl.co.assessment.entity.QuestionGroupVersion;
import pl.co.assessment.repository.QuestionGroupItemRepository;
import pl.co.assessment.repository.QuestionGroupVersionRepository;
import pl.co.assessment.service.QuestionGroupService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionGroupServiceImpl implements QuestionGroupService {

    private final QuestionGroupVersionRepository questionGroupVersionRepository;
    private final QuestionGroupItemRepository questionGroupItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionGroupResponse> buildGroups(List<ExamVersionQuestion> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }
        List<String> groupVersionIds = mappings.stream()
                .map(ExamVersionQuestion::getGroupVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupVersionIds.isEmpty()) {
            return List.of();
        }

        Map<String, String> questionVersionToQuestionId = new HashMap<>();
        Map<String, Integer> groupMinOrder = new HashMap<>();
        for (ExamVersionQuestion mapping : mappings) {
            if (mapping.getQuestionVersionId() != null) {
                questionVersionToQuestionId.put(mapping.getQuestionVersionId(), mapping.getQuestionId());
            }
            if (mapping.getGroupVersionId() != null) {
                Integer current = groupMinOrder.get(mapping.getGroupVersionId());
                Integer order = mapping.getQuestionOrder();
                if (order != null && (current == null || order < current)) {
                    groupMinOrder.put(mapping.getGroupVersionId(), order);
                }
            }
        }

        List<QuestionGroupVersion> groupVersions =
                questionGroupVersionRepository.findByIdInAndDeletedFalse(groupVersionIds);
        if (groupVersions.isEmpty()) {
            return List.of();
        }

        List<QuestionGroupItem> groupItems =
                questionGroupItemRepository.findByQuestionGroupVersionIdInAndDeletedFalseOrderByItemOrderAsc(groupVersionIds);
        Map<String, List<QuestionGroupItem>> itemsByVersion = groupItems.stream()
                .collect(Collectors.groupingBy(QuestionGroupItem::getQuestionGroupVersionId));

        List<QuestionGroupResponse> responses = new ArrayList<>();
        for (QuestionGroupVersion version : groupVersions) {
            List<QuestionGroupItem> items = itemsByVersion.getOrDefault(version.getId(), List.of());
            List<String> questionIds = items.stream()
                    .map(item -> questionVersionToQuestionId.get(item.getQuestionVersionId()))
                    .filter(Objects::nonNull)
                    .toList();
            responses.add(new QuestionGroupResponse(
                    version.getQuestionGroupId(),
                    version.getId(),
                    version.getPromptContent(),
                    questionIds
            ));
        }

        return responses.stream()
                .sorted(Comparator.comparingInt(response -> groupMinOrder.getOrDefault(response.getGroupVersionId(), Integer.MAX_VALUE)))
                .toList();
    }
}
