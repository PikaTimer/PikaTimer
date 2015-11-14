drop table results;

create table results (
    result_id int, 
    race_id int,
    bib varchar, 
    waveStart bigint,
    partStart bigint,
    partFinish bigint
);

create table split_results (
    result_id int,
    split_id int,
    split_time bigint
);


commit;
