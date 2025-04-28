package com.team01.hrbank.dto.changelog;

// changeLogDetail 저장용과 응답용으로 모두 사용
public record DiffDto(
    String propertyName,
    String before,
    String after
) {

}
