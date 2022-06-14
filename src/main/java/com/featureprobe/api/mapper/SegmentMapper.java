package com.featureprobe.api.mapper;

import com.featureprobe.api.dto.SegmentCreateRequest;
import com.featureprobe.api.dto.SegmentResponse;
import com.featureprobe.api.dto.SegmentUpdateRequest;
import com.featureprobe.api.dto.ToggleSegmentResponse;
import com.featureprobe.api.entity.Segment;
import com.featureprobe.api.entity.Toggle;
import com.featureprobe.api.model.SegmentRule;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Mapper
public interface SegmentMapper {

    SegmentMapper INSTANCE = Mappers.getMapper(SegmentMapper.class);

    @Mapping(target = "rules", expression = "java(toSegmentRulesString(createRequest.getRules()))")
    Segment requestToEntity(SegmentCreateRequest createRequest);

    @Mapping(target = "rules", expression = "java(toSegmentRules(segment.getRules()))")
    SegmentResponse entityToResponse(Segment segment);

    ToggleSegmentResponse toggleToToggleSegment(Toggle toggle);

    @Mapping(target = "rules", expression = "java(toSegmentRulesString(updateRequest.getRules()))")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void mapEntity(SegmentUpdateRequest updateRequest, @MappingTarget Segment segment);

    default String toSegmentRulesString(List<SegmentRule> rules) {
        if (!CollectionUtils.isEmpty(rules)) {
            return JsonMapper.toJSONString(rules);
        }
        return toDefaultSegmentRulesString();
    }

    default List<SegmentRule> toSegmentRules(String rules) {
        if (StringUtils.isNotBlank(rules)) {
            return JsonMapper.toObject(rules, List.class);
        }
        return toDefaultSegmentRules();
    }

    default String toDefaultSegmentRulesString() {
        return JsonMapper.toJSONString(Collections.emptyList());
    }

    default List<SegmentRule> toDefaultSegmentRules() {
        return Collections.emptyList();
    }

}
