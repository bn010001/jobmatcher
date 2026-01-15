alter table cv_file
  add column if not exists updated_at timestamp not null default now();

alter table cv_file
  alter column uploaded_at set default now();
