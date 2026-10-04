-- Sending the prizes to the club's WhatsApp group.

-- The photo of each prize that is sent: at most one per prize; with none marked, the first one.
alter table prize_photo add column main boolean not null default false;
create unique index prize_photo_one_main_idx on prize_photo (prize_id) where main;

-- When the admin confirmed that the prizes reached the group. They can be sent again, until the
-- melee closes; null means not sent (and every melee older than this feature).
alter table melee add column prizes_shared_at timestamptz;
