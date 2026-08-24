# Excel 导入导出 使用说明

学生管理系统现已支持 **学生 / 班级 / 课程 / 成绩 / 教师** 5 个模块的 Excel 批量导入与导出，基于 Alibaba EasyExcel 实现。导入、导出均**仅限管理员（ADMIN）**操作。

## 技术实现要点

- **后端**：`com.alibaba.easyexcel 3.3.4`；每个模块提供 `GET /export`（导出 `.xlsx`）与 `POST /import`（`MultipartFile` 导入）。
- **导入**：在服务层用 EasyExcel 同步读取，自动按"名称"反查关联 ID（如班级名→班级ID、课程名→课程ID、学号→学生ID），并做必填 / 重复（与库内冲突）校验；逐行记录失败原因，成功行批量写入。
- **导出**：按当前列表查询条件过滤（学生/班级/课程支持关键字与下拉过滤），并把关联名称（班级名、教师名、课程名、学生名）一并写出，便于直接查看。
- **前端**：列表页顶部「导出」「导入」按钮（仅管理员可见）。导出走 `blob` 下载；导入用 `el-upload` 选择文件后自动提交，结果提示成功 / 失败条数。

## Excel 模板字段（表头必须与下列中文列名一致，顺序随意）

### 学生（学生信息.xlsx）
| 学号 | 姓名 | 性别 | 出生日期 | 联系电话 | 班级 | 家庭住址 |
- 班级：填**班级名称**，系统反查班级 ID；留空则无班级。
- 性别：男 / 女。出生日期：`yyyy-MM-dd`。

### 班级（班级信息.xlsx）
| 班级名称 | 年级 | 班主任 |
- 班主任：填**教师姓名**，反查教师 ID；留空则无班主任。

### 课程（课程信息.xlsx）
| 课程编号 | 课程名称 | 学分 | 任课教师 |
- 任课教师：填**教师姓名**，反查教师 ID；留空则无任课教师。

### 成绩（成绩信息.xlsx）
| 学号 | 姓名 | 课程名称 | 成绩 | 考试日期 |
- 学号 + 课程名称 唯一定位一条成绩；姓名随学号反查填充（可不填，仅作展示）。
- 考试日期：`yyyy-MM-dd`。

### 教师（教师信息.xlsx）
| 教师编号 | 姓名 | 性别 | 联系电话 |
- 性别：男 / 女。

## 导入校验规则

1. 必填项为空 → 该行标记失败（如「第3行: 学号不能为空」）。
2. 学号 / 教师编号 / 班级名称 / 课程名称 与库内重复 → 跳过并标记「已存在」。
3. 关联名称（班级 / 教师 / 课程 / 学生）不存在 → 该行标记失败。
4. 成绩为空 → 失败。
5. 接口返回 `{ total, successCount, failRows }`（`failRows` 为失败行对象数组，每条含原表字段 + `错误信息`）。前端导入完成后：若有失败行，会弹出确认框**「是否下载错误明细 Excel（含失败原因）」**，点击即下载一份带「错误信息」列的 `.xlsx`（如 `学生导入失败明细.xlsx`），可直接看到每行错在哪、为什么错；全部成功则仅提示成功条数。

## 接口一览（均需 ADMIN 角色；import 表单字段名固定为 `file`）

| 模块 | 导出 | 导入 | 失败明细导出（POST，body 为失败行数组） |
| --- | --- | --- | --- |
| 学生 | `GET /api/student/export` | `POST /api/student/import` | `POST /api/student/import-fail-export` |
| 班级 | `GET /api/clazz/export` | `POST /api/clazz/import` | `POST /api/clazz/import-fail-export` |
| 课程 | `GET /api/course/export` | `POST /api/course/import` | `POST /api/course/import-fail-export` |
| 成绩 | `GET /api/score/export` | `POST /api/score/import` | `POST /api/score/import-fail-export` |
| 教师 | `GET /api/teacher/export` | `POST /api/teacher/import` | `POST /api/teacher/import-fail-export` |

## 快速验证建议

1. 先用页面「导出」拿到一份标准模板（含正确表头与现有数据）。
2. 在该模板上修改 / 增行，再用「导入」回填。
3. 故意留空必填项，验证失败行提示与控制台日志。
