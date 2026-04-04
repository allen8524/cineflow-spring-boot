alter table movies
    add column booking_open boolean not null default true;

alter table movies
    add column active boolean not null default true;

alter table theaters
    add column active boolean not null default true;

alter table screens
    add column active boolean not null default true;

create index idx_movies_active_booking_open on movies (active, booking_open);
create index idx_theaters_active on theaters (active);
create index idx_screens_active on screens (active);
