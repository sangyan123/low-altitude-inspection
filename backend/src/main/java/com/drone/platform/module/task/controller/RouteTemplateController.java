package com.drone.platform.module.task.controller;

import com.drone.platform.common.Result;
import com.drone.platform.module.task.entity.RouteTemplate;
import com.drone.platform.module.task.service.RouteTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task/route")
@RequiredArgsConstructor
public class RouteTemplateController {

    private final RouteTemplateService routeService;

    @GetMapping("/list")
    public Result<List<RouteTemplate>> list(@RequestParam(required = false) String sceneType,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(routeService.listAll(sceneType, keyword));
    }

    @PostMapping
    public Result<Void> create(@RequestBody RouteTemplate route) {
        routeService.create(route);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody RouteTemplate route) {
        routeService.update(route);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return Result.ok();
    }
}
