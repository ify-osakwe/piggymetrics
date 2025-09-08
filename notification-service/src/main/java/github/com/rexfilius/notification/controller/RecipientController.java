package github.com.rexfilius.notification.controller;

import github.com.rexfilius.notification.domain.Recipient;
import github.com.rexfilius.notification.service.RecipientServiceImpl;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping("/recipients")
public class RecipientController {

	private final RecipientServiceImpl recipientService;

	public RecipientController(RecipientServiceImpl recipientService) {
		this.recipientService = recipientService;
	}

	@GetMapping("/current")
	public Object getCurrentNotificationsSettings(Principal principal) {
		return recipientService.findByAccountName(principal.getName());
	}

	@PutMapping("/current")
	public Object saveCurrentNotificationsSettings(
		Principal principal, 
		@Valid @RequestBody Recipient recipient
	) {
		return recipientService.save(principal.getName(), recipient);
	}
}
