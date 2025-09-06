package github.com.rexfilius.accountservice.client;

import github.com.rexfilius.accountservice.domain.Account;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "statistics-service", fallback = StatisticsServiceClientFallback.class)
public interface StatisticsServiceClient {

//	@RequestMapping(method = RequestMethod.PUT, value = "/statistics/{accountName}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
//	void updateStatistics(@PathVariable("accountName") String accountName, Account account);

    @PutMapping(value = "/statistics/{accountName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateStatistics(@PathVariable("accountName")String accountName, @RequestBody Account account);

}
