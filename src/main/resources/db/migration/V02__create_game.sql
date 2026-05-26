create table if not exists game (
    game_id bigint auto_increment primary key,
    start_at timestamp not null,
    home_team_code varchar(20) not null,
    away_team_code varchar(20) not null,
    game_status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_game_start_at on game (start_at);
create index idx_game_home_team_code on game (home_team_code);
create index idx_game_away_team_code on game (away_team_code);

