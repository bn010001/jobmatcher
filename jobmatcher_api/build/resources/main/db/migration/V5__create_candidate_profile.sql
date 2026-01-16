create table if not exists candidate_profile (
  id uuid primary key,
  version bigint not null default 0,

  owner_username varchar(150) not null unique,

  first_name varchar(120),
  last_name varchar(120),
  email varchar(255),
  phone varchar(50),
  location varchar(255),

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_candidate_profile_owner_username
  on candidate_profile(owner_username);
