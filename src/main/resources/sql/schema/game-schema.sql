create table if not exists game (
    game_id bigint auto_increment primary key,
    start_at timestamp not null,
    game_status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_game_start_at on game (start_at);
