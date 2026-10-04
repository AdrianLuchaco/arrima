-- Arrima initial schema.
-- Flyway runs this inside the "arrima" schema (see spring.flyway.schemas), never in "public".
-- Domain rules that the database can guarantee cheaply are written as constraints, as a safety
-- net under the domain code: no rematches, one bye per round, at most one bye per team...

-- ---------------------------------------------------------------------------
-- Club and administrator account
-- ---------------------------------------------------------------------------

create table club (
    id                            bigint generated always as identity primary key,
    name                          varchar(100) not null,
    logo_path                     varchar(200),
    court_count                   integer      not null check (court_count between 1 and 200),
    default_rounds                integer      not null check (default_rounds between 1 and 20),
    default_prize_count           integer      not null check (default_prize_count between 1 and 100),
    -- Default points of la Internacional; every melee gets its own copy when it is created.
    points_pointing_out           integer      not null,
    points_pointing_big_circle    integer      not null,
    points_pointing_small_circle  integer      not null,
    points_pointing_near_jack     integer      not null,
    points_pointing_on_jack       integer      not null,
    points_shooting_miss          integer      not null,
    points_shooting_hit           integer      not null,
    points_shooting_hit_out       integer      not null,
    points_shooting_carreau       integer      not null,
    created_at                    timestamptz  not null default now(),
    updated_at                    timestamptz  not null default now(),
    constraint club_points_range check (
        least(points_pointing_out, points_pointing_big_circle, points_pointing_small_circle,
              points_pointing_near_jack, points_pointing_on_jack, points_shooting_miss,
              points_shooting_hit, points_shooting_hit_out, points_shooting_carreau) >= 0
        and greatest(points_pointing_out, points_pointing_big_circle, points_pointing_small_circle,
                     points_pointing_near_jack, points_pointing_on_jack, points_shooting_miss,
                     points_shooting_hit, points_shooting_hit_out, points_shooting_carreau) <= 99)
);

-- One account per club for now: dropping the unique constraint on club_id would allow several.
create table club_admin (
    id                   bigint generated always as identity primary key,
    club_id              bigint       not null unique references club (id) on delete cascade,
    -- Stored already normalised to lower case, so a plain unique constraint is enough.
    email                varchar(254) not null unique check (email = lower(email)),
    password_hash        varchar(255) not null,
    password_changed_at  timestamptz  not null default now(),
    last_login_at        timestamptz,
    created_at           timestamptz  not null default now()
);

-- Only the SHA-256 hash of each refresh token is stored. Tokens issued from the same login share a
-- family: presenting an already rotated token after the grace period revokes the whole family.
create table refresh_token (
    id          bigint generated always as identity primary key,
    admin_id    bigint      not null references club_admin (id) on delete cascade,
    family_id   uuid        not null,
    token_hash  varchar(64) not null unique,
    issued_at   timestamptz not null default now(),
    expires_at  timestamptz not null,
    rotated_at  timestamptz,
    revoked_at  timestamptz
);
create index refresh_token_admin_idx on refresh_token (admin_id);
create index refresh_token_family_idx on refresh_token (family_id);

-- Single-use codes that the platform owner hands to each new club (only the hash is stored).
create table signup_invitation (
    id               bigint generated always as identity primary key,
    code_hash        varchar(64)  not null unique,
    note             varchar(200),
    created_at       timestamptz  not null default now(),
    expires_at       timestamptz  not null,
    used_at          timestamptz,
    used_by_club_id  bigint references club (id) on delete set null
);
create index signup_invitation_club_idx on signup_invitation (used_by_club_id);

-- ---------------------------------------------------------------------------
-- Melee and registration
-- ---------------------------------------------------------------------------

