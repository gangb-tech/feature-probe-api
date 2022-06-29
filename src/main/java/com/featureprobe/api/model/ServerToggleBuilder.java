package com.featureprobe.api.model;

import com.featureprobe.api.base.exception.ServerToggleBuildException;
import com.featureprobe.api.entity.Segment;
import com.featureprobe.api.mapper.JsonMapper;
import com.featureprobe.sdk.server.model.Condition;
import com.featureprobe.sdk.server.model.ConditionType;
import com.featureprobe.sdk.server.model.Rule;
import com.featureprobe.sdk.server.model.Toggle;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerToggleBuilder {
    
    private static final String DATETIME_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";

    private Toggle toggle;
    private Variation.ValueConverter variationValueConverter;
    private TargetingContent targetingContent;
    private Map<String, Segment> segments;

    private static Map<String, Variation.ValueConverter<?>> converters = Maps.newHashMap();


    static {
        converters.put("string", value -> value);
        converters.put("boolean", value -> Boolean.valueOf(value));
        converters.put("json", value -> JsonMapper.toObject(value, Map.class));
        converters.put("number", value -> new BigDecimal(value));
    }

    public ServerToggleBuilder builder() {
        this.toggle = new Toggle();
        return this;
    }

    public ServerToggleBuilder key(String key) {
        toggle.setKey(key);
        return this;
    }

    public ServerToggleBuilder version(Long version) {
        toggle.setVersion(version);
        return this;
    }

    public ServerToggleBuilder disabled(boolean disabled) {
        toggle.setEnabled(!disabled);
        return this;
    }


    public ServerToggleBuilder forClient(Boolean forClient) {
        toggle.setForClient(forClient);
        return this;
    }

    public ServerToggleBuilder rules(String rules) {
        this.targetingContent = JsonMapper.toObject(rules, TargetingContent.class);
        return this;
    }

    public ServerToggleBuilder segments(Map<String, Segment> segments) {
        this.segments = segments;
        return this;
    }

    public ServerToggleBuilder returnType(String returnType) {
        this.variationValueConverter = getReturnTypeConverter(returnType);
        return this;
    }

    private Variation.ValueConverter<?> getReturnTypeConverter(String type) {
        if (converters.containsKey(type.toLowerCase())) {
            return converters.get(type.toLowerCase());
        }
        throw new ServerToggleBuildException("return type is unknown:" + type);
    }

    public Toggle build() {
        this.setDisabledServe();
        this.setDefaultServe();
        this.setVariations();
        this.setRules();
        return toggle;
    }

    private ServerToggleBuilder setDisabledServe() {
        if (targetingContent.getDisabledServe() == null) {
            throw new ServerToggleBuildException("disabled serve is null");
        }
        toggle.setDisabledServe(targetingContent.getDisabledServe().toServe());
        return this;
    }

    private void setDefaultServe() {
        if (targetingContent.getDefaultServe() != null) {
            toggle.setDefaultServe(targetingContent.getDefaultServe().toServe());
        }
        if (toggle.getDefaultServe() == null
                && BooleanUtils.isFalse(toggle.getEnabled())
                && targetingContent.getDisabledServe() != null) {
            toggle.setDefaultServe(targetingContent.getDisabledServe().toServe());
        }
        if (toggle.getDefaultServe() == null) {
            throw new ServerToggleBuildException("default serve is null");
        }
    }

    private void setVariations() {
        if (this.variationValueConverter == null) {
            throw new ServerToggleBuildException("return type not set");
        }
        List<Object> variations = targetingContent.getVariationObjectsByConverter(this.variationValueConverter);
        toggle.setVariations(variations);
    }


    private void setRules() {
        if (CollectionUtils.isEmpty(targetingContent.getRules())) {
            toggle.setRules(Collections.emptyList());
            return;
        }
        List<Rule> rules = targetingContent.getRules().stream().map(rule ->
                rule.toRule()).collect(Collectors.toList());
        rules.forEach(rule -> rule.getConditions().forEach(condition -> {
            if (condition.getType() == ConditionType.SEGMENT){
                replaceSegmentKeyToUniqueKey(condition);
            } else if (condition.getType() == ConditionType.DATETIME) {
                condition.setObjects(condition.getObjects().stream().map(datetime ->
                        translateUnix(datetime)).collect(Collectors.toList()));
            }
        }));
        toggle.setRules(rules);
    }

    private static String translateUnix(String datetime) {
        LocalDateTime time = LocalDateTime.parse(datetime.substring(0, 19), 
                DateTimeFormatter.ofPattern(DATETIME_FORMAT_PATTERN));
        Instant instant = time.toInstant(ZoneOffset.of(datetime.substring(19, datetime.length())));
        return String.valueOf(instant.toEpochMilli()/1000);
    }
    
    private void replaceSegmentKeyToUniqueKey(Condition condition) {
        condition.setObjects(condition.getObjects().stream().map(segmentKey ->
                segments.get(segmentKey).getUniqueKey()).collect(Collectors.toList()));
    }
}
