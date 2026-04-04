create table users (
    id bigint not null auto_increment,
    login_id varchar(50) not null,
    email varchar(120) not null,
    password varchar(255) not null,
    name varchar(50) not null,
    phone varchar(30) not null,
    role varchar(20) not null,
    enabled boolean not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
);

alter table users
    add constraint uk_users_login_id unique (login_id);

alter table users
    add constraint uk_users_email unique (email);

create index idx_users_role on users (role);
