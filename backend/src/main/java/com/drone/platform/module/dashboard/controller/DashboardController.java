package com.drone.platform.module.dashboard.controller;

import com.drone.platform.common.Result;
import com.drone.platform.module.dashboard.service.DashboardService;
import com.drone.platform.module.dashboard.vo.DashboardStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.ok(dashboardService.getStats());
    }
}