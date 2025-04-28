-- 기존 테이블 및 타입 삭제 (주의: 현재 주석 처리됨! 필요시 직접 실행)
-- DROP TABLE IF EXISTS backups_files CASCADE;
-- DROP TABLE IF EXISTS change_log_details CASCADE;
-- DROP TABLE IF EXISTS change_logs CASCADE;
-- DROP TABLE IF EXISTS employees CASCADE;
-- DROP TABLE IF EXISTS back_ups CASCADE;
-- DROP TABLE IF EXISTS binary_contents CASCADE;
-- DROP TABLE IF EXISTS departments CASCADE;
-- DROP TYPE IF EXISTS employee_status;
-- DROP TYPE IF EXISTS backup_status_enum;
-- DROP TYPE IF EXISTS change_type CASCADE;


-- 타입 생성 (스크립트 반복 실행 시 오류 방지 위해 IF NOT EXISTS 권장)
-- DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'employee_status') THEN CREATE TYPE employee_status AS ENUM ('ACTIVE', 'ON_LEAVE', 'RESIGNED'); END IF; END $$;
-- DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'backup_status_enum') THEN CREATE TYPE backup_status_enum AS ENUM ('COMPLETED', 'FAILED', 'IN_PROGRESS', 'SKIPPED'); END IF; END $$;
-- DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'change_type') THEN CREATE TYPE change_type AS ENUM ('CREATED', 'UPDATED', 'DELETED'); END IF; END $$;

-- 또는 타입 직접 생성 (주석 처리된 DO 블록 대신 사용)
CREATE TYPE employee_status AS ENUM ('ACTIVE', 'ON_LEAVE', 'RESIGNED');
CREATE TYPE backup_status_enum AS ENUM ('COMPLETED', 'FAILED', 'IN_PROGRESS', 'SKIPPED');
CREATE TYPE change_type AS ENUM ('CREATED', 'UPDATED', 'DELETED');


-- 테이블 생성
CREATE TABLE IF NOT EXISTS departments
(
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT         NOT NULL,
    foundation_date TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS binary_contents
(
    id           BIGSERIAL PRIMARY KEY,
    file_name    VARCHAR(100) NOT NULL,
    size         BIGINT       NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 시퀀스 시작 값 변경
--ALTER SEQUENCE binary_contents_id_seq RESTART WITH 10000;


CREATE TABLE IF NOT EXISTS employees
(
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(30)     NOT NULL,
    email            VARCHAR(100)    NOT NULL UNIQUE,
    emp_number       VARCHAR(100)    NOT NULL UNIQUE,
    dept_id          BIGINT          NOT NULL REFERENCES departments (id),
    position         VARCHAR(100)    NOT NULL,
    hire_date        DATE            NOT NULL,
    status           employee_status NOT NULL DEFAULT 'ACTIVE',
    profile_image_id BIGINT REFERENCES binary_contents (id),
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS back_ups
(
    id         SERIAL PRIMARY KEY,
    worker     VARCHAR(255)       NOT NULL,
    status     backup_status_enum NOT NULL,
    started_at TIMESTAMP          NOT NULL,
    ended_at   TIMESTAMP,
    created_at TIMESTAMPTZ        NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS backups_files
(
    backups_id         BIGINT      NOT NULL REFERENCES back_ups (id) on Delete Cascade,
    binary_contents_id BIGINT      NOT NULL REFERENCES binary_contents (id) on Delete Cascade,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (backups_id, binary_contents_id)
);

CREATE TABLE IF NOT EXISTS change_logs
(
    id              SERIAL PRIMARY KEY,
    type            change_type  NOT NULL,
    employee_number VARCHAR(100) NOT NULL,
    memo            TEXT,
    ip_address      VARCHAR(45)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS change_log_details
(
    id            SERIAL PRIMARY KEY,
    change_log_id BIGINT       NOT NULL REFERENCES change_logs (id) ON DELETE CASCADE,
    property_name VARCHAR(255) NOT NULL,
    before_value  TEXT,
    after_value   TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL
);


-- 샘플 데이터 INSERT (created_at 컬럼 제외)
INSERT INTO departments (name, description, foundation_date, updated_at)
VALUES ('개발팀', '소프트웨어 개발 부서', '2022-01-01', now()),
       ('마케팅팀', '브랜드 전략과 시장 분석을 담당합니다.', '2021-06-15', now()),
       ('인사팀', '사내 인사 및 복지 업무를 담당합니다.', '2020-09-01', now()),
       ('디자인팀', 'UI/UX 디자인 및 브랜딩을 담당합니다.', '2023-03-10', now()),
       ('데이터팀', '데이터 분석 및 인사이트 제공을 담당합니다.', '2022-11-20', now()),
       ('고객지원팀', '고객 문의 및 지원 업무를 담당합니다.', '2021-02-05', now()),
       ('프론트엔드팀', '웹 프론트엔드 개발을 담당합니다.', '2022-08-01', now()),
       ('백엔드팀', '서버 및 DB 관리, API 개발을 담당합니다.', '2020-12-20', now()),
       ('QA팀', '품질 보증 및 테스트를 담당합니다.', '2023-04-10', now()),
       ('IT운영팀', '사내 IT 인프라 운영을 담당합니다.', '2021-11-30', now()),
       ('보안팀', '시스템 보안 및 정보 보호를 담당합니다.', '2023-02-14', now());

INSERT INTO employees (name, email, emp_number, dept_id, position, hire_date, status,
                       profile_image_id, updated_at)
SELECT '직원_' || gs                                     AS name,
       'employee' || gs || '@hrbank.com'               AS email,
       'EMP' || LPAD(gs::text, 4, '0')                 AS emp_number,
       (FLOOR(random() * 11) + 1)::BIGINT              AS dept_id,
       CASE
           WHEN gs % 4 = 0 THEN '사원'
           WHEN gs % 4 = 1 THEN '대리'
           WHEN gs % 4 = 2 THEN '과장'
           ELSE '부장'
           END                                         AS position,
       (DATE '2020-01-01' + (floor(random() * (DATE '2024-04-27' - DATE '2020-01-01')) ||
                             ' days')::interval)::date AS hire_date,
       CASE
           WHEN gs % 10 = 0 THEN 'ON_LEAVE'::employee_status
           WHEN gs % 15 = 0 THEN 'RESIGNED'::employee_status
           ELSE 'ACTIVE'::employee_status
           END                                         AS status,
       NULL                                            AS profile_image_id,
       CURRENT_TIMESTAMP                               AS updated_at
FROM generate_series(1, 100) AS gs;


-- 모든 데이터 삽입 후 시퀀스 값 조정 (필요시 주석 해제)
-- SELECT setval('binary_contents_id_seq', COALESCE((SELECT MAX(id) FROM binary_contents), 9999), true);
-- SELECT setval('departments_id_seq', COALESCE((SELECT MAX(id) FROM departments), 0), true);
-- SELECT setval('employees_id_seq', COALESCE((SELECT MAX(id) FROM employees), 0), true);
-- SELECT setval('back_ups_id_seq', COALESCE((SELECT MAX(id) FROM back_ups), 0), true);
-- SELECT setval('change_logs_id_seq', COALESCE((SELECT MAX(id) FROM change_logs), 0), true);
-- SELECT setval('change_log_details_id_seq', COALESCE((SELECT MAX(id) FROM change_log_details), 0), true);
