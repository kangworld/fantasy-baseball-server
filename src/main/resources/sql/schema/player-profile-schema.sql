create table if not exists player_profile (
    date_id int not null,
    player_id bigint not null,
    team_code varchar(20) not null,
    pos varchar(20) not null,
    cost int not null,
    point int not null,
    avg_point int not null,
    point_game_count int not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (date_id, player_id)
);

create index if not exists idx_player_profile_player_id on player_profile (player_id);
create index if not exists idx_player_profile_date_id_point on player_profile (date_id, point);
