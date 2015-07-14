package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.common.money.Money;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    @ApiModelProperty
    private long itemId;

    @ApiModelProperty(required = true)
    private Integer quantity;

    @ApiModelProperty
    private String productName;

    @ApiModelProperty
    private ProductOptionDto options;

    @ApiModelProperty
    private BigDecimal price;

    @ApiModelProperty
    private Map attributes;

    @ApiModelProperty
    private long productId;

    @ApiModelProperty(required = true)
    private long skuId;

    @ApiModelProperty
    private String description;

    @ApiModelProperty
    protected Money salePrice;
    @ApiModelProperty
    protected Money retailPrice;

    // TODO: Bundles and all that other bs

}
