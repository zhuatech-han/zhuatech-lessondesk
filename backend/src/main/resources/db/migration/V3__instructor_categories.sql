-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
-- 扩展教练的教学类别集合，保留已有单类别；原款凭据增加数据库唯一约束。
alter table instructor modify column category varchar(500) not null;
alter table money_entry add constraint uq_money_receipt unique(department_id,reference);
