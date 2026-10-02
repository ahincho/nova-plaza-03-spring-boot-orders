-- Los pedidos pasan a la persistencia de Nova (ADR-054): heredan de AuditableEntity, que suma cuándo cambió el
-- pedido por última vez y quién lo creó y lo cambió, y se listan por cursor en lugar de todos de una vez.

alter table orders add column updated_at timestamp with time zone;
alter table orders add column created_by varchar(255);
alter table orders add column updated_by varchar(255);

-- Un pedido que ya existía no cambió desde que se creó, y lo creó el cliente que compró.
update orders set updated_at = created_at, created_by = customer_id, updated_by = customer_id;

alter table orders alter column updated_at set not null;

-- La página sigue el orden del scroll, del más nuevo al más viejo con el id como desempate, así que el índice
-- trae las dos columnas: la página siguiente salta al cursor sin leer las filas anteriores.
drop index orders_customer_created;

create index orders_customer_created_id on orders (customer_id, created_at desc, id desc);
