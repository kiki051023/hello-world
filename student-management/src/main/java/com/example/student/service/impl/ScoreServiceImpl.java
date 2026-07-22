package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Score;
import com.example.student.mapper.ScoreMapper;
import com.example.student.service.ScoreService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, Score> implements ScoreService {

    @Override
    public IPage<Score> getScorePage(Long current, Long size, Long studentId, Long courseId, Long clazzId) {
        Page<Score> page = new Page<>(current, size);
        return baseMapper.selectScorePage(page, studentId, courseId, clazzId);
    }

    @Override
    public void addScore(Score score) {
        baseMapper.insert(score);
    }

    @Override
    public void updateScore(Score score) {
        baseMapper.updateById(score);
    }

    @Override
    public void deleteScore(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public Map<String, BigDecimal> getScoreStats(Long courseId, Long clazzId) {
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(Score::getCourseId, courseId);
        }
        List<Score> scores = baseMapper.selectList(wrapper);

        Map<String, BigDecimal> stats = new HashMap<>();
        if (scores.isEmpty()) {
            stats.put("avg", BigDecimal.ZERO);
            stats.put("max", BigDecimal.ZERO);
            stats.put("min", BigDecimal.ZERO);
            stats.put("passRate", BigDecimal.ZERO);
            return stats;
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal min = new BigDecimal("100");
        long passCount = 0;

        for (Score s : scores) {
            BigDecimal sc = s.getScore() != null ? s.getScore() : BigDecimal.ZERO;
            sum = sum.add(sc);
            if (sc.compareTo(max) > 0) max = sc;
            if (sc.compareTo(min) < 0) min = sc;
            if (sc.compareTo(new BigDecimal("60")) >= 0) passCount++;
        }

        BigDecimal avg = sum.divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);
        BigDecimal passRate = new BigDecimal(passCount)
                .divide(new BigDecimal(scores.size()), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        stats.put("avg", avg);
        stats.put("max", max);
        stats.put("min", min);
        stats.put("passRate", passRate);
        return stats;
    }
}
