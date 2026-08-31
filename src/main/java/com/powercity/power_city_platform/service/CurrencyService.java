package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.enums.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyService {

    // Exchange rates relative to USD (base currency)
    // In a real implementation, these would come from a live API
    private static final Map<Currency, BigDecimal> EXCHANGE_RATES = new HashMap<>();
    
    static {
        EXCHANGE_RATES.put(Currency.USD, BigDecimal.ONE); // Base currency
        EXCHANGE_RATES.put(Currency.GBP, new BigDecimal("0.79")); // 1 USD = 0.79 GBP
        EXCHANGE_RATES.put(Currency.ZAR, new BigDecimal("18.50")); // 1 USD = 18.50 ZAR
        EXCHANGE_RATES.put(Currency.GHS, new BigDecimal("12.00")); // 1 USD = 12.00 GHS
        EXCHANGE_RATES.put(Currency.NGN, new BigDecimal("750.00")); // 1 USD = 750.00 NGN
    }

    /**
     * Convert amount from one currency to another
     */
    public BigDecimal convertCurrency(BigDecimal amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return amount;
        }

        // Convert to USD first, then to target currency
        BigDecimal usdAmount = convertToUSD(amount, fromCurrency);
        return convertFromUSD(usdAmount, toCurrency);
    }

    /**
     * Convert amount to USD
     */
    public BigDecimal convertToUSD(BigDecimal amount, Currency fromCurrency) {
        if (fromCurrency == Currency.USD) {
            return amount;
        }

        BigDecimal rate = EXCHANGE_RATES.get(fromCurrency);
        if (rate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + fromCurrency);
        }

        return amount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    /**
     * Convert amount from USD to target currency
     */
    public BigDecimal convertFromUSD(BigDecimal usdAmount, Currency toCurrency) {
        if (toCurrency == Currency.USD) {
            return usdAmount;
        }

        BigDecimal rate = EXCHANGE_RATES.get(toCurrency);
        if (rate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + toCurrency);
        }

        return usdAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get formatted currency display
     */
    public String formatCurrency(BigDecimal amount, Currency currency) {
        return currency.getSymbol() + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    /**
     * Get exchange rate between two currencies
     */
    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE;
        }

        BigDecimal fromRate = EXCHANGE_RATES.get(fromCurrency);
        BigDecimal toRate = EXCHANGE_RATES.get(toCurrency);

        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("Unsupported currency conversion");
        }

        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }

    /**
     * Get currency for region
     */
    public Currency getCurrencyForRegion(String region) {
        return Currency.fromRegion(region);
    }

    /**
     * Get regional pricing for an amount
     */
    public Map<String, Object> getRegionalPricing(BigDecimal baseAmount, Currency baseCurrency) {
        Map<String, Object> pricing = new HashMap<>();
        
        for (Currency currency : Currency.values()) {
            BigDecimal convertedAmount = convertCurrency(baseAmount, baseCurrency, currency);
            Map<String, Object> currencyInfo = new HashMap<>();
            currencyInfo.put("amount", convertedAmount);
            currencyInfo.put("formatted", formatCurrency(convertedAmount, currency));
            currencyInfo.put("currency", currency);
            currencyInfo.put("region", currency.getRegion());
            currencyInfo.put("symbol", currency.getSymbol());
            
            pricing.put(currency.getRegion(), currencyInfo);
        }
        
        return pricing;
    }

    /**
     * Calculate localized price for a region
     */
    public Map<String, Object> getLocalizedPrice(BigDecimal basePrice, Currency baseCurrency, String region) {
        Currency targetCurrency = getCurrencyForRegion(region);
        BigDecimal localizedPrice = convertCurrency(basePrice, baseCurrency, targetCurrency);
        
        Map<String, Object> priceInfo = new HashMap<>();
        priceInfo.put("amount", localizedPrice);
        priceInfo.put("formatted", formatCurrency(localizedPrice, targetCurrency));
        priceInfo.put("currency", targetCurrency);
        priceInfo.put("region", region);
        priceInfo.put("symbol", targetCurrency.getSymbol());
        priceInfo.put("exchangeRate", getExchangeRate(baseCurrency, targetCurrency));
        
        return priceInfo;
    }

    /**
     * Get all supported currencies
     */
    public Map<String, Object> getAllCurrencies() {
        Map<String, Object> currencies = new HashMap<>();
        
        for (Currency currency : Currency.values()) {
            Map<String, Object> currencyInfo = new HashMap<>();
            currencyInfo.put("code", currency.getCode());
            currencyInfo.put("symbol", currency.getSymbol());
            currencyInfo.put("displayName", currency.getDisplayName());
            currencyInfo.put("region", currency.getRegion());
            currencyInfo.put("exchangeRate", EXCHANGE_RATES.get(currency));
            
            currencies.put(currency.getCode(), currencyInfo);
        }
        
        return currencies;
    }

    /**
     * Validate currency code
     */
    public boolean isValidCurrency(String currencyCode) {
        try {
            Currency.fromCode(currencyCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get currency by code
     */
    public Currency getCurrencyByCode(String code) {
        return Currency.fromCode(code);
    }

    /**
     * Calculate transaction fee for cross-currency transactions
     */
    public BigDecimal calculateTransactionFee(BigDecimal amount, Currency fromCurrency, Currency toCurrency, double feePercentage) {
        if (fromCurrency == toCurrency) {
            return BigDecimal.ZERO;
        }

        BigDecimal convertedAmount = convertCurrency(amount, fromCurrency, toCurrency);
        return convertedAmount.multiply(new BigDecimal(feePercentage / 100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get minimum and maximum amounts for a currency
     */
    public Map<String, BigDecimal> getCurrencyLimits(Currency currency) {
        Map<String, BigDecimal> limits = new HashMap<>();
        
        // Set reasonable limits based on currency (these would be configurable in a real app)
        switch (currency) {
            case USD:
                limits.put("min", new BigDecimal("1.00"));
                limits.put("max", new BigDecimal("10000.00"));
                break;
            case GBP:
                limits.put("min", new BigDecimal("0.79"));
                limits.put("max", new BigDecimal("7900.00"));
                break;
            case ZAR:
                limits.put("min", new BigDecimal("18.50"));
                limits.put("max", new BigDecimal("185000.00"));
                break;
            case GHS:
                limits.put("min", new BigDecimal("12.00"));
                limits.put("max", new BigDecimal("120000.00"));
                break;
            case NGN:
                limits.put("min", new BigDecimal("750.00"));
                limits.put("max", new BigDecimal("7500000.00"));
                break;
            default:
                limits.put("min", BigDecimal.ONE);
                limits.put("max", new BigDecimal("10000.00"));
        }
        
        return limits;
    }
} 