create table if not exists fantasy_point_rule (
    player_type varchar(20) not null,
    record_code varchar(50) not null,
    point int not null,
    description varchar(255) not null,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (player_type, record_code)
);

create table if not exists player_daily_point (
    date_id int not null,
    game_id varchar(50) not null,
    player_id bigint not null,
    point int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (date_id, game_id, player_id)
);

create index if not exists idx_player_daily_point_player on player_daily_point (player_id, date_id);
create index if not exists idx_player_daily_point_date_point on player_daily_point (date_id, point);

create table if not exists kbo_game_record_raw (
    game_id varchar(50) primary key,
    record_json json not null,
    fetched_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_kbo_game_record_raw_fetched_at on kbo_game_record_raw (fetched_at);

insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_HIT', 2, '안타', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_HIT');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_2B', 2, '2루타 추가', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_2B');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_3B', 4, '3루타 추가', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_3B');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_HR', 6, '홈런 추가', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_HR');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_BB', 1, '볼넷', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_BB');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_HBP', 1, '몸에 맞는 공', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_HBP');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_RUN', 2, '득점', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_RUN');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_RBI', 2, '타점', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_RBI');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_SH', 1, '희생 번트', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_SH');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_SF', 1, '희생 플라이', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_SF');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_SB', 3, '도루', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_SB');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_CS', -2, '도루자', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_CS');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_RUNNER_OUT', -2, '주루사', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_RUNNER_OUT');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_SO', -1, '삼진', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_SO');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'BATTER', 'BAT_GDP', -3, '병살', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'BATTER' and record_code = 'BAT_GDP');

insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_OUT', 1, '아웃카운트', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_OUT');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_SO', 2, '탈삼진', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_SO');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_WIN', 5, '승리', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_WIN');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_LOSS', -3, '패전', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_LOSS');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_HOLD', 4, '홀드', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_HOLD');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_SAVE', 5, '세이브', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_SAVE');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_QS', 4, '퀄리티 스타트', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_QS');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_QSP', 6, '퀄리티 스타트+', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_QSP');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_HIT', -1, '피안타', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_HIT');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_HR', -4, '피홈런 추가', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_HR');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_BB', -1, '볼넷 허용', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_BB');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_HBP', -1, '몸에 맞는 공 허용', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_HBP');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_ER', -3, '자책점', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_ER');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_WP', -1, '폭투', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_WP');
insert into fantasy_point_rule (player_type, record_code, point, description, active, created_at, updated_at)
select 'PITCHER', 'PIT_BK', -1, '보크', true, current_timestamp, current_timestamp
where not exists (select 1 from fantasy_point_rule where player_type = 'PITCHER' and record_code = 'PIT_BK');
