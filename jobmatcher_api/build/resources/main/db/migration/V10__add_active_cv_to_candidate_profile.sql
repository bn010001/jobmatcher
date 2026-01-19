alter table candidate_profile
  add column if not exists active_cv_file_id uuid;

do $$
begin
  alter table candidate_profile
    add constraint fk_candidate_profile_active_cv
    foreign key (active_cv_file_id)
    references cv_file(id)
    on delete set null;
exception
  when duplicate_object then null;
end $$;

create index if not exists idx_candidate_profile_active_cv
  on candidate_profile(active_cv_file_id);
