alter table bookings
    add column user_id bigint null;

alter table bookings
    add constraint fk_bookings_user
    foreign key (user_id) references users (id);

create index idx_bookings_user_id on bookings (user_id);
