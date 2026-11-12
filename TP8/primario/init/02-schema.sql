CREATE TABLE producto (
    id     SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0)
);
