create table if not exists daily_rank (
    date_id int not null,
    rank_no int not null,
    member_id bigint not null,
    point int not null,
    display_rank_no int not null,
    previous_rank_no int not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (date_id, rank_no),
    constraint uk_daily_rank_member unique (date_id, member_id)
);

create index if not exists idx_daily_rank_member on daily_rank (member_id, date_id);
create index if not exists idx_daily_rank_point on daily_rank (date_id, point, member_id);

create table if not exists season_rank (
    date_id int not null,
    rank_no int not null,
    member_id bigint not null,
    point int not null,
    daily_point int not null,
    display_rank_no int not null,
    previous_rank_no int not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (date_id, rank_no),
    constraint uk_season_rank_member unique (date_id, member_id)
);

create index if not exists idx_season_rank_member on season_rank (member_id, date_id);
create index if not exists idx_season_rank_point on season_rank (date_id, point, member_id);
