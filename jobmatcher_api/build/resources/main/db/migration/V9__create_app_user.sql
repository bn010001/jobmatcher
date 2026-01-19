create table if not exists app_user (
  id uuid primary key,
  version bigint not null default 0,

  username varchar(150) not null unique,
  email varchar(255) unique,

  password_hash varchar(255) not null,
  role varchar(32) not null,

  enabled boolean not null default true,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_app_user_role on app_user(role);
create index if not exists idx_app_user_created_at on app_user(created_at);
