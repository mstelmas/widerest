package pl.touk.widerest.api;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.security.oauth2.Scope;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CurrencyControllerTest extends AbstractTest {
    private final static String INVALID_CURRENCY_CODE = "this_is_definitely_not_a_currency";
    private final static String EUR_CURRENCY_CODE = "eur";

    @Test
    public void shouldReturnDefaultCurrencyCode() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
            whenDefaultCurrency.codeRetrieved(defaultCurrencyCode ->
                    thenDefaultCurrency.codeEquals(defaultCurrencyCode)
            )
        );
    }

    @Test
    public void shouldChangeDefaultCurrencyCode() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            whenDefaultCurrency.codeSet(EUR_CURRENCY_CODE);

            thenDefaultCurrency.codeEquals(EUR_CURRENCY_CODE);
        });
    }

    @Test
    public void shouldThrowIfInvalidCurrencyCode() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                assertThatExceptionOfType(HttpClientErrorException.class)
                        .isThrownBy(() -> whenDefaultCurrency.codeSet(INVALID_CURRENCY_CODE))
                        .has(ApiTestHttpConditions.httpNotFoundCondition)
        );
    }
}
