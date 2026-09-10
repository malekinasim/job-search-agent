package com.nasim.jobsearch;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobSearchController {

    private final JobSearchService jobSearchService;

    public JobSearchController(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
    }
    @PostMapping("/run-job-search")
    public String runNow() throws Exception {
        String result = jobSearchService.searchJobs();
        return result;
    }
}
