package com.drone.platform.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.task.entity.RouteTemplate;
import com.drone.platform.module.task.mapper.RouteTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RouteTemplateService extends ServiceImpl<RouteTemplateMapper, RouteTemplate> {

    public List<RouteTemplate> listAll(String sceneType, String keyword) {
        LambdaQueryWrapper<RouteTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sceneType)) {
            wrapper.eq(RouteTemplate::getSceneType, sceneType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(RouteTemplate::getRouteName, keyword);
        }
        wrapper.orderByAsc(RouteTemplate::getId);
        return list(wrapper);
    }

    public void create(RouteTemplate route) {
        if (!StringUtils.hasText(route.getRouteName()) || !StringUtils.hasText(route.getSceneType())) {
            throw new BusinessException("航线名称和场景类型不能为空");
        }
        route.setStatus(1);
        save(route);
    }

    public void update(RouteTemplate route) {
        if (route.getId() == null) {
            throw new BusinessException("航线ID不能为空");
        }
        updateById(route);
    }

    public void delete(Long id) {
        removeById(id);
    }
}
