-- Extensões necessárias para o banco Lumyra.
-- pgcrypto: geração de UUIDs e funções criptográficas.
-- citext: comparação de texto case-insensitive (e-mails, slugs).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
