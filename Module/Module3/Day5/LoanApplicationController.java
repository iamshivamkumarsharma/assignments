package org.nbfc.assignment3.controller;

import org.nbfc.assignment3.model.LoanApplication;
import org.nbfc.assignment3.service.LendingAnalytics;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class LoanApplicationController {

    private final LendingAnalytics lendingAnalytics;

    public LoanApplicationController(LendingAnalytics lendingAnalytics) {
        this.lendingAnalytics = lendingAnalytics;
    }

    @PostMapping("/records")
    public ResponseEntity<Map<String, Object>> loadRecords(@RequestBody List<String> records) {
        lendingAnalytics.loadApplications(records == null ? List.of() : records);
        return ResponseEntity.ok(Map.of("loaded", true, "count", lendingAnalytics.getAllApplications().size()));
    }

    @PostMapping
    public ResponseEntity<LoanApplication> create(@RequestBody LoanApplication application) {
        try {
            LoanApplication created = lendingAnalytics.createApplication(application);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping
    public List<LoanApplication> all() {
        return lendingAnalytics.getAllApplications();
    }

    @GetMapping("/{applicationId}")
    public LoanApplication one(@PathVariable String applicationId) {
        return lendingAnalytics.getApplication(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    @PutMapping("/{applicationId}")
    public LoanApplication update(@PathVariable String applicationId, @RequestBody LoanApplication application) {
        try {
            return lendingAnalytics.updateApplication(applicationId, application);
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "Application not found".equals(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status, ex.getMessage());
        }
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> delete(@PathVariable String applicationId) {
        if (!lendingAnalytics.deleteApplication(applicationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
        return ResponseEntity.noContent().build();
    }
}
