-- Módulo identity: tenant, profissional, aluna e vínculo.
-- IDs INTEGER com sequência por tabela (mesmo padrão da entidade Usuario).
-- RLS é habilitado na migração V003 (tabelas com dado de aluna).

-- Tenant: unidade de isolamento. Um profissional/assinatura corresponde a um tenant.
CREATE SEQUENCE seq_tenant START 1 INCREMENT 1;
CREATE TABLE tenant (
    id        INTEGER      PRIMARY KEY DEFAULT nextval('seq_tenant'),
    nome      VARCHAR(150) NOT NULL,
    criado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ativo     BOOLEAN      NOT NULL DEFAULT true
);

-- Professional: educador(a) com CREF, dono dos dados das alunas do seu tenant.
CREATE SEQUENCE seq_professional START 1 INCREMENT 1;
CREATE TABLE professional (
    id         INTEGER      PRIMARY KEY DEFAULT nextval('seq_professional'),
    tenant_id  INTEGER      NOT NULL REFERENCES tenant(id),
    nome       VARCHAR(150) NOT NULL,
    email      CITEXT       NOT NULL UNIQUE,
    cref       VARCHAR(20),
    senha_hash VARCHAR(255) NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ativo      BOOLEAN      NOT NULL DEFAULT true
);

-- Student: aluna. Tabela com dado de aluna -> RLS habilitado em V003.
CREATE SEQUENCE seq_student START 1 INCREMENT 1;
CREATE TABLE student (
    id              INTEGER      PRIMARY KEY DEFAULT nextval('seq_student'),
    tenant_id       INTEGER      NOT NULL REFERENCES tenant(id),
    nome            VARCHAR(150) NOT NULL,
    email           CITEXT       UNIQUE,
    data_nascimento DATE,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ativo           BOOLEAN      NOT NULL DEFAULT true
);

-- Vínculo profissional <-> aluna. Soft delete via deletado_em. RLS em V003.
CREATE SEQUENCE seq_professional_student_link START 1 INCREMENT 1;
CREATE TABLE professional_student_link (
    id              INTEGER     PRIMARY KEY DEFAULT nextval('seq_professional_student_link'),
    tenant_id       INTEGER     NOT NULL REFERENCES tenant(id),
    professional_id INTEGER     NOT NULL REFERENCES professional(id),
    student_id      INTEGER     NOT NULL REFERENCES student(id),
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deletado_em     TIMESTAMPTZ,
    UNIQUE (professional_id, student_id)
);

CREATE INDEX idx_professional_tenant ON professional(tenant_id);
CREATE INDEX idx_student_tenant      ON student(tenant_id);
CREATE INDEX idx_link_tenant         ON professional_student_link(tenant_id);
CREATE INDEX idx_link_student        ON professional_student_link(student_id);
