create table if not exists settlement_execution (
    settlement_execution_id bigint auto_increment primary key,
    target_date date not null,
    status varchar(20) not null,
    started_at timestamp not null,
    finished_at timestamp null,
    failure_step varchar(50) null,
    failure_message varchar(1000) null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_settlement_execution_target on settlement_execution (target_date, settlement_execution_id);
create index idx_settlement_execution_status on settlement_execution (target_date, status);

create table if not exists settlement_step_execution (
    settlement_step_execution_id bigint auto_increment primary key,
    settlement_execution_id bigint not null,
    step_name varchar(50) not null,
    status varchar(20) not null,
    started_at timestamp not null,
    finished_at timestamp null,
    failure_type varchar(255) null,
    failure_message varchar(1000) null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_settlement_step_execution_parent
    on settlement_step_execution (settlement_execution_id, step_name);

