create table if not exists job (
  id uuid primary key,
  version bigint not null default 0,

  owner_username varchar(150) not null,

  title varchar(255) not null,
  description text,
  location varchar(255),

  -- geo per matching a distanza (Tinder-like)
  lat double precision,
  lon double precision,

  contract_type varchar(80),
  seniority varchar(80),

  -- lifecycle
  status varchar(32) not null default 'PUBLISHED',
  published_at timestamptz,
  archived_at timestamptz,

  -- per UI
  apply_url varchar(500),

  -- AI embedding (per matching vettoriale)
  embedding jsonb,
  embedding_model varchar(120),
  embedding_updated_at timestamptz,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_job_owner_username on job(owner_username);
create index if not exists idx_job_status on job(status);
create index if not exists idx_job_created_at on job(created_at);
create index if not exists idx_job_geo on job(lat, lon);