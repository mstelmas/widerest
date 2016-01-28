package pl.touk.widerest.api.cart.controllers;


import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javaslang.control.Match;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.service.CustomerServiceProxy;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;
import pl.touk.widerest.security.config.ResourceServerConfig;
import pl.touk.widerest.security.oauth2.Scope;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.jasongoodwin.monads.Try.ofFailable;
import static java.lang.Long.parseLong;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static pl.touk.widerest.api.DtoConverters.customerEntityToDto;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/customers")
@Api(value = "customers", description = "Customer management endpoint")
public class CustomerController {

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name = "wdCustomerService")
    private CustomerServiceProxy customerServiceProxy;

    @Resource
    private AuthorizationCodeServices authorizationCodeServices;

    @Resource
    private AnonymousUserDetailsService customerUserDetailsService;

    @Resource
    private AuthorizationServerEndpointsConfiguration authorizationServerEndpointsConfiguration;

    @Autowired
    ResourceServerTokenServices tokenServices;

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #customerId == 'me' or #customerId == #customerUserDetails.id")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single customer details", response = CustomerDto.class)
    public ResponseEntity<CustomerDto> readOneCustomer(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId
    ) {
        return ofNullable(customerId)
                .map(toCustomerId(customerUserDetails, customerId))
                .map(customerService::readCustomerById)
                .map(customerEntityToDto)
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #customerId  == 'me' or #customerId == #customerUserDetails.id")
    @RequestMapping(value = "/{id}/email", method = RequestMethod.PUT)
    @ApiOperation(value = "Update customer's email")
    public void updateCustomerEmail(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId,
            @RequestBody String email
    ) {
        Optional.ofNullable(customerId)
                .map(toCustomerId(customerUserDetails, customerId))
                .map(customerService::readCustomerById)
                .map(toCustomerWithEmail(email))
                .orElseThrow(CustomerNotFoundException::new);
    }

    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #customerId  == 'me' or #customerId == #customerUserDetails.id")
    @RequestMapping(value = "/{id}/authorization", method = RequestMethod.POST)
    @ApiOperation(value = "Update customer's email", response = String.class)
    public ResponseEntity createAuthorizationCode(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId
    ) {
        return ofNullable(customerId)
                .map(toCustomerId(customerUserDetails, customerId))
                .map(customerService::readCustomerById)
                .map(this::generateCode)
                .map(ResponseEntity::ok)
                .orElseThrow(CustomerNotFoundException::new);
    }



    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all customers",
            notes = "Gets a list of all currently active customers",
            response = CustomerDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of customers list", response = CustomerDto.class, responseContainer = "List")
    })
    public List<CustomerDto> readAllCustomers(@ApiIgnore @AuthenticationPrincipal UserDetails userDetails) {
        return Match.of(userDetails)
                .whenType(AdminUserDetails.class).then(() -> customerServiceProxy.getAllCustomers().stream()
                        .map(customerEntityToDto)
                        .collect(Collectors.toList()))
                .whenType(CustomerUserDetails.class).then(() -> Optional.ofNullable(customerServiceProxy.getCustomerById(((CustomerUserDetails) userDetails).getId()))
                            .map(id -> customerEntityToDto.apply(id))
                            .map(Collections::singletonList)
                            .orElse(emptyList()))
                .otherwise(Collections::emptyList)
                .get();
    }

    @Transactional
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public void registerCustomer(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            @RequestParam String email
    )  {
        // Assuming user already has token

        ofNullable(customerUserDetails.getId())
                .map(customerService::readCustomerById)
                .filter(c -> !c.isRegistered())
                .orElseThrow(() -> new ResourceNotFoundException("User already registered"));

        Optional.of(username)
                .filter(customerName -> isNull(customerService.readCustomerByUsername(customerName)))
                .orElseThrow(() -> new ResourceNotFoundException("Username already taken, please try with other"));

        Optional.of(email)
                .filter(e -> isNull(customerService.readCustomerByEmail(e)))
                .orElseThrow(() -> new ResourceNotFoundException("Email address already taken, please try with other"));

        Customer customer = customerService.readCustomerById(customerUserDetails.getId());
        customer.setUsername(username);
        customer.setEmailAddress(email);

        customerService.registerCustomer(customer, password, passwordConfirm);

    }

    @Transactional
    @RequestMapping(value = "/merge", method = RequestMethod.POST)
    public void mergeWithAnonymous(@ApiIgnore @AuthenticationPrincipal CustomerUserDetails userDetails,
                                   @RequestBody final String anonymousToken) {

        final Customer loggedUser = customerService.readCustomerById(userDetails.getId());
        final CustomerUserDetails anonymousUserDetails = (CustomerUserDetails) tokenServices.loadAuthentication
                (anonymousToken).getPrincipal();

        final Customer anonymousUser = customerService.readCustomerById(anonymousUserDetails.getId());
    }


    private static Function<String, Long> toCustomerId(CustomerUserDetails customerUserDetails, String customerId) {
        return id -> Optional.ofNullable(customerUserDetails)
                .filter(ud -> "me".equals(customerId))
                .map(CustomerUserDetails::getId)
                .orElse(ofFailable(() -> parseLong(customerId)).orElse(null));
    }

    private static UnaryOperator<Customer> toCustomerWithEmail(final String email) {
        return customer -> {
            customer.setEmailAddress(email);
            return customer;
        };
    }

    private String generateCode(Customer customer) throws AuthenticationException {

        final String clientId = ((OAuth2Authentication) getContext().getAuthentication())
                .getOAuth2Request().getClientId();

        final OAuth2RequestFactory oAuth2RequestFactory =
                authorizationServerEndpointsConfiguration
                        .getEndpointsConfigurer()
                        .getOAuth2RequestFactory();

        final OAuth2Request storedOAuth2Request = oAuth2RequestFactory.createOAuth2Request(
                oAuth2RequestFactory.createAuthorizationRequest(
                        ImmutableMap.<String, String>builder()
                                .put(OAuth2Utils.SCOPE, Scope.CUSTOMER.toString())
                                .put(OAuth2Utils.CLIENT_ID, clientId)
                                .build()
                )
        );

        final UserDetails customerUserDetails = customerUserDetailsService.createCustomerUserDetails(customer);
        final OAuth2Authentication combinedAuth = new OAuth2Authentication(storedOAuth2Request, new
                SiteAuthenticationToken(customerUserDetails, null, customerUserDetails.getAuthorities()
        ));

        return authorizationCodeServices.createAuthorizationCode(combinedAuth);
    }
}
