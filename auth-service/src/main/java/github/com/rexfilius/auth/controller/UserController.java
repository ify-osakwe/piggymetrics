package github.com.rexfilius.auth.controller;

import github.com.rexfilius.auth.domain.User;
import github.com.rexfilius.auth.service.UserServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserServiceImpl userService;

	public UserController(UserServiceImpl userService) {
		this.userService = userService;
	}
	
	@GetMapping("/current")
	public Principal getUser(Principal principal) {
		return principal;
	}

    @PreAuthorize("hasAuthority('SCOPE_server')")
    @PostMapping
    public void createUser(@Valid @RequestBody User user) {
        userService.create(user);
    }
}
