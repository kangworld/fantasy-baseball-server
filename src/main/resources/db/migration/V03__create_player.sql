create table if not exists player (
    player_id bigint primary key,
    player_name varchar(100) not null,
    player_type varchar(20) not null,
    position varchar(20) not null,
    team_code varchar(20) not null,
    status varchar(20) not null,
    team_updated_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_player_player_type on player (player_type);
create index idx_player_position on player (position);
create index idx_player_team_updated_at on player (team_updated_at);
create index idx_player_player_name on player (player_name);

