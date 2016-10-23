package pl.touk.widerest.api.settings;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;

import javax.annotation.Resource;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/currency")
@Api(value = "/currency", description = "Currency endpoint")
public class CurrencyController {

    @Resource
    private CurrencyServiceProxy currencyServiceProxy;

    @ResponseBody
    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation("Get default currency code")
    public String getDefaultCurrencyCode() {
        return currencyServiceProxy.findDefaultBroadleafCurrency()
                .map(BroadleafCurrency::getCurrencyCode)
                .orElseThrow(CurrencyNotFoundException::new);
    }

    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation(value = "Set default currency code")
    public void setDefaultCurrencyCode(@ApiParam @RequestBody final String currencyCode) {
        currencyServiceProxy.findCurrencyByCode(currencyCode.toUpperCase())
                .map(currencyServiceProxy::setDefaultBroadleafCurrency)
                .orElseThrow(CurrencyNotFoundException::new);
    }
}
