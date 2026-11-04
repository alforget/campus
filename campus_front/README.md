# 校园活动报名与场地预约平台 · 前端

基于 Vue 3 + Vite + Element Plus + Pinia + Vue Router，风格参考 `big_event/frontend`：深色侧边栏、顶部用户区、卡片式内容区。

## 运行

```bash
cd E:\Java\vue\projcet\campus_front
npm install
npm run dev
```

默认地址：`http://localhost:5173`。开发服务器把 `/api` 代理到 `http://localhost:8080`，所以请先启动后端。

```bash
npm run build
```

构建产物在 `dist/`。

## 角色与菜单

| roleId | 角色 | 可见菜单 |
| --- | --- | --- |
| 1 | 系统管理员 | 首页、活动广场、我的报名、通知中心、场地与时段、预约管理、活动审核、全部活动、用户管理、社团管理、个人中心 |
| 2 | 社团管理员 | 首页、活动广场、我的报名、通知中心、我的活动、场地预约、个人中心 |
| 3 | 场地管理员 | 首页、活动广场、我的报名、通知中心、场地与时段、预约管理、个人中心 |
| 4 | 学生 | 首页、活动广场、我的报名、通知中心、个人中心 |

路由守卫在 `src/router/index.js` 中按 `meta.roles` 限制页面；后端接口内部还会再校验一次角色。

## 页面

- 登录/注册：`src/views/Login.vue`
- 首页：`src/views/Dashboard.vue`
- 活动广场：`src/views/Activities.vue`（搜索、状态下拉、详情、报名）
- 我的报名：`src/views/MyRegistrations.vue`（状态筛选、票号、取消）
- 社团：`src/views/club/ClubActivities.vue`（活动 CRUD、提交审核、取消、封面上传）
- 报名名单/签到：`src/views/club/ActivityRegistrations.vue`（筛选、CSV 导出、签到）
- 场地预约：`src/views/club/ClubBooking.vue`（场馆/日期/时段下拉，草稿提交、取消）
- 场地管理：`src/views/venue/VenueManage.vue`（场馆 CRUD、时段生成/关闭）
- 预约管理：`src/views/venue/VenueBookings.vue`（状态/社团筛选、强制取消）
- 管理员：`src/views/admin/*.vue`（审核、全部活动、用户、社团）
- 通知：`src/views/Notifications.vue`
- 个人中心：`src/views/Profile.vue`（资料、头像、密码）

## 接口约定

- 后端统一返回 `{ code, msg, data }`，`code === 200`（兼容 `0`）为成功；`src/utils/request.js` 直接返回 `data`。
- 登录 token 原样放在 `Authorization` 头中，未加 `Bearer` 前缀，与后端拦截器一致。
- 角色 ID：1 管理员、2 社团管理员、3 场地管理员、4 学生。
- 字段名按 `com.campus.vo` 中的 VO 取：`ActivityVO`、`RegistrationVO`、`BookingVO`、`VenueSlotVO`、`ClubVO`、`VenueVO`、`UserVO`。

## 权限说明

后端 `/booking/list` 目前只允许场地管理员，管理员前端菜单不显示“预约管理”，首页也不会请求该接口；如果希望管理员查看全部预约，需要后端放开角色判断。

内部交接文档 `HANDOFF.md` 含本地测试账号信息，不纳入 Git。
