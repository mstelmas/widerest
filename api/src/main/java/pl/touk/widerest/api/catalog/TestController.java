package pl.touk.widerest.api.catalog;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.security.config.ResourceServerConfig;

@RestController
@RequestMapping(ResourceServerConfig.API_PATH + "/test")
@Api
public class TestController {

    @RequestMapping("/none")
    public String none() {
        return "FAIL";
    }

    @RequestMapping("/all")
    @PreAuthorize("permitAll")
    public String all() {
        return "OK";
    }

    @RequestMapping("/anonymous")
    @PreAuthorize("isAnonymous()")
    public String anonymous() {
        return "OK";
    }

    @ApiOperation(
            value = "a registered user operation"
    )
    @RequestMapping("/registered")
    @PreAuthorize("isFullyAuthenticated()")
    public String registered() {
        return "OK";
    }

    @RequestMapping("/admin")
    @PreAuthorize("isFullyAuthenticated()")
    public String admin() {
        return "OK";
    }

}