package pl.touk.widerest.api.categories;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.MediaDto;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("category")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Category", description = "Category DTO resource representation")
public class CategoryDto extends BaseDto {

    @NotBlank(message = "Category has to have a non empty name")
    @ApiModelProperty(position = 0, value = "Name of the category", required = true, dataType = "java.lang.String")
    private String name;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 1, value = "Short description of the category", required = false,
            dataType = "java.lang.String")
    private String description;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 2, value = "Long description of the category", required = false,
            dataType = "java.lang.String")
    private String longDescription;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 3, value = "Availability of all products in this category", required = false,
            dataType = "java.lang.String", allowableValues = "ALWAYS_AVAILABLE, UNAVAILABLE, CHECK_QUANTITY")
    private String productsAvailability;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 4, value = "Attributes associated with the category", required = false)
    private Map<String, String> attributes;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 5, value = "List of medias associated with a category")
    private Map<String, MediaDto> media;
}