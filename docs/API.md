# LessonDesk 接口说明

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

所有业务接口/api，同源会话；写入先GET/auth/csrf获取请求头名与token。登录成功刷新CSRF，不把Cookie复制到公开资料。错误为HTTP状态及code，不返回堆栈。

| 路径 | 行为与授权 |
|---|---|
| POST /auth/login；GET /auth/me；POST /auth/logout、/auth/password | 登录、当前权限、退出、本人改密码 |
| GET /public/catalog；POST /public/inquiries | 公开套餐，新增私有询盘，仅返回回执；POST仍要CSRF |
| GET /options | 按岗位过滤最小目录；学员只含本人档案，教练无学生联系人 |
| GET /records/{type} | lessons、enrollments、students、instructors、vehicles、plans、windows、inquiries；search/status/page/size/sort；size1–100，page从0；items/total |
| POST /masters/{type}；PUT/DELETE /masters/{type}/{id} | 版本化主数据；DELETE需version查询参数；windows允许教练本人 |
| POST /enrollments；GET /enrollments/{id} | 购买快照、原款和分钟流水 |
| POST /enrollments/{id}/commands/{action} | COLLECT、CLOSE、REFUND；finance或enroll权限 |
| GET /slots | enrollmentId/instructorId/vehicleId/date/minutes，可选lessonId用于改期；只返回空档 |
| POST /lessons；GET /lessons/{id} | 报读version、三个资源和分钟预留 |
| POST /lessons/{id}/commands/{action} | RESCHEDULE/CANCEL/ARRIVE/START/FINISH/NOSHOW；本人、指定教练或授权管理岗位 |
| GET /reports、/audit、/report.csv | 分校统计、审计或导出权限 |
| GET /admin/options、/admin/{type}；POST/PUT /admin/{type}[/{id}] | users/roles/permissions/departments/menus/dictionaries/settings；ALL及各自权限 |

新建报读、约课和所有命令带UUID requestId；命令带当前version，约课带enrollmentVersion。金额amountMinor/priceMinor为最小货币单位正整数，套餐可0价；仅支持两位小数币种，不接受小数最小单位。REFUND带本报读原收款originalId、唯一reference和note。FINISH带actualMinutes（0至预约分钟）、topic、progress、note。

主数据均带departmentId；账号身份绑定后不能变。教练category为1–8个目录代码组成的逗号集合，服务端校验后规范排序；页面用复选框。车辆/套餐category单代码。窗口kind=WORK或BLOCK，instructorId/vehicleId恰选一个，WORK只能教练；起止为整分钟ISO UTC。

查空档date按分校IANA时区，显示结果可按浏览器时区转换。搜索不锁资源，真正创建时再次核验。学员改期以取消提前小时快照限制，逾期取消按lateCharge计时；管理取消保留豁免理由。404不存在、400输入、401登录、403权限、409状态/冲突/版本/余额、413目录保护、429来源限额。网络中断不自动重发变更，先读取原单确认结果。
