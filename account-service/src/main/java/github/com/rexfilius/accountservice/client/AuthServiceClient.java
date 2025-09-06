package github.com.rexfilius.accountservice.client;

import github.com.rexfilius.accountservice.domain.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

//	@RequestMapping(
//            method = RequestMethod.POST,
//            value = "/uaa/users",
//            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
//    )
    @PostMapping(value = "/uaa/users", consumes = MediaType.APPLICATION_JSON_VALUE)
	void createUser(@RequestBody User user);

}
