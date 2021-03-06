package pl.touk.widerest.api.products;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import pl.touk.widerest.api.BaseDto;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("attribute")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Product Attribute", description = "Product Attribute DTO resource representation")
public class ProductAttributeDto extends BaseDto {

    @NotBlank(message = "Product attribute has to have a non empty name")
    @ApiModelProperty(position = 0, value = "Name of the attribute", required = true, dataType = "java.lang.String")
    private String attributeName;

    @NotNull(message = "Product attribute has to have a value provided")
    @ApiModelProperty(position = 1, value = "Value of the attribute", required = true, dataType = "java.lang.String")
    private String attributeValue;
}
