-- V002 · Outbox transaccional de la plataforma.
--
-- Es la tabla del registro de publicación de eventos de Spring Modulith (spring-modulith-events-jdbc), copiada de su
-- schema-postgresql.sql 1.4.0 para que Flyway sea el único dueño del esquema (spring.modulith.events.jdbc.
-- schema-initialization.enabled=false). El evento se inserta en la misma transacción que el cambio que lo origina;
-- completion_date queda nula hasta que el listener termina, y los pendientes se pueden reintentar (al menos una vez).
-- Es la única tabla compartida entre módulos y la única excepción a R1.

CREATE TABLE plataforma.event_publication
(
    id               UUID                     NOT NULL,
    listener_id      TEXT                     NOT NULL,
    event_type       TEXT                     NOT NULL,
    serialized_event TEXT                     NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx
    ON plataforma.event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx
    ON plataforma.event_publication (completion_date);

GRANT SELECT, INSERT, UPDATE, DELETE ON plataforma.event_publication TO rol_plataforma;
