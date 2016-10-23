package pl.touk.widerest.api.settings;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.Optional;

@Service
public class CurrencyServiceProxy {

    @Resource(name = "blCurrencyService")
    private BroadleafCurrencyService broadleafCurrencyService;

    public Optional<BroadleafCurrency> findDefaultBroadleafCurrency() {
        return Optional.ofNullable(broadleafCurrencyService.findDefaultBroadleafCurrency());
    }

    @Transactional
    public BroadleafCurrency setDefaultBroadleafCurrency(final BroadleafCurrency broadleafCurrency) {
        invalidateCurrentDefaultCurrency();

        broadleafCurrency.setDefaultFlag(true);

        return broadleafCurrencyService.save(broadleafCurrency);
    }

    public Optional<BroadleafCurrency> findCurrencyByCode(final String currencyCode) {
        return Optional.ofNullable(broadleafCurrencyService.findCurrencyByCode(currencyCode));
    }

    private void invalidateCurrentDefaultCurrency() {
        findDefaultBroadleafCurrency().ifPresent(currentDefaultCurrency -> {
            currentDefaultCurrency.setDefaultFlag(false);
            broadleafCurrencyService.save(currentDefaultCurrency);
        });
    }
}
