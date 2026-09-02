// 宿舍管理 CrudPage 配置（代码生成）
export default {
  title: '宿舍管理',
  columns: [
    { prop: 'id', label: 'ID', width: 70 },
    { prop: 'building', label: '楼栋' },
    { prop: 'roomNo', label: '房间号' },
    { prop: 'capacity', label: '容量' },
    { prop: 'remark', label: '备注' },
  ],
  formFields: [
    { prop: 'building', label: '楼栋', required: true },
    { prop: 'roomNo', label: '房间号', required: true },
    { prop: 'capacity', label: '容量', required: true },
    { prop: 'remark', label: '备注', required: false },
  ],
  keywordPlaceholder: '关键字搜索'
}
