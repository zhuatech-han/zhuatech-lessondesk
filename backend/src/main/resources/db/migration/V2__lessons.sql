-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
create table student (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 account_id bigint not null unique,
 name varchar(120) not null,
 contact varchar(120) not null,
 note varchar(1000) not null,
 enabled boolean not null,
 foreign key(department_id) references department(id),
 foreign key(account_id) references account(id)
);
create index ix_student_dept on student(department_id,id);
create table instructor (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 account_id bigint not null unique,
 name varchar(120) not null,
 category varchar(60) not null,
 buffer_minutes int not null,
 enabled boolean not null,
 foreign key(department_id) references department(id),
 foreign key(account_id) references account(id)
);
create index ix_instructor_dept on instructor(department_id,id);
create table vehicle (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 name varchar(120) not null,
 plate varchar(24) not null,
 category varchar(60) not null,
 enabled boolean not null,
 foreign key(department_id) references department(id),
 unique(department_id,plate)
);
create index ix_vehicle_dept on vehicle(department_id,id);
create table lesson_plan (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 name varchar(120) not null,
 category varchar(60) not null,
 total_minutes int not null,
 price_minor bigint not null,
 currency varchar(3) not null,
 valid_days int not null,
 cancel_hours int not null,
 late_charge boolean not null,
 enabled boolean not null,
 foreign key(department_id) references department(id)
);
create index ix_lesson_plan_dept on lesson_plan(department_id,id);
create table inquiry (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 reference varchar(40) not null unique,
 name varchar(120) not null,
 contact varchar(120) not null,
 note varchar(1000) not null,
 status varchar(20) not null,
 followup varchar(1000) not null,
 student_id bigint,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(student_id) references student(id)
);
create index ix_inquiry_dept on inquiry(department_id,id);
create table time_window (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 instructor_id bigint,
 vehicle_id bigint,
 kind varchar(20) not null,
 starts_at timestamp(6) not null,
 ends_at timestamp(6) not null,
 note varchar(1000) not null,
 foreign key(department_id) references department(id),
 foreign key(instructor_id) references instructor(id),
 foreign key(vehicle_id) references vehicle(id)
);
create index ix_time_window_dept on time_window(department_id,id);
create index ix_window_times on time_window(starts_at,ends_at);
create table enrollment (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 student_id bigint not null,
 plan_id bigint not null,
 reference varchar(40) not null unique,
 plan_name varchar(120) not null,
 category varchar(60) not null,
 total_minutes int not null,
 held_minutes int not null,
 spent_minutes int not null,
 price_minor bigint not null,
 paid_minor bigint not null,
 refunded_minor bigint not null,
 currency varchar(3) not null,
 cancel_hours int not null,
 late_charge boolean not null,
 starts_on date not null,
 ends_on date not null,
 status varchar(20) not null,
 note varchar(1000) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(student_id) references student(id),
 foreign key(plan_id) references lesson_plan(id)
);
create index ix_enrollment_dept on enrollment(department_id,id);
create table lesson (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 enrollment_id bigint not null,
 student_id bigint not null,
 instructor_id bigint not null,
 vehicle_id bigint not null,
 reference varchar(40) not null unique,
 student_name varchar(120) not null,
 instructor_name varchar(120) not null,
 vehicle_name varchar(120) not null,
 category varchar(60) not null,
 starts_at timestamp(6) not null,
 ends_at timestamp(6) not null,
 blocked_until timestamp(6) not null,
 minutes int not null,
 buffer_minutes int not null,
 status varchar(20) not null,
 actual_minutes int not null,
 topic varchar(200) not null,
 progress varchar(1000) not null,
 note varchar(1000) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(enrollment_id) references enrollment(id),
 foreign key(student_id) references student(id),
 foreign key(instructor_id) references instructor(id),
 foreign key(vehicle_id) references vehicle(id)
);
create index ix_lesson_dept on lesson(department_id,id);
create index ix_lesson_times on lesson(starts_at,blocked_until,status);
create table credit_entry (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 enrollment_id bigint not null,
 lesson_id bigint,
 held_delta int not null,
 spent_delta int not null,
 action varchar(40) not null,
 actor varchar(60) not null,
 note varchar(1000) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(enrollment_id) references enrollment(id),
 foreign key(lesson_id) references lesson(id)
);
create table money_entry (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 enrollment_id bigint not null,
 original_id bigint,
 amount_minor bigint not null,
 kind varchar(20) not null,
 reference varchar(120) not null,
 actor varchar(60) not null,
 note varchar(1000) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(enrollment_id) references enrollment(id),
 foreign key(original_id) references money_entry(id)
);
create table business_event (
 id bigint auto_increment primary key,
 department_id bigint not null,
 version bigint not null,
 object_type varchar(30) not null,
 object_id bigint not null,
 action varchar(40) not null,
 actor varchar(60) not null,
 note varchar(2000) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id)
);
create table command_stamp(id bigint auto_increment primary key,request_id varchar(36) not null unique,actor_id bigint not null,fingerprint varchar(64) not null,result_type varchar(30) not null,result_id bigint not null,foreign key(actor_id) references account(id));
