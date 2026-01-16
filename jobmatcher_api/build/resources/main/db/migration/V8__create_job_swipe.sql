create table if not exists job_swipe (
  id uuid primary key,
  version bigint not null default 0,

  candidate_username varchar(150) not null,
  job_id uuid not null references job(id) on delete cascade,

  action varchar(16) not null, -- LIKE / DISLIKE
  created_at timestamptz not null default now(),

  unique(candidate_username, job_id)
);

create index if not exists idx_job_swipe_candidate
  on job_swipe(candidate_username);

create index if not exists idx_job_swipe_job
  on job_swipe(job_id);

create index if not exists idx_job_swipe_action
  on job_swipe(action);
