package github.com.rexfilius.statisticsservice.service;

import com.google.common.collect.ImmutableMap;
import github.com.rexfilius.statisticsservice.client.ExchangeRatesClient;
import github.com.rexfilius.statisticsservice.model.Currency;
import github.com.rexfilius.statisticsservice.model.ExchangeRatesContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

@Service
public class ExchangeRatesService {

	private static final Logger log = LoggerFactory.getLogger(ExchangeRatesService.class);

	private ExchangeRatesContainer container;

	private final ExchangeRatesClient client;

	public ExchangeRatesService(ExchangeRatesClient client) {
		this.client = client;
	}
	
	public Map<Currency, BigDecimal> getCurrentRates() {
		if (container == null || !container.getDate().equals(LocalDate.now())) {
			container = client.getRates(Currency.getBase());
			log.info("exchange rates has been updated: {}", container);
		}

		return ImmutableMap.of(
				Currency.EUR, container.getRates().get(Currency.EUR.name()),
				Currency.RUB, container.getRates().get(Currency.RUB.name()),
				Currency.USD, BigDecimal.ONE
		);
	}

	
	public BigDecimal convert(Currency from, Currency to, BigDecimal amount) {

		Assert.notNull(amount, "Amount must not be null");

		Map<Currency, BigDecimal> rates = getCurrentRates();
		BigDecimal ratio = rates.get(to).divide(rates.get(from), 4, RoundingMode.HALF_UP);

		return amount.multiply(ratio);
	}
}
