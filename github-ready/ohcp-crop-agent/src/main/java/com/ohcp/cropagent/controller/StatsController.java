package com.ohcp.cropagent.controller;

import com.ohcp.cropagent.domain.StatsSnapshot;
import com.ohcp.cropagent.service.QueryStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final QueryStatsService queryStatsService;

    public StatsController(QueryStatsService queryStatsService) {
        this.queryStatsService = queryStatsService;
    }

    @GetMapping
    public StatsSnapshot stats() {
        return queryStatsService.snapshot();
    }
}
