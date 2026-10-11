#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""仅在独立本机空库创建TEST资料，验收真实MySQL闭环与越权。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse, concurrent.futures, datetime as dt, json, secrets, uuid
from pathlib import Path
from urllib.parse import urlparse
from http_client import Client, ROOT, env

def main():
    p=argparse.ArgumentParser();p.add_argument('--base',default='http://127.0.0.1:8146');p.add_argument('--env-file',default=str(ROOT/'.env'));a=p.parse_args()
    if urlparse(a.base).hostname not in ['localhost','127.0.0.1','::1']:p.error('Only isolated localhost QA is allowed')
    c=Client(a.base);values=env(a.env_file);c.login(values.get('ADMIN_USERNAME','admin'),values['ADMIN_PASSWORD'])
    assert c.request('/records/lessons')['total']==0 and c.request('/records/enrollments')['total']==0,'Refuse nonempty business database'
    assert c.request('/records/students')['total']==0,'Refuse existing student profiles'
    pw='Aa9'+secrets.token_urlsafe(25)
    def command(**b):return {**b,'requestId':str(uuid.uuid4()),'note':b.get('note','TEST verified action')}
    def master(resource,**b):return c.request('/masters/'+resource,{'departmentId':1,**b})
    def ed(i):return c.request('/enrollments/'+str(i))
    def ld(i):return c.request('/lessons/'+str(i))
    def ec(i,action,status=200,**b):return c.request(f'/enrollments/{i}/commands/{action}',command(version=ed(i)['enrollment']['version'],**b),status)
    def act(actor,i,action,status=200,**b):return actor.request(f'/lessons/{i}/commands/{action}',command(version=ld(i)['lesson']['version'],**b),status)
    d=c.request('/admin/departments')[0];d['zone']='UTC';c.request('/admin/departments/1',d,method='PUT')
    setting=next(x for x in c.request('/admin/settings') if x['code']=='bookingLeadMinutes');setting['value']='0';c.request('/admin/settings/'+str(setting['id']),setting,method='PUT')
    accounts={};actors={}
    for name,role in [('lesson-p1',5),('lesson-p2',5),('lesson-p3',5),('lesson-coach1',4),('lesson-coach2',4),('lesson-office',2),('lesson-finance',3),('lesson-ui',1)]:
        accounts[name]=c.request('/admin/users',{'username':name,'displayName':'TEST '+name,'roleId':role,'departmentId':1,'password':pw,'enabled':True})
        actors[name]=Client(a.base);actors[name].login(name,pw)
    students=[master('students',name='TEST 学员'+str(i+1),contact=f'student{i+1}@example.invalid',accountId=accounts[f'lesson-p{i+1}']['id'],note='TEST private enrollment contact',enabled=True) for i in range(3)]
    coaches=[master('instructors',name='TEST 教练'+str(i+1),accountId=accounts[f'lesson-coach{i+1}']['id'],category='AUTO',bufferMinutes=15,enabled=True) for i in range(2)]
    vehicles=[master('vehicles',name='TEST 自动挡教练车'+str(i+1),plate='TEST-CAR-'+str(i+1),category='AUTO',enabled=True) for i in range(2)]
    plans=[master('plans',name='TEST 自动挡10小时套餐',category='AUTO',totalMinutes=600,priceMinor=360000,currency='CNY',validDays=90,cancelHours=24,lateCharge=True,enabled=True),master('plans',name='TEST International 6h Package',category='AUTO',totalMinutes=360,priceMinor=30000,currency='GBP',validDays=90,cancelHours=12,lateCharge=False,enabled=True)]
    now=dt.datetime.now(dt.timezone.utc).replace(second=0,microsecond=0);start=now+dt.timedelta(minutes=2)
    def iso(t):return t.isoformat().replace('+00:00','Z')
    for coach in coaches:
        master('windows',kind='WORK',instructorId=coach['id'],vehicleId=None,startsAt=iso(now-dt.timedelta(hours=1)),endsAt=iso(now+dt.timedelta(hours=10)),note='TEST published instructor work')
        master('windows',kind='WORK',instructorId=coach['id'],vehicleId=None,startsAt=iso(now+dt.timedelta(days=2)),endsAt=iso(now+dt.timedelta(days=2,hours=8)),note='TEST future work')
    enrollments=[]
    for index,s in enumerate(students):
        e=c.request('/enrollments',command(studentId=s['id'],planId=plans[0]['id'],startsOn=now.date().isoformat()))['enrollment'];enrollments.append(e)
        actors[f'lesson-p{index+1}'].request('/lessons',command(enrollmentId=e['id'],enrollmentVersion=e['version'],instructorId=coaches[0]['id'],vehicleId=vehicles[0]['id'],minutes=60,startsAt=iso(start)),409)
        ec(e['id'],'COLLECT',amountMinor=360000,reference='TEST-PAID-'+str(index+1))
    def bookdata(enrol,time=start,coach=0,car=0,minutes=60):return command(enrollmentId=enrol,enrollmentVersion=ed(enrol)['enrollment']['version'],instructorId=coaches[coach]['id'],vehicleId=vehicles[car]['id'],minutes=minutes,startsAt=iso(time))
    e=enrollments[0]['id'];p1=actors['lesson-p1'];p2=actors['lesson-p2'];teach=actors['lesson-coach1']
    b=bookdata(e);lesson=p1.request('/lessons',b)['lesson'];repeat=p1.request('/lessons',b)['lesson'];assert repeat['id']==lesson['id'];assert ed(e)['enrollment']['heldMinutes']==60
    changed={**b,'minutes':30};p1.request('/lessons',changed,409)
    p2.request('/lessons',bookdata(enrollments[1]['id'],coach=0,car=1),409);p2.request('/lessons',bookdata(enrollments[1]['id'],coach=1,car=0),409)
    p2.request('/lessons/'+str(lesson['id']),status=403);p2.request('/enrollments/'+str(e),status=403);teach.request('/enrollments/'+str(e),status=403);p1.request('/admin/users',status=403);p1.request('/records/inquiries',status=403)
    assert not any('contact' in json.dumps(x) for x in teach.request('/options')['students'])
    p1.request('/lessons',bookdata(e,coach=1,car=1),409);p1.request('/lessons',bookdata(e),403,csrf=False)
    Client(a.base).request('/records/lessons',status=401)
    public=Client(a.base);catalog=public.request('/public/catalog');assert 'contact' not in json.dumps(catalog) and 'accountId' not in json.dumps(catalog)
    q=public.request('/public/inquiries',{'departmentId':1,'name':'TEST 咨询学员','contact':'inquiry@example.invalid','note':'TEST 自动挡培训咨询','website':''});assert set(q)=={'reference','received'}
    public.request('/public/inquiries',{'departmentId':1,'name':'TEST bot','contact':'bot@example.invalid','note':'TEST','website':'spam'},409)
    # Exact concurrent retries use separate sessions, and the request is executed once.
    c1=Client(a.base);c2=Client(a.base);c1.login('lesson-p2',pw);c2.login('lesson-p2',pw);parallel=bookdata(enrollments[1]['id'],coach=1,car=1)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures=[pool.submit(x.request,'/lessons',parallel) for x in [c1,c2]];parallel_results=[f.result() for f in futures]
    assert parallel_results[0]['lesson']['id']==parallel_results[1]['lesson']['id'];assert ed(enrollments[1]['id'])['enrollment']['heldMinutes']==60
    parallel_id=parallel_results[0]['lesson']['id'];act(c,parallel_id,'CANCEL');assert ed(enrollments[1]['id'])['enrollment']['heldMinutes']==0
    # Early check-in is permitted within 15 minutes; taught minutes are instructor attestations.
    act(teach,lesson['id'],'NOSHOW',409);act(p1,lesson['id'],'ARRIVE');act(actors['lesson-coach2'],lesson['id'],'START',403);act(teach,lesson['id'],'START')
    act(c,lesson['id'],'CANCEL',409);act(teach,lesson['id'],'FINISH',400,actualMinutes=61,topic='TEST steering',progress='TEST')
    act(teach,lesson['id'],'FINISH',actualMinutes=45,topic='TEST 起步与停车',progress='TEST 完成45分钟训练；停车步骤需继续练习')
    assert ed(e)['enrollment']['spentMinutes']==45 and ed(e)['enrollment']['heldMinutes']==0
    # Planned buffer is retained when a lesson ends early.
    p2.request('/lessons',bookdata(enrollments[1]['id'],start+dt.timedelta(minutes=60),coach=0,car=0),409)
    adjacent=p2.request('/lessons',bookdata(enrollments[1]['id'],start+dt.timedelta(minutes=75)))['lesson'];act(c,adjacent['id'],'CANCEL')
    # Reschedule does not alter held credits. Student late cancellation follows the purchased rule.
    future=start+dt.timedelta(hours=4);next_lesson=p1.request('/lessons',bookdata(e,future))['lesson'];act(c,next_lesson['id'],'RESCHEDULE',startsAt=iso(future+dt.timedelta(minutes=75)),instructorId=coaches[0]['id'],vehicleId=vehicles[0]['id']);assert ed(e)['enrollment']['heldMinutes']==60
    act(p1,next_lesson['id'],'RESCHEDULE',409,startsAt=iso(future+dt.timedelta(minutes=150)),instructorId=coaches[0]['id'],vehicleId=vehicles[0]['id'])
    act(p1,next_lesson['id'],'CANCEL');assert ed(e)['enrollment']['spentMinutes']==105
    ec(e,'COLLECT',409,amountMinor=1,reference='TEST-EXCESS');ec(e,'CLOSE');payment=ed(e)['payments'][0]['id'];refundable=ed(e)['refundableMinor'];assert refundable==297000
    ec(e,'REFUND',amountMinor=refundable,originalId=payment,reference='TEST-REFUND-1');assert ed(e)['refundableMinor']==0
    ec(e,'REFUND',409,amountMinor=1,originalId=payment,reference='TEST-REFUND-EXCESS')
    # A second currency is accounted separately and remains available for screenshots / UI.
    gbp=c.request('/enrollments',command(studentId=students[0]['id'],planId=plans[1]['id'],startsOn=now.date().isoformat()))['enrollment'];ec(gbp['id'],'COLLECT',amountMinor=30000,reference='TEST-GBP-PAID')
    pending=p2.request('/lessons',bookdata(enrollments[1]['id'],start+dt.timedelta(days=2),coach=0,car=0))['lesson']
    block=master('windows',kind='BLOCK',instructorId=None,vehicleId=vehicles[1]['id'],startsAt=iso(start+dt.timedelta(days=2,hours=2)),endsAt=iso(start+dt.timedelta(days=2,hours=3)),note='TEST 车辆保养')
    ec(enrollments[1]['id'],'CLOSE',409)
    car=c.request('/records/vehicles?search=TEST-CAR-1')['items'][0];c.request('/masters/vehicles/'+str(car['id']),{**car,'enabled':False},409,method='PUT')
    export=c.request('/report.csv',binary=True);assert b'contact' not in export
    assert len(c.request('/records/lessons?size=1')['items'])==1
    assert c.request('/records/lessons?search=does-not-exist')['total']==0
    reports=c.request('/reports');assert len(reports['currencies'])==2
    assert sum(e['netMinor'] for e in reports['currencies'] if e['currency']=='CNY')==783000
    output=ROOT/'output';output.mkdir(exist_ok=True)
    state={'enrollments':{str(e['id']):ed(e['id']) for e in enrollments+[gbp]},'lessons':{str(l['id']):ld(l['id']) for l in c.request('/records/lessons?size=100')['items']},'reports':reports}
    # Detail includes volatile now only on lesson DTO; remove it for persistence comparisons.
    for d in state['lessons'].values():d.pop('now',None)
    path=output/'mysql-quality-state.json';path.write_text(json.dumps(state,ensure_ascii=False,indent=2));path.chmod(0o600)
    ui=output/'qa-private.json';ui.write_text(json.dumps({'password':pw,'accounts':accounts,'students':students,'instructors':coaches,'vehicles':vehicles,'plans':plans,'gbpId':gbp['id'],'pendingId':pending['id'],'utcDate':now.date().isoformat()},ensure_ascii=False));ui.chmod(0o600)
    clients=[c,p1,p2,teach,actors['lesson-coach2'],public,c1,c2];checks=[check for client in clients for check in client.checks]
    result={'result':'passed','requests':len(checks),'checks':checks,'coverage':['three-resource-conflicts','paid-before-booking','minute-conservation','concurrent-idempotency','late-cancel','reschedule','teacher-permissions','student-privacy','csrf','public-inquiry','original-refund','buffer-boundary','currency-separated-reports']};(output/'mysql-quality.json').write_text(json.dumps(result,ensure_ascii=False,indent=2));print(f'Passed {len(checks)} tracked HTTP checks; exact business state saved privately.')
if __name__=='__main__':main()
