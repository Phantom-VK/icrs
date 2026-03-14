CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id TEXT PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding VECTOR(384)
);

CREATE INDEX IF NOT EXISTS spring_ai_vector_index
    ON vector_store USING HNSW (embedding vector_cosine_ops);
