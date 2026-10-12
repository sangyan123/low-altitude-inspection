package com.drone.platform.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.entity.AiAlgorithm;
import com.drone.platform.module.ai.mapper.AiAlgorithmMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiAlgorithmService extends ServiceImpl<AiAlgorithmMapper, AiAlgorithm> {

    public List<AiAlgorithm> list(String keyword, Integer status) {
        LambdaQueryWrapper<AiAlgorithm> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(AiAlgorithm::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiAlgorithm::getAlgoName, keyword)
                    .or().like(AiAlgorithm::getAlgoCategory, keyword)
                    .or().like(AiAlgorithm::getAlgoDesc, keyword));
        }
        wrapper.orderByAsc(AiAlgorithm::getAlgoCategory)
                .orderByAsc(AiAlgorithm::getId);
        return list(wrapper);
    }

    public void createAlgorithm(AiAlgorithm algorithm) {
        validate(algorithm);
        if (existsByName(algorithm.getAlgoName(), null)) {
            throw new BusinessException("算法名称已存在");
        }
        algorithm.setId(null);
        if (algorithm.getStatus() == null) algorithm.setStatus(1);
        save(algorithm);
    }

    public void updateAlgorithm(AiAlgorithm algorithm) {
        if (algorithm.getId() == null) throw new BusinessException("算法ID不能为空");
        validate(algorithm);
        if (existsByName(algorithm.getAlgoName(), algorithm.getId())) {
            throw new BusinessException("算法名称已存在");
        }
        updateById(algorithm);
    }

    public void deleteAlgorithm(Long id) {
        removeById(id);
    }

    private void validate(AiAlgorithm algorithm) {
        if (!StringUtils.hasText(algorithm.getAlgoName())) throw new BusinessException("算法名称不能为空");
        if (!StringUtils.hasText(algorithm.getAlgoCategory())) throw new BusinessException("算法分类不能为空");
    }

    private boolean existsByName(String name, Long excludeId) {
        LambdaQueryWrapper<AiAlgorithm> wrapper = new LambdaQueryWrapper<AiAlgorithm>()
                .eq(AiAlgorithm::getAlgoName, name);
        if (excludeId != null) wrapper.ne(AiAlgorithm::getId, excludeId);
        return count(wrapper) > 0;
    }
}