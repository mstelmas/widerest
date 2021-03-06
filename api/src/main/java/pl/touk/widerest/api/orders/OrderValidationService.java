package pl.touk.widerest.api.orders;

import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.service.CountryService;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.common.AddressDto;
import pl.touk.widerest.api.orders.fulfillments.NoFulfillmentOptionException;
import pl.touk.widerest.api.orders.fulfillments.NoShippingAddressException;

import javax.annotation.Resource;
import java.util.Optional;

@Service("wdOrderValidationService")
public class OrderValidationService {

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    public void validateOrderBeforeCheckout(Order order) throws OrderValidationException {

        validateOrderNotEmpty(order);
        validateFulfillmentOption(order);
        validateFulfillmentAddresses(order);

    }


    private void validateOrderNotEmpty(Order order) throws OrderValidationException {
        if(order.getItemCount() <= 0) {
            throw new OrderValidationException("Order with ID: " + order.getId() + " is empty");
        }
    }


    private void validateCustomerDataInAddress(Address address) throws OrderValidationException {
        if(StringUtils.isEmpty(address.getFirstName()) || StringUtils.isEmpty(address.getLastName())) {
            throw new OrderValidationException("Provided address does not contain First and/or Last names");
        }
        if(StringUtils.isEmpty(address.getAddressLine1())){
            throw new OrderValidationException("Provided address does not contain address lines");
        }
        if(StringUtils.isEmpty(address.getPostalCode()) || StringUtils.isEmpty(address.getCity())) {
            throw new OrderValidationException("Provided address does not contain postal code and/or city");
        }
        if(address.getIsoCountryAlpha2() == null) {
            throw new OrderValidationException("Provided address does not contain valid country code");
        }
    }

    private void validateFulfillmentAddresses(Order order) {
        final Address address = Optional.ofNullable(fulfillmentGroupService.getFirstShippableFulfillmentGroup(order))
                .map(FulfillmentGroup::getAddress)
                .orElseThrow(() -> new NoShippingAddressException(
                        String.format("Shipping address for order with ID: %d has not been provided", order.getId())));

        validateCustomerDataInAddress(address);
    }


    private void validateFulfillmentOption(Order order) throws OrderValidationException {
        Optional.ofNullable(fulfillmentGroupService.getFirstShippableFulfillmentGroup(order))
                .map(FulfillmentGroup::getFulfillmentOption)
                .orElseThrow(() -> new NoFulfillmentOptionException("FulfillmentOption for order with ID: " + order
                        .getId() + " has not been provided"));
    }



}
