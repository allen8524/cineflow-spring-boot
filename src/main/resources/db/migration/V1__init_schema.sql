create table movies (
    id bigint not null auto_increment,
    title varchar(200) not null,
    short_description varchar(300),
    description varchar(2000),
    genre varchar(100),
    age_rating varchar(20),
    running_time integer,
    poster_url varchar(255),
    booking_rate double,
    score double,
    release_date date,
    status varchar(20),
    primary key (id)
);

create table theaters (
    id bigint not null auto_increment,
    name varchar(100) not null,
    location varchar(255) not null,
    region varchar(100) not null,
    description varchar(1000),
    primary key (id)
);

create table screens (
    id bigint not null auto_increment,
    theater_id bigint not null,
    name varchar(50) not null,
    screen_type varchar(50) not null,
    total_seats integer not null,
    primary key (id)
);

create table schedules (
    id bigint not null auto_increment,
    movie_id bigint not null,
    screen_id bigint not null,
    start_time datetime(6) not null,
    end_time datetime(6) not null,
    price integer not null,
    available_seats integer not null,
    active boolean not null,
    primary key (id)
);

create table seat_templates (
    id bigint not null auto_increment,
    screen_id bigint not null,
    seat_row varchar(4) not null,
    seat_number integer not null,
    seat_code varchar(10) not null,
    seat_type varchar(20) not null,
    active boolean not null,
    primary key (id)
);

create table bookings (
    id bigint not null auto_increment,
    booking_code varchar(40) not null,
    customer_name varchar(100),
    customer_phone varchar(30),
    movie_title varchar(200),
    poster_url varchar(255),
    age_rating varchar(20),
    theater_name varchar(100),
    screen_name varchar(50),
    screen_type varchar(50),
    seat_names varchar(255),
    people_count integer,
    total_price integer,
    start_time datetime(6),
    end_time datetime(6),
    cancel_reason varchar(500),
    canceled_at datetime(6),
    schedule_id bigint,
    status varchar(20),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
);

create table schedule_seats (
    id bigint not null auto_increment,
    schedule_id bigint not null,
    seat_template_id bigint not null,
    reserved boolean not null,
    held boolean not null,
    hold_expires_at datetime(6),
    price_override integer,
    primary key (id)
);

create table booking_seats (
    id bigint not null auto_increment,
    booking_id bigint not null,
    seat_code varchar(10) not null,
    seat_row varchar(4) not null,
    seat_number integer not null,
    seat_type varchar(20) not null,
    price integer not null,
    primary key (id)
);

create table payments (
    id bigint not null auto_increment,
    booking_id bigint not null,
    method varchar(20) not null,
    amount integer not null,
    payment_status varchar(20) not null,
    paid_at datetime(6),
    canceled_at datetime(6),
    transaction_id varchar(60) not null,
    cancel_transaction_id varchar(60),
    primary key (id)
);

alter table screens
    add constraint fk_screens_theater
    foreign key (theater_id) references theaters (id);

alter table schedules
    add constraint fk_schedules_movie
    foreign key (movie_id) references movies (id);

alter table schedules
    add constraint fk_schedules_screen
    foreign key (screen_id) references screens (id);

alter table seat_templates
    add constraint fk_seat_templates_screen
    foreign key (screen_id) references screens (id);

alter table bookings
    add constraint fk_bookings_schedule
    foreign key (schedule_id) references schedules (id);

alter table schedule_seats
    add constraint fk_schedule_seats_schedule
    foreign key (schedule_id) references schedules (id);

alter table schedule_seats
    add constraint fk_schedule_seats_seat_template
    foreign key (seat_template_id) references seat_templates (id);

alter table booking_seats
    add constraint fk_booking_seats_booking
    foreign key (booking_id) references bookings (id);

alter table payments
    add constraint fk_payments_booking
    foreign key (booking_id) references bookings (id);
