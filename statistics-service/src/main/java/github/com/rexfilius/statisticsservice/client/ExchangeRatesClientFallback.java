package github.com.rexfilius.statisticsservice.client;

import org.springframework.stereotype.Component;

import github.com.rexfilius.statisticsservice.model.Currency;
import github.com.rexfilius.statisticsservice.model.ExchangeRatesContainer;

import java.util.Collections;

@Component
public class ExchangeRatesClientFallback implements ExchangeRatesClient {

    @Override
    public ExchangeRatesContainer getRates(Currency base) {
        ExchangeRatesContainer container = new ExchangeRatesContainer();
        container.setBase(Currency.getBase());
        container.setRates(Collections.emptyMap());
        return container;
    }
}
