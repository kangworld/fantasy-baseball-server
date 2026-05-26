create table if not exists member (
    member_id bigint auto_increment primary key,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    display_name varchar(50) not null,
    role varchar(20) not null,
    status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_member_email unique (email)
);

