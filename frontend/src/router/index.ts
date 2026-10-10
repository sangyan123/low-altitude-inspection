import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "Login",
      component: () => import("../views/Login.vue"),
    },
    {
      path: "/",
      component: () => import("../views/Layout.vue"),
      redirect: "/dashboard",
      children: [
        { path: "dashboard", name: "Dashboard", component: () => import("../views/dashboard/Dashboard.vue"), meta: { title: "首页大屏" } },
        { path: "system/dept", name: "DeptManage", component: () => import("../views/system/DeptManage.vue"), meta: { title: "部门管理" } },
        { path: "system/user", name: "UserManage", component: () => import("../views/system/UserManage.vue"), meta: { title: "账户管理" } },
        { path: "system/role", name: "RoleManage", component: () => import("../views/system/RoleManage.vue"), meta: { title: "角色管理" } },
        { path: "system/device", name: "DeviceManage", component: () => import("../views/device/DeviceManage.vue"), meta: { title: "无人机管理" } },
      ],
    },
  ],
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  if (to.path !== "/login" && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router;

