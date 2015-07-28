package pl.touk.widerest.paypal.endpoint;

import com.paypal.api.payments.PaymentExecution;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.*;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalPaymentGatewayType;
import pl.touk.widerest.paypal.gateway.PayPalRequestDto;
import pl.touk.widerest.paypal.gateway.PayPalResponseDto;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Controller
@ResponseBody
@RequestMapping("/orders/{id}/paypal")
public class PayPalController {

    @Resource(name = "blOrderToPaymentRequestDTOService")
    private OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blOrderService")
    private OrderService orderService;

    private PaymentGatewayConfigurationService configurationService;
    private PaymentGatewayHostedService hostedService;
    private PaymentGatewayWebResponseService webResponseService;
    private PaymentGatewayTransactionConfirmationService transactionConfirmationService;

    @PostConstruct
    public void afterPropertiesSet() {
        configurationService = paymentGatewayConfigurationServiceProvider.getGatewayConfigurationService(PayPalPaymentGatewayType.PAYPAL);
        hostedService = configurationService.getHostedService();
        webResponseService = configurationService.getWebResponseService();
        transactionConfirmationService = configurationService.getTransactionConfirmationService();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity initiate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) throws PaymentException {

        if(userDetails instanceof AdminUserDetails) {
            throw new PaymentException("Admins can't make orders");
        }
        CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;

        // find order
        Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException(""));

        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        // TODO: odhardcodowac adres
        String SELF_URL = "http://localhost:8080/orders/"+orderId+"/paypal";

        String returnUrl = SELF_URL+"/return";
        String cancelUrl = SELF_URL+"/cancel";

        PaymentRequestDTO paymentRequest =
                orderToPaymentRequestDTOService.translateOrder(order)
                        .additionalField(PayPalMessageConstants.RETURN_URL, returnUrl)
                        .additionalField(PayPalMessageConstants.CANCEL_URL, cancelUrl)
                        .orderDescription("TODO");
        paymentRequest = populateLineItemsAndSubscriptions(order, paymentRequest);

        PaymentResponseDTO paymentResponse = hostedService.requestHostedEndpoint(paymentRequest);

        //return redirect URI from the paymentResponse

        String redirectURI = Optional.ofNullable(paymentResponse.getResponseMap().get(PayPalMessageConstants.REDIRECT_URL))
                .orElseThrow(() -> new ResourceNotFoundException(""));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(redirectURI)
                        .build().toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.I_AM_A_TEAPOT);
    }

    @RequestMapping(value = "/return", method = RequestMethod.GET)
    public ResponseEntity handleReturn(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) throws PaymentException {


        // call checkout workflow
        //PaymentResponseDTO response = webResponseService.translateWebResponse(request);
        // handle no funds failures

        if(userDetails instanceof AdminUserDetails) {
            throw new PaymentException("Admins can't make orders");
        }
        CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;

        Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException(""));
        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        // get data from link
        PaymentResponseDTO payPalResponse = webResponseService.translateWebResponse(request);

        // create request
        PayPalRequestDto requestDTO = new PayPalRequestDto();
        requestDTO.setOrderId(payPalResponse.getOrderId());
        requestDTO.setPayerId(payPalResponse.getResponseMap().get(PayPalMessageConstants.PAYER_ID));
        requestDTO.setPaymentId(payPalResponse.getResponseMap().get(PayPalMessageConstants.PAYMENT_ID));
        requestDTO.setAccessToken(payPalResponse.getResponseMap().get(PayPalMessageConstants.ACCESS_TOKEN));
        requestDTO.getWrapped().transactionTotal(order.getTotal().toString());
        requestDTO.getWrapped().orderCurrencyCode(order.getCurrency().getCurrencyCode());
        //TODO: czy to jest potrzebne by pamietac? (PaymentTransactionType)
        //requestDTO.setPaymentTransactionType(payPalResponse.getPaymentTransactionType());

        // execute payment, check if the client has money
        payPalResponse = transactionConfirmationService.confirmTransaction(requestDTO.getWrapped());

        payPalResponse.getResponseMap().entrySet().stream()
                .forEach(System.out::println);


        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public ResponseEntity handleCancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        return ResponseEntity.notFound().build();

    }

    private PaymentRequestDTO populateLineItemsAndSubscriptions(Order order, PaymentRequestDTO paymentRequest) {
        for (OrderItem item : order.getOrderItems()) {
            String name;
            if (item instanceof BundleOrderItem) {
                name = ((BundleOrderItem) item).getSku().getDescription();
            } else if (item instanceof DiscreteOrderItem) {
                name = ((DiscreteOrderItem) item).getSku().getDescription();
            } else {
                name = item.getName();
            }
            String category = item.getCategory() == null ? null : item.getCategory().getName();
            paymentRequest = paymentRequest
                    .lineItem()
                    .name(name)
                    .amount(String.valueOf(item.getAveragePrice())) // TODO: blad przyblizen przy mnozeniu
                    .category(category)
                    .quantity(String.valueOf(item.getQuantity()))
                    .done();
        }
        return paymentRequest;
    }



}