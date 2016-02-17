package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.Application;
import pl.touk.widerest.security.oauth2.Scope;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringApplicationConfiguration(classes = Application.class)
public class SettingsControllerTest extends ApiTestBase {

    @Resource
    protected Collection<SettingsConsumer> settingsConsumers;

    @Test
    public void  shouldSetAKnownPropertyTest() throws IOException {

        whenLoggedIn("backoffice", "admin", "admin");
        whenAuthorizationRequestedFor(Scope.STAFF);

        final String propertyKey = settingsConsumers.iterator().next().getHandledProperties().iterator().next();
        final String propertyValue = "value";


        oAuth2RestTemplate.put(SETTINGS_BY_NAME_URL, propertyValue, serverPort, propertyKey);

        final ResponseEntity<String> receivedSettingEntity = oAuth2RestTemplate.getForEntity(SETTINGS_BY_NAME_URL, String.class, serverPort, propertyKey);

        Assert.assertTrue(receivedSettingEntity.getStatusCode().is2xxSuccessful());
        assertThat(receivedSettingEntity.getBody(), equalTo(propertyValue));
    }

}
