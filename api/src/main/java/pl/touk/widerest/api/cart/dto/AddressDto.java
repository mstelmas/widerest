package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;

/**
 * Created by mst on 08.07.15.
 */
@Data
@Builder
@ApiModel("Address")
public class AddressDto {

    @ApiModelProperty
    private String firstName;
    @ApiModelProperty
    private String lastName;
    @ApiModelProperty
    protected String city;
    @ApiModelProperty
    // TODO: Add validation
    protected String postalCode;
    @ApiModelProperty
    protected String addressLine1;
    @ApiModelProperty
    protected String addressLine2;
    @ApiModelProperty
    protected String addressLine3;
    @ApiModelProperty
    protected String companyName;
    @ApiModelProperty
    protected String addressName;
    @ApiModelProperty
    protected String county;

}