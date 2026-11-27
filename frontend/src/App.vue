<template>
  <el-container class="app">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <path d="M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5l-8-3z" fill="currentColor" opacity="0.9"/>
            <path d="m9 12 2 2 4-4" stroke="#4f46e5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">风控系统</div>
          <div class="brand-sub">RISK CONTROL</div>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item index="/rules">
          <svg class="mi" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 6h16M4 12h10M4 18h7"/>
            <circle cx="18" cy="17" r="3"/>
            <path d="m20.5 19.5 1.5 1.5"/>
          </svg>
          <span>规则配置</span>
        </el-menu-item>
        <el-menu-item index="/vars">
          <svg class="mi" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 7V5h3M20 7V5h-3M4 17v2h3M20 17v2h-3"/>
            <path d="M9 10a2 2 0 0 1 4 0v4a2 2 0 0 0 4 0"/>
          </svg>
          <span>变量管理</span>
        </el-menu-item>
        <el-menu-item index="/flows">
          <svg class="mi" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="6" height="6" rx="1.5"/>
            <rect x="15" y="15" width="6" height="6" rx="1.5"/>
            <path d="M9 6h6a2 2 0 0 1 2 2v4m0 0-2-2m2 2 2-2"/>
          </svg>
          <span>流程编排</span>
        </el-menu-item>
        <el-menu-item index="/field">
          <svg class="mi" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 4H5a1 1 0 0 0-1 1v3"/>
            <path d="M16 4h3a1 1 0 0 1 1 1v3"/>
            <path d="M20 16v3a1 1 0 0 1-1 1h-3"/>
            <path d="M8 20H5a1 1 0 0 1-1-1v-3"/>
            <path d="M8 12h8M12 8v8"/>
          </svg>
          <span>字段补全</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <span class="dot"></span>
        <span>Engine v1.0.0</span>
      </div>
    </aside>

    <el-container class="body-wrap">
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
.app { height: 100%; }

/* ---------- 侧边栏 ---------- */
.app .sidebar {
  width: var(--rc-sidebar-w);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #101233 0%, #151a3d 55%, #1b1f4e 100%);
  padding: 20px 14px;
  box-sizing: border-box;
}
.app .brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 10px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.app .brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5 0%, #8b5cf6 100%);
  box-shadow: 0 6px 16px -6px rgba(99, 102, 241, 0.7);
  flex-shrink: 0;
}
.app .brand-name { color: #fff; font-size: 17px; font-weight: 700; letter-spacing: 0.02em; }
.app .brand-sub { color: rgba(165, 180, 252, 0.65); font-size: 10px; letter-spacing: 0.22em; margin-top: 2px; }

/* 菜单 */
.app .side-menu {
  margin-top: 16px;
  background: transparent;
  border-right: none;
  flex: 1;
}
.app .side-menu .el-menu-item {
  height: 46px;
  line-height: 46px;
  margin: 4px 0;
  border-radius: 10px;
  color: rgba(203, 213, 225, 0.82);
  transition: all 0.2s ease;
}
.app .side-menu .el-menu-item .mi { margin-right: 10px; vertical-align: -3px; }
.app .side-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.app .side-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.35) 0%, rgba(139, 92, 246, 0.28) 100%);
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 252, 0.25);
}
.app .side-menu .el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: -14px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 22px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, #818cf8, #a78bfa);
}

.app .sidebar-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 12px 2px;
  color: rgba(148, 163, 184, 0.6);
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.app .sidebar-footer .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.8);
}

/* ---------- 主内容区 ---------- */
.app .body-wrap { flex-direction: column; min-width: 0; }
.app .main {
  padding: 26px 30px 40px;
  background:
    radial-gradient(1200px 300px at 85% -50px, rgba(99, 102, 241, 0.09), transparent 70%),
    #f1f5f9;
  overflow-y: auto;
}
</style>
