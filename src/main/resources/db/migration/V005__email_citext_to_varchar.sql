-- V005: troca CITEXT -> VARCHAR(255) nas colunas de e-mail.
-- Motivo: o JDBC do Postgres reporta CITEXT como Types#OTHER; o Hibernate mapeia
-- String -> VARCHAR (Types#VARCHAR). Com ddl-auto: validate o boot falha com
-- SchemaManagementException (wrong column type ... found [citext], expecting [varchar]).
-- Mantemos a comparacao case-insensitive via indice unico em lower(email).

-- Professional: dropa o unique inline (professional_email_key), troca o tipo e
-- recria a unicidade de forma case-insensitive.
ALTER TABLE professional DROP CONSTRAINT IF EXISTS professional_email_key;
ALTER TABLE professional ALTER COLUMN email TYPE VARCHAR(255) USING email::text;
CREATE UNIQUE INDEX ux_professional_email_lower ON professional (lower(email));

-- Student: email e nullable; lower(NULL) = NULL, entao multiplos NULL continuam
-- permitidos pelo indice unico (semantica igual a do UNIQUE original).
ALTER TABLE student DROP CONSTRAINT IF EXISTS student_email_key;
ALTER TABLE student ALTER COLUMN email TYPE VARCHAR(255) USING email::text;
CREATE UNIQUE INDEX ux_student_email_lower ON student (lower(email));

-- Invite: nao havia UNIQUE no email, apenas troca de tipo.
ALTER TABLE invite ALTER COLUMN email TYPE VARCHAR(255) USING email::text;
