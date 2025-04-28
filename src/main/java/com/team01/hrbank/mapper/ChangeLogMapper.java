package com.team01.hrbank.mapper;

import com.team01.hrbank.dto.changelog.ChangeLogDto;
import com.team01.hrbank.dto.changelog.DiffDto;
import com.team01.hrbank.entity.ChangeLog;
import com.team01.hrbank.entity.ChangeLogDetail;
import org.springframework.stereotype.Component;

@Component
public class ChangeLogMapper {
    public ChangeLogDto toDto(ChangeLog changeLog) {
        return new ChangeLogDto(
            changeLog.getId(),
            changeLog.getType(),
            changeLog.getEmployeeNumber(),
            changeLog.getMemo(),
            changeLog.getIpAddress(),
            changeLog.getUpdatedAt()
        );
    }

    // TODO: 나중에 사용할 예정
    public DiffDto toDiffDto(ChangeLogDetail changeLogDetail) {
        return new DiffDto(
            changeLogDetail.getPropertyName(),
            changeLogDetail.getBefore(),
            changeLogDetail.getAfter()
        );
    }
}
