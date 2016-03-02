package pl.touk.widerest.api.cart.orders.converters;

import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentGroupImpl;
import org.broadleafcommerce.core.order.domain.FulfillmentGroupItem;
import org.broadleafcommerce.core.order.domain.FulfillmentGroupItemImpl;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.cart.customers.converters.AddressConverter;
import pl.touk.widerest.api.cart.orders.OrderController;
import pl.touk.widerest.api.cart.orders.dto.FulfillmentGroupDto;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class FulfillmentGroupConverter implements Converter<FulfillmentGroup, FulfillmentGroupDto> {

    @Resource
    private AddressConverter addressConverter;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Override
    public FulfillmentGroupDto createDto(final FulfillmentGroup fulfillmentGroup, final boolean embed) {

        final FulfillmentGroupDto fulfillmentGroupDto = new FulfillmentGroupDto();

        Optional.ofNullable(fulfillmentGroup.getAddress()).ifPresent(address -> {
            fulfillmentGroupDto.setAddress(addressConverter.createDto(address, false));
        });

        fulfillmentGroupDto.setItems(
                Optional.ofNullable(fulfillmentGroup.getFulfillmentGroupItems()).orElse(Collections.emptyList()).stream()
                        .map(fulfillmentGroupItem -> linkTo(methodOn(OrderController.class)
                                .getOneItemFromOrder(null, fulfillmentGroup.getOrder().getId(), fulfillmentGroupItem.getOrderItem().getId())).toUri().toASCIIString())
                        .collect(Collectors.toList())
        );

        fulfillmentGroupDto.setType(Optional.ofNullable(fulfillmentGroup.getType()).map(FulfillmentType::getFriendlyType).orElse(null));

        /* HATEOAS links */

        fulfillmentGroupDto.add(linkTo(methodOn(OrderController.class).getOrderFulfillmentById(null, fulfillmentGroup.getOrder().getId(), fulfillmentGroup.getId())).withSelfRel());

        return fulfillmentGroupDto;
    }

    @Override
    public FulfillmentGroup createEntity(final FulfillmentGroupDto fulfillmentGroupDto) {
        return updateEntity(new FulfillmentGroupImpl(), fulfillmentGroupDto);
    }

    @Override
    public FulfillmentGroup updateEntity(final FulfillmentGroup fulfillmentGroup, final FulfillmentGroupDto fulfillmentGroupDto) {

        fulfillmentGroup.setAddress(addressConverter.createEntity(fulfillmentGroupDto.getAddress()));

        return fulfillmentGroup;
    }

    @Override
    public FulfillmentGroup partialUpdateEntity(final FulfillmentGroup fulfillmentGroup, final FulfillmentGroupDto fulfillmentGroupDto) {
        throw new UnsupportedOperationException();
    }

}
