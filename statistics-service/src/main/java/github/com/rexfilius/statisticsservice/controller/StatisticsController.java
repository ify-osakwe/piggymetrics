package github.com.rexfilius.statisticsservice.controller;

import github.com.rexfilius.statisticsservice.model.Account;
import github.com.rexfilius.statisticsservice.model.timeseries.DataPoint;
import github.com.rexfilius.statisticsservice.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/current")
	public List<DataPoint> getCurrentAccountStatistics(Principal principal) {
		return statisticsService.findByAccountName(principal.getName());
	}

	@PreAuthorize("#oauth2.hasScope('server') or #accountName.equals('demo')")
	@GetMapping("/{accountName}")
	public List<DataPoint> getStatisticsByAccountName(@PathVariable String accountName) {
		return statisticsService.findByAccountName(accountName);
	}

	@PreAuthorize("#oauth2.hasScope('server')")
	@PutMapping("/{accountName}")
	public void saveAccountStatistics(
		@PathVariable String accountName, 
		@Valid @RequestBody Account account
	) {
		statisticsService.save(accountName, account);
	}
}
