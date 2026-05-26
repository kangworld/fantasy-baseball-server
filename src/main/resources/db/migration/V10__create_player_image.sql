create table if not exists player_image (
    player_id bigint primary key,
    img_path varchar(500) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

