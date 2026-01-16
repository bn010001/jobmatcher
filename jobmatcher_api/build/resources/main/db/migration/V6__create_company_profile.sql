create table if not exists company_profile (
  id uuid primary key,
  version bigint not null default 0,

  owner_username varchar(150) not null unique,

  company_name varchar(255) not null,
  website varchar(255),
  industry varchar(120),
  location varchar(255),

  contact_name varchar(255),
  contact_email varchar(255),
  contact_phone varchar(50),

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_company_profile_owner_username
  on company_profile(owner_username);
