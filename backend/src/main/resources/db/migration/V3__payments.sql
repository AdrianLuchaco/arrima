-- Payment control at the sign-up table: who has paid the entry fee. The app only keeps the record;
-- no money moves through it. Amounts in cents, so 2,50 € is exact.

alter table club add column default_entry_fee_cents integer not null default 500
    check (default_entry_fee_cents between 0 and 10000);

-- A fee of 0 means "no payment control". Melees created before this feature had none, so they get 0
-- and keep looking exactly as they did. New melees always copy the fee explicitly: no default.
alter table melee add column entry_fee_cents integer not null default 0
    check (entry_fee_cents between 0 and 10000);
alter table melee alter column entry_fee_cents drop default;

alter table participant add column payment_status varchar(10) not null default 'UNMARKED'
    check (payment_status in ('UNMARKED', 'PAID', 'UNPAID'));
