#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""逐字核对重启和隔离恢复的课程、分钟与收退款流水。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse,json
from urllib.parse import urlparse
from http_client import Client,ROOT,env
p=argparse.ArgumentParser();p.add_argument('--base',default='http://127.0.0.1:8146');p.add_argument('--env-file',default=str(ROOT/'.env'));p.add_argument('--state',default=str(ROOT/'output/mysql-quality-state.json'));a=p.parse_args()
if urlparse(a.base).hostname not in ['127.0.0.1','localhost','::1']:p.error('Only isolated localhost instances')
c=Client(a.base);v=env(a.env_file);c.login(v.get('ADMIN_USERNAME','admin'),v['ADMIN_PASSWORD']);state=json.load(open(a.state))
for key,d in state['enrollments'].items():assert c.request('/enrollments/'+key)==d,'Enrollment differs: '+key
for key,d in state['lessons'].items():
 actual=c.request('/lessons/'+key);actual.pop('now',None);assert actual==d,'Lesson differs: '+key
assert c.request('/reports')==state['reports'],'Report snapshot differs'
print(f"Exact match: {len(state['enrollments'])} enrollment ledgers, {len(state['lessons'])} lesson histories and reports. No data loss.")
