create table orders (
    id              uuid primary key,
    customer_id     varchar(64)  not null,
    status          varchar(16)  not null,
    currency        varchar(3)   not null,
    total           numeric(12, 2) not null,
    reservation_id  uuid         not null,
    idempotency_key varchar(128) not null unique,
    created_at      timestamp with time zone not null,
    version         bigint       not null
);

create index orders_customer_created on orders (customer_id, created_at desc);

create table order_items (
    id         bigint generated always as identity primary key,
    order_id   uuid        not null references orders (id) on delete cascade,
    sku        varchar(64) not null,
    quantity   integer     not null check (quantity > 0),
    unit_price numeric(12, 2) not null check (unit_price > 0)
);

create index order_items_order on order_items (order_id);
