// 实习就业模块共享组合式函数与常量
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

/** 可建立实习分配的角色 */
export function useCanAssign() {
  const userStore = useUserStore()
  return computed(() =>
    ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'TEACHER', 'PRACTICE_TEACHER'].includes(userStore.role))
}

/** 可点评周记的角色 */
export function useCanCommentLog() {
  const userStore = useUserStore()
  return computed(() => ['ADMIN', 'EDUCATION_ADMIN', 'TEACHER', 'PRACTICE_TEACHER'].includes(userStore.role))
}

/** 可管理就业/证书的角色 */
export function useCanManagePractice() {
  const userStore = useUserStore()
  return computed(() => ['ADMIN', 'EDUCATION_ADMIN', 'COUNSELOR'].includes(userStore.role))
}

export function useIsStudent() {
  const userStore = useUserStore()
  return computed(() => userStore.role === 'STUDENT')
}

/** 就业去向类型字典 */
export const DEST = {
  EMPLOYED: '就业', FURTHER_STUDY: '升学', ENTREPRENEUR: '创业',
  MILITARY: '入伍', UNEMPLOYED: '未就业', FLEXIBLE: '灵活就业'
}
export const destText = (t) => DEST[t] || t
