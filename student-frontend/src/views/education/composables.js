// 教务模块共享组合式函数
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

/** 教务管理角色：可开设课程/安排任务 */
export function useCanManageEdu() {
  const userStore = useUserStore()
  return computed(() =>
    ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER'].includes(userStore.role))
}

export function useIsAdmin() {
  const userStore = useUserStore()
  return computed(() => userStore.role === 'ADMIN')
}

export function useIsStudent() {
  const userStore = useUserStore()
  return computed(() => userStore.role === 'STUDENT')
}

/** 学期列表（多个 Tab 共用，只加载一次） */
let termListPromise = null
export function loadTermListOnce(listTermsApi, termListRef) {
  if (termListPromise) return termListPromise
  termListPromise = listTermsApi()
    .then((res) => { termListRef.value = res.data || [] })
    .catch(() => { /* 学期接口失败不阻塞页面 */ })
  return termListPromise
}
