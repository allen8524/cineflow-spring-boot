alter table movies
    add column tmdb_id bigint null;

alter table movies
    add column poster_path varchar(255) null;

alter table movies
    add column backdrop_path varchar(255) null;

alter table movies
    add column overview varchar(2000) null;

alter table movies
    add column runtime_minutes integer null;

-- release_date already exists from V1__init_schema.sql and is reused for TMDB-backed release metadata.

create unique index uk_movies_tmdb_id on movies (tmdb_id);
