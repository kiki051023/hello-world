<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon size="40" color="#409EFF"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.studentCount }}</div>
              <div class="stat-label">学生总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon size="40" color="#67C23A"><OfficeBuilding /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.clazzCount }}</div>
              <div class="stat-label">班级总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon size="40" color="#E6A23C"><Reading /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.courseCount }}</div>
              <div class="stat-label">课程总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon size="40" color="#F56C6C"><Avatar /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.teacherCount }}</div>
              <div class="stat-label">教师总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>欢迎使用</span>
          </template>
          <div class="welcome">
            <h3>欢迎使用学生管理系统 👋</h3>
            <p>当前用户：{{ userStore.realName || userStore.username }}</p>
            <p>角色：{{ roleText }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>系统功能</span>
          </template>
          <div class="features">
            <div class="feature-item">
              <el-icon size="24" color="#409EFF"><User /></el-icon>
              <span>学生信息管理</span>
            </div>
            <div class="feature-item">
              <el-icon size="24" color="#67C23A"><OfficeBuilding /></el-icon>
              <span>班级信息管理</span>
            </div>
            <div class="feature-item">
              <el-icon size="24" color="#E6A23C"><Reading /></el-icon>
              <span>课程信息管理</span>
            </div>
            <div class="feature-item">
              <el-icon size="24" color="#F56C6C"><DataAnalysis /></el-icon>
              <span>成绩管理统计</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getStudentPage } from '@/api/student'
import { getClazzList } from '@/api/clazz'
import { getCourseList } from '@/api/course'
import { getTeacherList } from '@/api/teacher'

const userStore = useUserStore()

const stats = ref({
  studentCount: 0,
  clazzCount: 0,
  courseCount: 0,
  teacherCount: 0
})

const roleText = computed(() => {
  const roleMap = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return roleMap[userStore.role] || '未知'
})

onMounted(() => {
  userStore.loadUserInfo()
  loadStats()
})

async function loadStats() {
  try {
    const [studentRes, clazzRes, courseRes, teacherRes] = await Promise.all([
      getStudentPage({ current: 1, size: 1 }),
      getClazzList(),
      getCourseList(),
      getTeacherList()
    ])
    stats.value.studentCount = studentRes.data?.total || 0
    stats.value.clazzCount = clazzRes.data?.length || 0
    stats.value.courseCount = courseRes.data?.length || 0
    stats.value.teacherCount = teacherRes.data?.length || 0
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  cursor: pointer;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.welcome {
  padding: 20px 0;
}

.welcome h3 {
  margin-bottom: 15px;
  color: #303133;
}

.welcome p {
  color: #606266;
  line-height: 2;
}

.features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 10px 0;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 14px;
}
</style>
