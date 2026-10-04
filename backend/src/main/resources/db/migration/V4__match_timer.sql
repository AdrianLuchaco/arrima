-- Match timer: every match of a round starts at once and lasts a fixed time; when it is up an alarm
-- sounds and a notification reaches the phones that asked for it.

alter table club add column default_match_minutes integer not null default 45
    check (default_match_minutes between 5 and 180);

-- Existing melees get the usual 45 minutes; new ones always copy the club's value: no default.
alter table melee add column match_minutes integer not null default 45
    check (match_minutes between 5 and 180);
alter table melee alter column match_minutes drop default;

-- The countdown of one round, kept in the database so it survives a restart of the backend.
-- Every phone computes what is left from these values; nothing is sent every second.
-- ended_at is set exactly once (an atomic update): whoever sets it sends the notifications.
create table round_timer (
    id               bigint generated always as identity primary key,
    melee_id         bigint      not null references melee (id) on delete cascade,
    round_number     integer     not null check (round_number between 1 and 20),
    duration_seconds integer     not null check (duration_seconds > 0),
    started_at       timestamptz not null,
    -- Total length of the pauses already over, and the start of the current one (null if running).
    paused_millis    bigint      not null default 0 check (paused_millis >= 0),
    paused_at        timestamptz,
    ended_at         timestamptz,
    end_reason       varchar(20) check (end_reason in ('TIME_UP', 'NEXT_ROUND')),
    unique (melee_id, round_number),
    check ((ended_at is null) = (end_reason is null))
);

-- Phones that asked to be told when the time is up: the admin's and the players'. Deleted when the
-- melee closes. The endpoint is a URL of the browser's push service (Google, Apple, Mozilla...).
create table push_subscription (
    id          bigint generated always as identity primary key,
    melee_id    bigint        not null references melee (id) on delete cascade,
    endpoint    varchar(1000) not null,
    p256dh      varchar(100)  not null,
    auth        varchar(50)   not null,
    audience    varchar(10)   not null check (audience in ('ADMIN', 'PUBLIC')),
    created_at  timestamptz   not null default now(),
    unique (melee_id, endpoint)
);

alter table round_timer       enable row level security;
alter table push_subscription enable row level security;
