import { reactive } from 'vue'
import { listDictData } from '@/api/dict'

/**
 * 字典缓存（M2-1/M2-4）：按 dictType 拉取一次后内存缓存，
 * 返回 [{ value, label }]；并提供 labelOf(type, value) 翻译。
 * ponytail: 纯内存 Map，无 TTL——字典改动低频，页面刷新即更新。
 */
const cache = reactive({})

export function useDict() {
  async function loadDict(dictType) {
    if (!cache[dictType]) {
      const res = await listDictData(dictType)
      cache[dictType] = (res.data || []).map(d => ({ value: d.dictValue, label: d.dictLabel }))
    }
    return cache[dictType]
  }

  function labelOf(dictType, value) {
    const hit = (cache[dictType] || []).find(d => d.value === value)
    return hit ? hit.label : (value ?? '')
  }

  return { loadDict, labelOf }
}
