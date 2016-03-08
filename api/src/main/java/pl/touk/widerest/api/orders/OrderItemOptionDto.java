package pl.touk.widerest.api.orders;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Order Item Option", description = "Order Item Option DTO resource description")
public class OrderItemOptionDto {

    @ApiModelProperty(position = 0, value = "Name of the selected option", required = true, dataType = "java.lang.String")
    private String optionName;

    @ApiModelProperty(position = 1, value = "Value of the selected option", required = true, dataType = "java.lang.String")
    private String optionValue;
}