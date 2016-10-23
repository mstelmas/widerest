package pl.touk.widerest.api.settings;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CurrencyServiceProxyTest {
    private static final String CURRENCY_CODE = "EUR";

    @Mock
    private BroadleafCurrencyService broadleafCurrencyService;

    @InjectMocks
    private CurrencyServiceProxy currencyServiceProxy;

    @Mock
    private BroadleafCurrency defaultBroadleafCurrency;

    @Mock
    private BroadleafCurrency broadleafCurrency;

    @Test
    public void shouldReturnDefaultBroadleafCurrency() throws Exception {
        currencyServiceProxy.findDefaultBroadleafCurrency();

        verify(broadleafCurrencyService).findDefaultBroadleafCurrency();
    }

    @Test
    public void shouldReturnBroadleafCurrencyByCode() throws Exception {
        currencyServiceProxy.findCurrencyByCode(CURRENCY_CODE);

        verify(broadleafCurrencyService).findCurrencyByCode(CURRENCY_CODE);
    }

    @Test
    public void shouldChangeDefaultBroadleafCurrency() throws Exception {
        when(broadleafCurrencyService.findDefaultBroadleafCurrency()).thenReturn(defaultBroadleafCurrency);

        currencyServiceProxy.setDefaultBroadleafCurrency(broadleafCurrency);

        verify(defaultBroadleafCurrency).setDefaultFlag(false);
        verify(broadleafCurrency).setDefaultFlag(true);
        verify(broadleafCurrencyService).save(defaultBroadleafCurrency);
        verify(broadleafCurrencyService).save(broadleafCurrency);
    }
}
