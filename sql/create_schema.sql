
create table ping_hourly (
id int generated by default as identity primary key,
ts timestamp,
avg_response decimal(12,8),
cnt int default 1,
cnt_unreachable int default 0
);

create table ping_daily (
id int generated by default as identity primary key,
ts timestamp,
avg_response decimal(12,8)
);

create table log_unreachable (
id int generated by default as identity primary key,
ts timestamp default current_timestamp,
text varchar(20)
);

create table log (
id int generated by default as identity primary key,
ts timestamp default current_timestamp,
text varchar(8000)
);

