-- T0.5: convite + consentimento LGPD da aluna.
-- Adiciona credencial e soft-delete na aluna, e cria as tabelas invite e consent.
-- invite e consent contêm dado de aluna -> RLS habilitado JUNTO (G1), espelhando V003.

-- 1) Credencial da aluna (definida ao aceitar o convite) + soft-delete para LGPD.
ALTER TABLE student ADD COLUMN senha_hash  VARCHAR(255);
ALTER TABLE student ADD COLUMN deletado_em TIMESTAMPTZ;

-- 2) Convite: o profissional convida a aluna; o token bruto vai no link e só o
--    hash SHA-256 é persistido (G4). O tenant vai embutido no token, pois o
--    accept-invite é público e precisa setar o contexto de RLS manualmente.
CREATE SEQUENCE seq_invite START 1 INCREMENT 1;
CREATE TABLE invite (
    id         INTEGER      PRIMARY KEY DEFAULT nextval('seq_invite'),
    tenant_id  INTEGER      NOT NULL REFERENCES tenant(id),
    student_id INTEGER      NOT NULL REFERENCES student(id),
    email      CITEXT       NOT NULL,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    status     VARCHAR(20)  NOT NULL,
    expira_em  TIMESTAMPTZ  NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    aceito_em  TIMESTAMPTZ
);
CREATE INDEX idx_invite_tenant  ON invite(tenant_id);
CREATE INDEX idx_invite_student ON invite(student_id);

-- 3) Consentimento LGPD: base legal para tratar dado sensível (G2). Ativo
--    enquanto revogado_em IS NULL. Versão do termo registrada junto.
CREATE SEQUENCE seq_consent START 1 INCREMENT 1;
CREATE TABLE consent (
    id            INTEGER      PRIMARY KEY DEFAULT nextval('seq_consent'),
    tenant_id     INTEGER      NOT NULL REFERENCES tenant(id),
    student_id    INTEGER      NOT NULL REFERENCES student(id),
    terms_version VARCHAR(20)  NOT NULL,
    registrado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revogado_em   TIMESTAMPTZ
);
CREATE INDEX idx_consent_tenant         ON consent(tenant_id);
CREATE INDEX idx_consent_student_ativo  ON consent(student_id, revogado_em);

-- 4) RLS por tenant (G1) -- mesmo padrão da V003.
ALTER TABLE invite ENABLE ROW LEVEL SECURITY;
ALTER TABLE invite FORCE  ROW LEVEL SECURITY;
CREATE POLICY invite_tenant_isolation ON invite
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::integer);

ALTER TABLE consent ENABLE ROW LEVEL SECURITY;
ALTER TABLE consent FORCE  ROW LEVEL SECURITY;
CREATE POLICY consent_tenant_isolation ON consent
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::integer);

-- Rollback manual:
--   DROP POLICY consent_tenant_isolation ON consent;
--   ALTER TABLE consent NO FORCE ROW LEVEL SECURITY;
--   ALTER TABLE consent DISABLE ROW LEVEL SECURITY;
--   DROP TABLE consent; DROP SEQUENCE seq_consent;
--   DROP POLICY invite_tenant_isolation ON invite;
--   ALTER TABLE invite NO FORCE ROW LEVEL SECURITY;
--   ALTER TABLE invite DISABLE ROW LEVEL SECURITY;
--   DROP TABLE invite; DROP SEQUENCE seq_invite;
--   ALTER TABLE student DROP COLUMN deletado_em;
--   ALTER TABLE student DROP COLUMN senha_hash;
