package pl.touk.widerest.api.orders;

import javaslang.control.Match;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.touk.widerest.api.common.AddressConverter;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.customers.CustomerNotFoundException;
import pl.touk.widerest.api.orders.fulfillments.FulfilmentServiceProxy;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;


@Service("wdOrderService")
public class OrderServiceProxy {

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blCustomerService")
    private CustomerService customerService;

    private FulfilmentServiceProxy fulfillmentServiceProxy;

    @Resource
    private AddressConverter addressConverter;

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;

    @PostAuthorize("permitAll")
    @Transactional
    public List<Order> getOrdersByCustomer(final UserDetails userDetails) throws CustomerNotFoundException {
        return Match.of(userDetails)
                .whenType(AdminUserDetails.class).then(this::getAllOrders)
                .whenType(CustomerUserDetails.class).then(() -> {
                    final Long id = ((CustomerUserDetails) userDetails).getId();
                    return Optional.ofNullable(customerService.readCustomerById(id))
                        .map(c -> orderService.findOrdersForCustomer(c))
                        .orElseThrow(() -> new CustomerNotFoundException(format("Cannot find customer with ID: %d",
                                id)));
        }).otherwise(Collections::emptyList)
                .get();
    }

    @PostAuthorize("hasRole('PERMISSION_ALL_ORDER')")
    @Transactional
    public List<Order> getAllOrders() {
        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        final CriteriaQuery criteria = builder.createQuery(Order.class);
        Root order = criteria.from(OrderImpl.class);
        criteria.select(order);
        TypedQuery query = this.em.createQuery(criteria);
        query.setHint("org.hibernate.cacheable", Boolean.valueOf(true));
        query.setHint("org.hibernate.cacheRegion", "query.Order");
        return query.getResultList();
    }

    @Transactional
    public Optional<Order> getProperCart(UserDetails userDetails, Long orderId) {

        return Match.of(userDetails)
                .whenType(CustomerUserDetails.class).then(d -> getOrderForCustomerById(d, orderId))
                .whenType(AdminUserDetails.class).then(() -> orderService.findOrderById(orderId))
                .toJavaOptional();
    }

    @Transactional
    public List<DiscreteOrderItem> getDiscreteOrderItemsFromProperCart(UserDetails userDetails, Long orderId) {
        return getProperCart(userDetails, orderId)
                .map(Order::getDiscreteOrderItems)
                .orElseThrow(ResourceNotFoundException::new);
    }

    public Order getOrderForCustomerById(CustomerUserDetails customerUserDetails, Long orderId) throws OrderNotFoundException {

        return Optional.ofNullable(getOrdersByCustomer(customerUserDetails))
                .orElseThrow(() -> new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId()))
                .stream()
                .filter(x -> x.getId().equals(orderId))
                .findAny()
                .orElseThrow(() -> new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId()));
    }

    @Transactional(rollbackFor = UpdateCartException.class)
    public void updateItemQuantityInOrder (
            Integer quantity, UserDetails userDetails, Long orderId, Long itemId)
                throws UpdateCartException, RemoveFromCartException {

//        if (quantity <= 0) {
//            return new ResponseEntity<>(HttpStatus.CONFLICT);
//        }

        final Order cart = getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new);

        if (cart.getDiscreteOrderItems().stream().filter(x -> Objects.equals(x.getId(), itemId)).count() != 1) {
            throw new ResourceNotFoundException("Cannot find an item with ID: " + itemId);
        }

        final OrderItemRequestDTO orderItemRequestDto = new OrderItemRequestDTO();
        orderItemRequestDto.setQuantity(quantity);
        orderItemRequestDto.setOrderItemId(itemId);

        orderService.updateItemQuantity(orderId, orderItemRequestDto, true);
    }

}
