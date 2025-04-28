package com.team01.hrbank.service.impl;

import com.team01.hrbank.dto.backup.BackupDto;
import com.team01.hrbank.dto.backup.BackupPageDto;
import com.team01.hrbank.dto.backup.CursorRequest;
import com.team01.hrbank.entity.Backup;
import com.team01.hrbank.entity.BaseEntity;
import com.team01.hrbank.entity.BinaryContent;
import com.team01.hrbank.entity.Employee;
import com.team01.hrbank.enums.BackupStatus;
import com.team01.hrbank.exception.BackupFailedException;
import com.team01.hrbank.mapper.BackupMapper;
import com.team01.hrbank.repository.BackupRepository;
import com.team01.hrbank.repository.ChangeLogRepository;
import com.team01.hrbank.repository.EmployeeRepository;
import com.team01.hrbank.service.BackupService;
import com.team01.hrbank.storage.CsvBackupStorage;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupRepository backUpRepository;
    private final EmployeeRepository employeeRepository;
    private final CsvBackupStorage csvBackupStorage;
    private final BackupMapper backUpMapper;
    private final ChangeLogRepository changeLogRepository;
    private final ApplicationContext applicationContext;

    private BackupServiceImpl getSelf() {
        return applicationContext.getBean(BackupServiceImpl.class);
    }

    @Override
    @Scheduled(cron = "${spring.backup.schedule.time}")
    public void run() {
        Backup runBackup = null;
        try {
            final String worker = "system";
            if (!needLogForScheduler()) {
                getSelf().skippedSystemBackup();
                return;
            }
            runBackup = getSelf().startBackup(worker);
            getSelf().registerBackup(runBackup);
        } catch (Exception e) {

            if (runBackup != null) {
                try {
                    getSelf().handleBackupFailure(runBackup.getId(), e);
                } catch (Exception failureHandlerEx) {
                    failureHandlerEx.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }
    }

    @Transactional
    @Override
    public BackupDto triggerManualBackup(String workerIp) {
        if (!lastedBackup()) {
            Backup skipped = getSelf().skippedManualBackup(workerIp);
            return backUpMapper.toDto(skipped);
        }

        Backup initialBackup = null;
        try {
            final String worker = workerIp;
            initialBackup = getSelf().startBackup(worker);
            getSelf().registerBackup(initialBackup);

            Backup finalBackup = findBackupByIdOrThrow(initialBackup.getId());
            return backUpMapper.toDto(finalBackup);

        } catch (Exception e) {
            if (initialBackup != null) {
                Long failedBackupId = initialBackup.getId();
                try {
                    getSelf().handleBackupFailure(failedBackupId, e);
                } catch (Exception failureHandlerEx) {
                    failureHandlerEx.printStackTrace();
                }
                throw new BackupFailedException("수동 백업 실패" + failedBackupId, e);
            } else {
                throw new BackupFailedException("백업 시장 중 오류 발생");
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Backup startBackup(String worker) {
        Backup newBackup = Backup.builder().worker(worker)
            .startedAt(Instant.now())
            .status(BackupStatus.IN_PROGRESS)
            .build();
        
        return backUpRepository.save(newBackup);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerBackup(Backup backupInProgress) throws IOException {
        Long backupId = backupInProgress.getId();
        List<Employee> employees = employeeRepository.findAll();
        List<BinaryContent> profiles = employees.stream().map(Employee::getProfile)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Backup managed = findBackupByIdOrThrow(backupId);
        managed.setProFileBackup(profiles);
        backUpRepository.saveAndFlush(managed);

        try (Stream<Employee> stream = employees.stream()) {
            csvBackupStorage.saveCsvFromStream(backupId, stream);
        }

        Backup toComplete = findBackupByIdOrThrow(backupId);
        if (toComplete.getStatus() == BackupStatus.IN_PROGRESS) {
            toComplete.complete(Instant.now());
            backUpRepository.save(toComplete);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBackupFailure(Long backupId, Exception failureCause) {
        try {
            cleanupBackupFiles(backupId, failureCause);

            Backup backup = backUpRepository.findById(backupId).orElseThrow(
                () -> new EntityNotFoundException("실패 처리 중 백업 ID를 찾을 수 없음" + backupId));

            if (backup.getStatus() != BackupStatus.FAILED) {
                backup.fail(Instant.now());
                backUpRepository.saveAndFlush(backup);
            }
        } catch (Exception e) {

            throw new BackupFailedException("백업 실패 처리 중 오류 발생", e);
        }
    }

    private void cleanupBackupFiles(Long backupId, Exception failureCause) {
        try {
            csvBackupStorage.saveErrorLog(backupId, failureCause);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            csvBackupStorage.deleteCsvFile(backupId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean lastedBackup() {
        Optional<Backup> lastCompleted = backUpRepository.findTopByStatusOrderByEndedAtDesc(
            (BackupStatus.COMPLETED));
        if (lastCompleted.isEmpty()) {
            return true;
        }
        Instant lastCompletionTime = lastCompleted.get().getEndedAt();
        if (lastCompletionTime == null) {
            return true;
        }
        return changeLogRepository.existsByCreatedAtAfter(lastCompletionTime);
    }

    private boolean needLogForScheduler() {
        final String batchWorker = "system";
        Optional<Backup> lastCompletedBatch = backUpRepository.findTopByWorkerAndStatusOrderByEndedAtDesc(
            batchWorker, BackupStatus.COMPLETED);
        if (lastCompletedBatch.isEmpty()) {
            return true;
        }
        Instant lastBatchEndTime = lastCompletedBatch.get().getEndedAt();
        if (lastBatchEndTime == null) {
            return true;
        }
        return changeLogRepository.existsByCreatedAtAfter(lastBatchEndTime);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void skippedSystemBackup() {
        createSkippedBackup("system");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Backup skippedManualBackup(String workerIp) {
        return createSkippedBackup(workerIp);
    }

    private Backup createSkippedBackup(String worker) {
        Instant now = Instant.now();
        Backup skippedBackup = Backup.builder().worker(worker).startedAt(now).endedAt(now)
            .status(BackupStatus.SKIPPED).build();
        return backUpRepository.save(skippedBackup);
    }

    @Transactional(readOnly = true)
    @Override
    public BackupPageDto search(String worker, String statusStr, Instant startedAtFrom,
        Instant startedAtTo, CursorRequest req) {
        BackupStatus status = parseStatus(statusStr);
        long totalElements = backUpRepository.countBackups(worker, status, startedAtFrom,
            startedAtTo);
        List<Backup> allResults = backUpRepository.searchWithCursor(worker, status, startedAtFrom,
            startedAtTo, req);
        boolean hasNext = allResults.size() > req.size();
        List<Backup> pageResults = hasNext ? allResults.subList(0, req.size()) : allResults;
        Long lastId =
            pageResults.isEmpty() ? null : pageResults.get(pageResults.size() - 1).getId();
        String nextCursor = generateCursor(lastId);
        return backUpMapper.toPageDto(pageResults, nextCursor, lastId, req.size(), totalElements,
            hasNext);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<BackupDto> findLatestBackupByStatus(String statusStr) {
        BackupStatus status = parseStatus(statusStr);
        return backUpRepository.findTopByStatusOrderByStartedAtDesc(status)
            .map(backUpMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBackupId(Long id) {
        Backup backup = findBackupByIdOrThrow(id);
        if (backup.getStatus() != BackupStatus.COMPLETED) {
            throw new BackupFailedException("백업이 완료되지 않음.");
        }
    }

    private BackupStatus parseStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return null;
        }
        String upperStatus = statusStr.toUpperCase().trim();
        if ("COMPLETE".equals(upperStatus)) {
            return BackupStatus.COMPLETED;
        }
        try {
            return BackupStatus.valueOf(upperStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String generateCursor(Long lastId) {
        if (lastId == null) {
            return null;
        }
        String cursorData = String.format("{\"id\":%d}", lastId);
        return Base64.getEncoder().encodeToString(cursorData.getBytes());
    }

    private Backup findBackupByIdOrThrow(Long backupId) {
        return backUpRepository.findById(backupId).orElseThrow(
            () -> new EntityNotFoundException("백업 ID에 해당하는 기록을 찾을 수 없음" + backupId));
    }
}