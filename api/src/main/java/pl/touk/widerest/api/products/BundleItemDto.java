package pl.touk.widerest.api.products;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.ResourceSupport;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@ApiModel(value = "Bundle Item", description = "Bundle Item DTO resource representation")
public class BundleItemDto extends ResourceSupport {

    @ApiModelProperty(position = 0, value = "Quantity of this item", required = true, dataType = "java.lang.Integer")
    private Integer quantity;

    @ApiModelProperty(position = 1, value = "ID of the SKU", required = true, dataType = "java.lang.Long")
    private Long skuId;

    @ApiModelProperty(position = 2, value = "Sale price for this item when selling as part of a bundle", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal salePrice;
}
