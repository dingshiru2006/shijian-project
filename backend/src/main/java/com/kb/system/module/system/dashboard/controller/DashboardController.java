package com.kb.system.module.system.dashboard.controller;

import com.kb.system.common.Result;
import com.kb.system.module.system.dashboard.service.DashboardService;
import com.kb.system.module.system.dashboard.vo.DashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘统计控制器
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Result<DashboardVO> stats() {
        return Result.ok(dashboardService.stats());
    }
}
