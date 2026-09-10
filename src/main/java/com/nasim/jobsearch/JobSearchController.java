package com.nasim.jobsearch;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobSearchController {

    private final JobSearchService jobSearchService;

    public JobSearchController(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
    }

    /**
     * Manual trigger for testing: POST http://localhost:8080/run-job-search
     * The scheduled run in JobSearchService fires automatically every day at 08:00
     * without needing this endpoint at all.
     */
    @PostMapping("/run-job-search")
    public String runNow() throws Exception {
        String result = jobSearchService.searchJobs();
        return result;
    }
}
