package pl.touk.widerest.api.catalog.api.catalog.dto;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
public class BundleItemDto {

    private long quantity;

    private long productId;
}