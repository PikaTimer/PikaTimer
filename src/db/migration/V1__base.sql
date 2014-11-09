create table event (id int primary key, event_name varchar(2048), event_date date);

create table race (race_id int auto_increment, race_name varchar(2048), race_date date); 
create table race_participants (race_id int, participant_id int); 

create table participent ( 
    participent_id int auto_increment, 
    first_name varchar(2048), 
    last_name varchar(2048), 
    age int, birthday date, 
    bib_number varchar(128), 
    sex char(1), 
    city varchar(1024), 
    state varchar(32), 
    country varchar(256), 
    team_id int,
    email varchar(1024)
);

create table participent_custom (particpent_id int, key varchar(128), value varchar(128));
create table particpent_custom_defs (key varchar(128), type varchar(64), allowable_values array); 

create table bib2chip (bib varchar(128), chip varchar(1024) unique);

commit;
