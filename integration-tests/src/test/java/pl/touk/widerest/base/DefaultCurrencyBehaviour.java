package pl.touk.widerest.base;

import javaslang.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

import static pl.touk.widerest.base.ApiTestUrls.DEFAULT_CURRENCY_URL;

@RequiredArgsConstructor
public class DefaultCurrencyBehaviour extends ApiBaseBehaviour {

    private final RestTemplate restTemplate;
    private final String serverPort;

    public void codeRetrieved(final Try.CheckedConsumer<String>... thens) throws Throwable {
        when(() -> restTemplate.getForObject(DEFAULT_CURRENCY_URL, String.class, serverPort), thens);
    }

    public void codeSet(final String currencyCode, final Try.CheckedConsumer<Void>... thens) throws Throwable {
        when(() -> {
            restTemplate.put(DEFAULT_CURRENCY_URL, currencyCode, serverPort);
            return null;
        }, thens);
    }
}
