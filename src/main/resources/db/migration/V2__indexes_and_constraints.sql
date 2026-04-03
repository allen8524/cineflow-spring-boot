alter table bookings
    add constraint uk_bookings_booking_code unique (booking_code);

alter table payments
    add constraint uk_payments_booking_id unique (booking_id);

alter table payments
    add constraint uk_payments_transaction_id unique (transaction_id);

alter table seat_templates
    add constraint uk_seat_templates_screen_seat_code unique (screen_id, seat_code);

alter table schedule_seats
    add constraint uk_schedule_seats_schedule_seat_template unique (schedule_id, seat_template_id);

alter table booking_seats
    add constraint uk_booking_seats_booking_seat_code unique (booking_id, seat_code);

create index idx_movies_status on movies (status);
create index idx_movies_release_date on movies (release_date);
create index idx_theaters_region on theaters (region);
create index idx_screens_theater_id on screens (theater_id);
create index idx_schedules_movie_id on schedules (movie_id);
create index idx_schedules_screen_id on schedules (screen_id);
create index idx_schedules_start_time on schedules (start_time);
create index idx_seat_templates_screen_id on seat_templates (screen_id);
create index idx_bookings_status on bookings (status);
create index idx_bookings_schedule_id on bookings (schedule_id);
create index idx_bookings_start_time on bookings (start_time);
create index idx_schedule_seats_schedule_id on schedule_seats (schedule_id);
create index idx_schedule_seats_schedule_reserved on schedule_seats (schedule_id, reserved);
create index idx_booking_seats_booking_id on booking_seats (booking_id);
create index idx_payments_status on payments (payment_status);
