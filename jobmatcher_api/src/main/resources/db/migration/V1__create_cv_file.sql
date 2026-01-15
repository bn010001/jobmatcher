create table if not exists cv_file (
  id uuid primary key,
  owner_username varchar(255) not null,
  original_filename varchar(1024) not null,
  content_type varchar(255) not null,
  size_bytes bigint not null,
  storage_path varchar(1024) not null,
  uploaded_at timestamp not null,
  analysis_json jsonb,
  analyzed_at timestamp
);

create index if not exists idx_cv_file_owner on cv_file(owner_username);
create index if not exists idx_cv_file_uploaded_at on cv_file(uploaded_at);
