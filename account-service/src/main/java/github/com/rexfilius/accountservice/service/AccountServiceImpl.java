package github.com.rexfilius.accountservice.service;

import github.com.rexfilius.accountservice.client.AuthServiceClient;
import github.com.rexfilius.accountservice.client.StatisticsServiceClient;
import github.com.rexfilius.accountservice.domain.Account;
import github.com.rexfilius.accountservice.domain.Currency;
import github.com.rexfilius.accountservice.domain.Saving;
import github.com.rexfilius.accountservice.domain.User;
import github.com.rexfilius.accountservice.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class AccountServiceImpl implements AccountService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final StatisticsServiceClient statisticsClient;
	private final AuthServiceClient authClient;
	private final  AccountRepository repository;

    public AccountServiceImpl(
            StatisticsServiceClient statisticsClient,
            AuthServiceClient authClient,
            AccountRepository repository
    ) {
        this.statisticsClient = statisticsClient;
        this.authClient = authClient;
        this.repository = repository;
    }


	@Override
	public Account findByName(String accountName) {
		Assert.hasLength(accountName, "accountName cannot be empty");
		return repository.findByName(accountName);
	}


	@Override
	public Account create(User user) {

		Account existing = repository.findByName(user.getUsername());
		Assert.isNull(existing, "account already exists: " + user.getUsername());

		authClient.createUser(user);

		Saving saving = new Saving();
		saving.setAmount(new BigDecimal(0));
		saving.setCurrency(Currency.getDefault());
		saving.setInterest(new BigDecimal(0));
		saving.setDeposit(false);
		saving.setCapitalization(false);

		Account account = new Account();
		account.setName(user.getUsername());
		account.setLastSeen(new Date());
		account.setSaving(saving);

		repository.save(account);
        log.info("new account has been created: {}", account.getName());
		return account;
	}


	@Override
	public void saveChanges(String name, Account update) {
		Account account = repository.findByName(name);
		Assert.notNull(account, "can't find account with name " + name);

		account.setIncomes(update.getIncomes());
		account.setExpenses(update.getExpenses());
		account.setSaving(update.getSaving());
		account.setNote(update.getNote());
		account.setLastSeen(new Date());
		repository.save(account);
		log.debug("account {} changes has been saved", name);
		statisticsClient.updateStatistics(name, account);
	}
}
