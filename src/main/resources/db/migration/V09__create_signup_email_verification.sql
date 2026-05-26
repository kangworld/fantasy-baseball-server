create table if not exists signup_email_verification (
    signup_email_verification_id bigint auto_increment primary key,
    email varchar(255) not null,
    code_hash varchar(255) not null,
    code_expires_at timestamp not null,
    verified_at timestamp null,
    verified_expires_at timestamp null,
    consumed_at timestamp null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_signup_email_verification_email_created
    on signup_email_verification (email, created_at);

