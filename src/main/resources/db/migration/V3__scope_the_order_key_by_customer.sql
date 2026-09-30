-- Pedidos deja de ser el dueño de la clave de idempotencia: la aplica el starter de Nova (ADR-047), con su
-- propia tabla. La clave se queda en el pedido para saber de qué compra salió, pero su restricción única pasa
-- a acotarse por cliente, como la de la capacidad: la misma clave de dos clientes son dos compras distintas.
-- También admite los 255 caracteres que acepta el contrato.

alter table orders alter column idempotency_key type varchar(255);

alter table orders drop constraint orders_idempotency_key_key;

alter table orders
    add constraint orders_customer_idempotency_key unique (customer_id, idempotency_key);
