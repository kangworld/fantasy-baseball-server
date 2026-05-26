create table if not exists roster (
    roster_id bigint auto_increment primary key,
    member_id bigint not null,
    date_id int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_roster_member_date unique (member_id, date_id)
);

create index idx_roster_date_id on roster (date_id);

create table if not exists roster_player (
    roster_player_id bigint auto_increment primary key,
    roster_id bigint not null,
    slot_no int not null,
    player_id bigint not null,
    player_type varchar(20) not null,
    position_code varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_roster_player_slot unique (roster_id, slot_no),
    constraint uk_roster_player_player unique (roster_id, player_id)
);

create index idx_roster_player_player_id on roster_player (player_id);

