package pl.touk.widerest.api.cart.dto;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
public class CartItemDto {
    @ApiModelProperty(required = true)
    private long itemId;
    @ApiModelProperty(required = true)
    private long quantity;
    @ApiModelProperty(required = true)
    private String productName;
    @ApiModelProperty(required = true)
    private ProductOptionDto options;
    @ApiModelProperty(required = false)
    private String msisdn;
    @ApiModelProperty(required = true)
    private BigDecimal price;

    private Map attributes;
    @ApiModelProperty(required = true)
    private long productId;
    @ApiModelProperty
    private long skuId;
    @ApiModelProperty
    private String description;



}
