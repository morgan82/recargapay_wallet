-- users
create table users
(
    id         BIGINT UNSIGNED not null auto_increment,
    uuid       varchar(36)  not null,
    username   varchar(50)  not null,
    dni        varchar(255) not null,
    email      varchar(255) not null,
    first_name varchar(255) not null,
    last_name  varchar(255) not null,
    primary key (id)
) engine = InnoDB;
alter table users
    add constraint uk_users_uuid unique (uuid);
alter table users
    add constraint uk_users_dni unique (dni);
alter table users
    add constraint uk_users_email unique (email);
alter table users
    add constraint uk_user_username unique (username);

-- wallets
create table wallets
(
    balance      DECIMAL(38, 2) CHECK (balance >= 0) not null,
    created_date datetime(6)                                  not null,
    id           BIGINT UNSIGNED                              not null auto_increment,
    updated_date datetime(6)                                  not null,
    user_id      BIGINT UNSIGNED                              not null,
    cvu          varchar(36),
    uuid         varchar(36)                         not null,
    alias        varchar(100),
    extra_info   varchar(300),
    currency     varchar(255)                        not null,
    status       enum ('ACTIVE','ERROR','PENDING','REJECTED') not null,
    primary key (id)
) engine = InnoDB;

alter table wallets
    add constraint uk_wallet_cvu unique (cvu);
alter table wallets
    add constraint uk_wallet_uuid unique (uuid);
alter table wallets
    add constraint uk_wallet_alias unique (alias);
alter table wallets
    add constraint fk_wallet_user foreign key (user_id) references users (id);

-- transactions
create table transactions
(
    amount                   decimal(38, 2) not null,
    created_date             datetime(6)                                not null,
    id                       BIGINT UNSIGNED                            not null auto_increment,
    updated_date             datetime(6)                                not null,
    wallet_id                BIGINT UNSIGNED                            not null,
    uuid                     varchar(36)    not null,
    destination_account_id   varchar(100)   not null,
    source_account_id        varchar(100)   not null,
    external_tx_id           varchar(150),
    extra_info               varchar(300),
    destination_account_type enum ('BANK_CBU','BANK_CVU','WALLET_UUID') not null,
    source_account_type      enum ('BANK_CBU','BANK_CVU','WALLET_UUID') not null,
    status                   enum ('COMPLETED','FAILED','PENDING')      not null,
    transaction_type         enum ('DEPOSIT','TRANSFER','WITHDRAWAL')   not null,
    primary key (id)
) engine = InnoDB;

alter table transactions
    add constraint uk_transaction_uuid unique (uuid);
alter table transactions
    add constraint fk_transaction_wallet foreign key (wallet_id) references wallets (id);



