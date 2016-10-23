package pl.touk.widerest.base;

import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class DefaultCurrencyExpectations {

    @Resource
    private BroadleafCurrencyService broadleafCurrencyService;

    public void codeEquals(final String currencyCode) {
        assertThat(broadleafCurrencyService.findDefaultBroadleafCurrency().getCurrencyCode()).isEqualToIgnoringCase(currencyCode);
    }
}
