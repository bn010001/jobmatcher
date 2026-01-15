alter table cv_file
  add column if not exists version bigint not null default 0;
