package com.drone.platform.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.mapper.FlightPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class FlightPlanService extends ServiceImpl<FlightPlanMapper, FlightPlan> {

    public Page<FlightPlan> page(int current, int size, Long deptId, Integer approvalStatus) {
        LambdaQueryWrapper<FlightPlan> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) wrapper.eq(FlightPlan::getDeptId, deptId);
        if (approvalStatus != null) wrapper.eq(FlightPlan::getApprovalStatus, approvalStatus);
        wrapper.orderByDesc(FlightPlan::getId);
        return page(new Page<>(current, size), wrapper);
    }

    public void create(FlightPlan plan) {
        if (plan.getPlanStartTime() == null || plan.getPlanEndTime() == null) {
            throw new BusinessException("计划起止时间不能为空");
        }
        if (plan.getPlanEndTime().isBefore(plan.getPlanStartTime())) {
            throw new BusinessException("计划结束时间不能早于开始时间");
        }
        plan.setPlanNo(generatePlanNo());
        if (plan.getApprovalStatus() == null) plan.setApprovalStatus(0);
        if (plan.getAirspaceCheck() == null) plan.setAirspaceCheck(0);
        save(plan);
    }

    public void update(FlightPlan plan) {
        if (plan.getId() == null) {
            throw new BusinessException("计划ID不能为空");
        }
        updateById(plan);
    }

    public void delete(Long id) {
        removeById(id);
    }

    /** 审批 */
    public void approve(Long id, Long approvalUserId, int approvalStatus, String remark) {
        FlightPlan plan = getById(id);
        if (plan == null) throw new BusinessException("计划不存在");
        if (plan.getApprovalStatus() != 0) throw new BusinessException("该计划已审批");
        plan.setApprovalStatus(approvalStatus);
        plan.setApprovalUserId(approvalUserId);
        plan.setApprovalTime(LocalDateTime.now());
        plan.setApprovalRemark(remark);
        updateById(plan);
    }

    private String generatePlanNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new Random().nextInt(900) + 100;
        return "FP" + date + rand;
    }
}
