create table if not exists player (
    player_id bigint auto_increment primary key,
    player_name varchar(100) not null,
    player_type varchar(20) not null,
    position_code varchar(20) not null,
    status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_player_player_type on player (player_type);
create index if not exists idx_player_position_code on player (position_code);
create index if not exists idx_player_player_name on player (player_name);