create table melee (
    id                            bigint generated always as identity primary key,
    club_id                       bigint      not null references club (id) on delete cascade,
    played_on                     date        not null,
    format                        varchar(20) not null check (format in ('CLASSIC')),
    team_size                     integer     not null check (team_size in (2, 3)),
    status                        varchar(20) not null check (status in
                                      ('REGISTRATION', 'TEAMS', 'MATCHES', 'INTERNATIONAL', 'PRIZES', 'CLOSED')),
    public_code                   varchar(8)  not null unique,
    -- Copy of the club settings taken when the melee is created, so later profile edits
    -- never change the history.
    rounds_count                  integer     not null check (rounds_count between 1 and 20),
    prize_count                   integer     not null check (prize_count between 1 and 100),
    court_count                   integer     not null check (court_count between 1 and 200),
    points_pointing_out           integer     not null,
    points_pointing_big_circle    integer     not null,
    points_pointing_small_circle  integer     not null,
    points_pointing_near_jack     integer     not null,
    points_pointing_on_jack       integer     not null,
    points_shooting_miss          integer     not null,
    points_shooting_hit           integer     not null,
    points_shooting_hit_out       integer     not null,
    points_shooting_carreau       integer     not null,
    -- Render cannot run scheduled jobs reliably: the 20-minute auto-close is evaluated on the next request.
    last_activity_at              timestamptz not null default now(),
    closed_at                     timestamptz,
    created_at                    timestamptz not null default now(),
    updated_at                    timestamptz not null default now(),
    -- Increased by every change to the melee or anything in it: clients compare it to know
    -- whether their copy is up to date (SSE events, polling with ETag).
    revision                      bigint      not null default 0
);
create index melee_club_played_on_idx on melee (club_id, played_on desc);

-- Registrants belong to one melee. A future club_player table can be linked through a new
-- nullable column (participant.club_player_id) without rewriting existing rows.
create table participant (
    id            bigint generated always as identity primary key,
    melee_id      bigint      not null references melee (id) on delete cascade,
    -- Number from the WhatsApp list: it may be missing or repeated, so it is neither required nor unique.
    list_number   integer     check (list_number > 0),
    display_name  varchar(60) not null,
    status        varchar(20) not null check (status in ('ACTIVE', 'WITHDRAWN')),
    created_at    timestamptz not null default now()
);
create index participant_melee_idx on participant (melee_id);

create table team (
    id        bigint generated always as identity primary key,
    melee_id  bigint   not null references melee (id) on delete cascade,
    number    integer not null check (number > 0),
    constraint team_number_per_melee unique (melee_id, number)
);

-- The team size is the number of members, so the odd-sized team needs no special column.
create table team_member (
    team_id         bigint not null references team (id) on delete cascade,
    participant_id  bigint not null references participant (id) on delete cascade,
    primary key (team_id, participant_id),
    -- A player is in one team at most. Checked at commit, so two players can swap teams.
    constraint team_member_one_team unique (participant_id) deferrable initially deferred
);

-- ---------------------------------------------------------------------------
-- Court schedule and results
-- ---------------------------------------------------------------------------

create table matchup (
    id              bigint generated always as identity primary key,
    melee_id        bigint      not null references melee (id) on delete cascade,
    round_number    integer     not null check (round_number > 0),
    -- Teams are stored in id order so that the unique constraint below forbids rematches.
    team_a_id       bigint      not null references team (id) on delete cascade,
    team_b_id       bigint      not null references team (id) on delete cascade,
    -- Null while the matchup waits for a free court.
    court_number    integer     check (court_number > 0),
    winner_team_id  bigint      references team (id),
    decided_at      timestamptz,
    constraint matchup_teams_ordered check (team_a_id < team_b_id),
    constraint matchup_no_rematch unique (team_a_id, team_b_id),
    constraint matchup_winner_plays check (winner_team_id in (team_a_id, team_b_id))
);
create index matchup_melee_round_idx on matchup (melee_id, round_number);
create index matchup_team_b_idx on matchup (team_b_id);
create index matchup_winner_idx on matchup (winner_team_id);
-- No unique (round, court): a matchup that waited for a court is later played on a court that
-- another matchup of the same round has already used and freed.

create table bye (
    id            bigint generated always as identity primary key,
    melee_id      bigint   not null references melee (id) on delete cascade,
    round_number  integer not null check (round_number > 0),
    -- A team rests at most once in the whole melee.
    team_id       bigint   not null unique references team (id) on delete cascade,
    constraint bye_one_per_round unique (melee_id, round_number)
);

-- ---------------------------------------------------------------------------
-- La Internacional (tie-break by points)
-- ---------------------------------------------------------------------------

