package github.com.rexfilius.statisticsservice.model;

public enum Currency {

	USD, EUR, RUB;

	public static Currency getBase() {
		return USD;
	}
}
