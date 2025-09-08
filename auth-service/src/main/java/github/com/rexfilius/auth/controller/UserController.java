package github.com.rexfilius.auth.controller;

import github.com.rexfilius.auth.domain.User;
import github.com.rexfilius.auth.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}
	
	@GetMapping("/current")
	public Principal getUser(Principal principal) {
		return principal;
	}

	@PreAuthorize("#oauth2.hasScope('server')")
	@PostMapping
	public void createUser(@Valid @RequestBody User user) {
		userService.create(user);
	}
}