create table international_group (
    id                    bigint generated always as identity primary key,
    melee_id              bigint      not null references melee (id) on delete cascade,
    -- 1 is the group with the fewest wins that still competes for a prize: it plays first.
    play_order            integer     not null check (play_order > 0),
    wins                  integer     not null check (wins >= 0),
    best_prize_position   integer     not null,
    worst_prize_position  integer     not null,
    status                varchar(20) not null check (status in ('PENDING', 'IN_PROGRESS', 'FINISHED')),
    constraint international_group_order unique (melee_id, play_order),
    constraint international_group_prizes check (best_prize_position between 1 and worst_prize_position)
);

-- Round 1 is the regular round of a group; rounds 2+ are tie-breaks among tied teams only.
create table international_round (
    id            bigint generated always as identity primary key,
    group_id      bigint   not null references international_group (id) on delete cascade,
    round_number  integer not null check (round_number > 0),
    constraint international_round_number unique (group_id, round_number)
);

create table international_round_team (
    round_id    bigint   not null references international_round (id) on delete cascade,
    team_id     bigint   not null references team (id) on delete cascade,
    play_order  integer not null check (play_order > 0),
    primary key (round_id, team_id),
    constraint international_round_team_order unique (round_id, play_order)
);
create index international_round_team_team_idx on international_round_team (team_id);

create table ball_throw (
    id            bigint generated always as identity primary key,
    round_id      bigint      not null,
    team_id       bigint      not null,
    kind          varchar(10) not null check (kind in ('POINTING', 'SHOOTING')),
    ball_number   integer     not null check (ball_number between 1 and 3),
    outcome       varchar(20) not null,
    points        integer     not null check (points >= 0),
    recorded_at   timestamptz not null default now(),
    corrected_at  timestamptz,
    -- The team must take part in that round.
    foreign key (round_id, team_id) references international_round_team (round_id, team_id) on delete cascade,
    -- Saving the same ball twice (an offline retry) overwrites it instead of duplicating it.
    constraint ball_throw_once unique (round_id, team_id, kind, ball_number),
    constraint ball_throw_outcome_matches_kind check (
        (kind = 'POINTING' and outcome in ('OUT', 'BIG_CIRCLE', 'SMALL_CIRCLE', 'NEAR_JACK', 'ON_JACK'))
        or (kind = 'SHOOTING' and outcome in ('MISS', 'HIT', 'HIT_OUT', 'CARREAU')))
);
create index ball_throw_team_idx on ball_throw (team_id);

-- ---------------------------------------------------------------------------
-- Prizes and photos
-- ---------------------------------------------------------------------------

create table prize (
    id                    bigint generated always as identity primary key,
    melee_id              bigint      not null references melee (id) on delete cascade,
    position              integer     not null check (position > 0),
    team_id               bigint      not null references team (id) on delete cascade,
    -- Null when the team did not need to play la Internacional.
    international_points  integer,
    -- Set when the prize is shown during the ceremony.
    awarded_at            timestamptz,
    -- Deferred to the end of the transaction, so recalculating the ranking can swap positions.
    constraint prize_position_per_melee unique (melee_id, position) deferrable initially deferred,
    constraint prize_team_per_melee unique (melee_id, team_id)
);
create index prize_team_idx on prize (team_id);

create table prize_photo (
    id            bigint generated always as identity primary key,
    prize_id      bigint       not null references prize (id) on delete cascade,
    -- Server-generated object name in the private Supabase Storage bucket.
    storage_path  varchar(200) not null unique,
    content_type  varchar(50)  not null,
    size_bytes    integer      not null check (size_bytes > 0),
    created_at    timestamptz  not null default now()
);
create index prize_photo_prize_idx on prize_photo (prize_id);

-- ---------------------------------------------------------------------------
-- Defence in depth for Supabase: the backend connects as the table owner, which bypasses RLS,
-- while any other role (such as the Data API's anon and authenticated) is denied because no
-- policy grants it anything.
-- ---------------------------------------------------------------------------

alter table club                     enable row level security;
alter table club_admin               enable row level security;
alter table refresh_token            enable row level security;
alter table signup_invitation        enable row level security;
alter table melee                    enable row level security;
alter table participant              enable row level security;
alter table team                     enable row level security;
alter table team_member              enable row level security;
alter table matchup                  enable row level security;
alter table bye                      enable row level security;
alter table international_group      enable row level security;
alter table international_round      enable row level security;
alter table international_round_team enable row level security;
alter table ball_throw               enable row level security;
alter table prize                    enable row level security;
alter table prize_photo              enable row level security;
