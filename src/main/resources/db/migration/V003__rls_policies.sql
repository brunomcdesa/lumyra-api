-- Row-Level Security (G1): isolamento por tenant em toda tabela com dado de aluna.
-- A cada requisição autenticada, a aplicação executa
--   SET LOCAL app.current_tenant = '<id>'
-- na conexão. As policies abaixo filtram as linhas pelo tenant corrente.
--
-- tenant_id é INTEGER; o setting é texto -> NULLIF(...,'')::integer converte.
-- Quando o setting não está definido, current_setting(...,true) devolve '',
-- NULLIF transforma em NULL e a comparação "tenant_id = NULL" é falsa:
-- nenhuma linha é visível (comportamento fail-safe).
--
-- FORCE ROW LEVEL SECURITY garante que a policy também vale para o dono da
-- tabela (caso a aplicação conecte com um papel que seja owner).

ALTER TABLE student ENABLE ROW LEVEL SECURITY;
ALTER TABLE student FORCE ROW LEVEL SECURITY;

CREATE POLICY student_tenant_isolation ON student
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::integer);

ALTER TABLE professional_student_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE professional_student_link FORCE ROW LEVEL SECURITY;

CREATE POLICY link_tenant_isolation ON professional_student_link
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::integer);

-- Rollback manual:
--   DROP POLICY student_tenant_isolation ON student;
--   ALTER TABLE student NO FORCE ROW LEVEL SECURITY;
--   ALTER TABLE student DISABLE ROW LEVEL SECURITY;
--   DROP POLICY link_tenant_isolation ON professional_student_link;
--   ALTER TABLE professional_student_link NO FORCE ROW LEVEL SECURITY;
--   ALTER TABLE professional_student_link DISABLE ROW LEVEL SECURITY;
