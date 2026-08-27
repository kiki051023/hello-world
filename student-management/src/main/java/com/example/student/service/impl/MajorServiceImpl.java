package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Clazz;
import com.example.student.entity.Major;
import com.example.student.entity.Student;
import com.example.student.entity.TrainingPlan;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.MajorMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.TrainingPlanMapper;
import com.example.student.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MajorServiceImpl extends ServiceImpl<MajorMapper, Major> implements MajorService {

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TrainingPlanMapper trainingPlanMapper;

    @Override
    public IPage<Major> getMajorPage(Long current, Long size, String keyword, Long departmentId) {
        return baseMapper.selectMajorPage(new Page<>(current, size), keyword, departmentId);
    }

    @Override
    public List<Major> getEnabledMajors(Long departmentId) {
        return baseMapper.selectEnabled(departmentId);
    }

    @Override
    public void addMajor(Major major) {
        if (major == null || major.getDepartmentId() == null
                || !StringUtils.hasText(major.getMajorCode())
                || !StringUtils.hasText(major.getMajorName())) {
            throw new BusinessException("所属系部、专业编码和名称不能为空");
        }
        Long exists = count(new LambdaQueryWrapper<Major>()
                .eq(Major::getDepartmentId, major.getDepartmentId())
                .eq(Major::getMajorCode, major.getMajorCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该系部下专业编码已存在");
        }
        major.setId(null);
        if (major.getStatus() == null) major.setStatus(1);
        save(major);
    }

    @Override
    public void updateMajor(Major major) {
        if (major == null || major.getId() == null) throw new BusinessException("参数错误");
        Major exist = getById(major.getId());
        if (exist == null) throw new BusinessException("专业不存在");
        Long dup = count(new LambdaQueryWrapper<Major>()
                .eq(Major::getDepartmentId, major.getDepartmentId())
                .eq(Major::getMajorCode, major.getMajorCode())
                .ne(Major::getId, major.getId()));
        if (dup != null && dup > 0) {
            throw new BusinessException("该系部下专业编码已存在");
        }
        updateById(major);
    }

    @Override
    public void deleteMajor(Long id) {
        if (id == null || getById(id) == null) throw new BusinessException("专业不存在");
        // 删除保护：有班级/学生/培养方案引用则禁止删除
        Long clazzCount = clazzMapper.selectCount(new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getMajorId, id));
        if (clazzCount != null && clazzCount > 0) {
            throw new BusinessException("该专业下存在 " + clazzCount + " 个班级，无法删除");
        }
        Long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getMajorId, id));
        if (studentCount != null && studentCount > 0) {
            throw new BusinessException("该专业下存在 " + studentCount + " 名学生，无法删除");
        }
        Long planCount = trainingPlanMapper.selectCount(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getMajorId, id));
        if (planCount != null && planCount > 0) {
            throw new BusinessException("该专业下存在 " + planCount + " 个培养方案，无法删除");
        }
        // 用 baseMapper.deleteById（逻辑删除语义相同，且可在无 TableInfo 的单元测试下验证）
        baseMapper.deleteById(id);
    }
}