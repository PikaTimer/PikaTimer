create table event (id int primary key, event_name varchar(2048), event_date date);

create table race (race_id int auto_increment primary key, race_name varchar(2048), race_date date); 
create table race_participants (race_id int, participant_id int); 

create table participant ( 
    participant_id int auto_increment primary key, 
    first_name varchar(2048), 
    last_name varchar(2048), 
    age int, 
    birthday date, 
    bib_number varchar(128), 
    sex varchar(64), 
    city varchar(1024), 
    state varchar(32), 
    country varchar(256), 
    email varchar(1024)
);

create table participant_custom (particpant_id int, key varchar(128), value varchar(128));
create table participant_custom_defs (key varchar(128) primary key, type varchar(64), allowable_values array); 

create table bib2chip (bib varchar(128) primary key, chip varchar(1024) unique);

commit;
