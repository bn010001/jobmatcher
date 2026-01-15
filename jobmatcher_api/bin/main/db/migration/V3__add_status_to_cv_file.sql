alter table cv_file
  add column if not exists status varchar(32) not null default 'UPLOADED';

alter table cv_file
  add column if not exists error_message text;

update cv_file
set status = case
    when analysis_json is not null and analyzed_at is not null then 'PARSED'
    else 'UPLOADED'
end
where status is null or status = 'UPLOADED';
