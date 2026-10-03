-- Creates a single-use invitation so a new club can register, and shows the code to hand over.
--
-- Production: paste it into Supabase → SQL Editor, change the note, and press Run.
-- Local:      docker compose exec -T postgres psql -U <DB_USERNAME> -d arrima < ../scripts/create-invitation.sql
--             (run from backend/)
--
-- Only the SHA-256 of the code is stored: once you close the result, the code cannot be recovered
-- (create another one if it gets lost). It expires in 30 days if nobody uses it.

with new_code as (
    -- 12 random hexadecimal characters (48 bits), e.g. 3F9A1C07B2E4
    select upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12)) as code
), invitation as (
    insert into arrima.signup_invitation (code_hash, note, expires_at)
    select encode(sha256(convert_to(code, 'UTF8')), 'hex'),
           'Club de Petanca ...',            -- note for yourself: which club it is for
           now() + interval '30 days'
    from new_code
    returning id
)
select new_code.code as invitation_code
from new_code, invitation;
