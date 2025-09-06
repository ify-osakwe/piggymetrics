package github.com.rexfilius.accountservice.controller;

import github.com.rexfilius.accountservice.domain.Account;
import github.com.rexfilius.accountservice.domain.User;
import github.com.rexfilius.accountservice.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;

@RestController
public  class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAuthority('SCOPE_server') || #name == 'demo'")
    public Account getAccountByName(@PathVariable("name") String name) {
        return accountService.findByName(name);
    }

    @GetMapping("/current")
    public Account getCurrentAccount(Principal principal) {
        return accountService.findByName(principal.getName());
    }

    @PutMapping("/current")
    public void saveCurrentAccount(
            Principal principal,
            @Valid @RequestBody Account account
    ) {
        accountService.saveChanges(principal.getName(), account);
    }

    @PostMapping
    public Account createNewAccount(@Valid @RequestBody User user) {
        return accountService.create(user);
    }

}

/*
@RestController
public class AccountController {

	@Autowired
	private AccountService accountService;

	@PreAuthorize("#oauth2.hasScope('server') or #name.equals('demo')")
	@RequestMapping(path = "/{name}", method = RequestMethod.GET)
	public Account getAccountByName(@PathVariable String name) {
		return accountService.findByName(name);
	}

	@RequestMapping(path = "/current", method = RequestMethod.GET)
	public Account getCurrentAccount(Principal principal) {
		return accountService.findByName(principal.getName());
	}

	@RequestMapping(path = "/current", method = RequestMethod.PUT)
	public void saveCurrentAccount(Principal principal, @Valid @RequestBody Account account) {
		accountService.saveChanges(principal.getName(), account);
	}

	@RequestMapping(path = "/", method = RequestMethod.POST)
	public Account createNewAccount(@Valid @RequestBody User user) {
		return accountService.create(user);
	}
}
*/