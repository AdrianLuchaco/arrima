-- Password recovery by e-mail. Like refresh tokens, only the SHA-256 hash of each link's token is
-- stored: someone who reads this table cannot use the links. Each admin has at most one pending
-- link (asking for a new one deletes the previous) and each link works once.
create table password_reset_token (
    id          bigint generated always as identity primary key,
    admin_id    bigint      not null references club_admin (id) on delete cascade,
    token_hash  varchar(64) not null unique,
    created_at  timestamptz not null default now(),
    expires_at  timestamptz not null,
    used_at     timestamptz
);
create index password_reset_token_admin_idx on password_reset_token (admin_id);

alter table password_reset_token enable row level security;
