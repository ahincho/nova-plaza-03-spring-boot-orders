-- El outbox de Nova (ADR-048), copiado de V1__create_outbox.sql de nova-outbox 0.1.0. Un evento se escribe en
-- la misma transacción que el cambio del pedido y se borra en ella: Debezium lo lee del log, no de la tabla.
create table outbox (
    id             uuid         primary key,
    aggregate_type varchar(255) not null,
    aggregate_id   varchar(255) not null,
    type           varchar(255) not null,
    source         varchar(255) not null,
    time           timestamptz  not null,
    payload        jsonb        not null,
    traceparent    varchar(55),
    tracestate     varchar(512),
    specversion    varchar(10)  not null default '1.0'
);
