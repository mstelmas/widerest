package pl.touk.widerest.api.categories;

import javaslang.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.media.domain.MediaImpl;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryAttribute;
import org.broadleafcommerce.core.catalog.domain.CategoryMediaXrefImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentOptionsMapConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfilmentServiceProxy;
import pl.touk.widerest.hal.EmbeddedResource;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static pl.touk.widerest.api.common.CatalogUtils.shouldCategoryBeVisible;
import static pl.touk.widerest.api.common.CatalogUtils.valueExtractor;

@Component
public class CategoryConverter implements Converter<Category, CategoryDto> {

    @Resource
    protected MediaConverter mediaConverter;

    @Resource
    protected CatalogService catalogService;

    @Resource
    protected FulfilmentServiceProxy fulfilmentServiceProxy;

    @Resource
    protected FulfillmentOptionsMapConverter fulfillmentOptionsMapConverter;

    @Override
    public CategoryDto createDto(final Category entity, final boolean embed, final boolean link) {

        final CategoryDto dto = CategoryDto.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .longDescription(entity.getLongDescription())
                .productsAvailability(
                        Optional.ofNullable(entity.getInventoryType())
                            .map(InventoryType::getType)
                            .orElse(null))
                .attributes(
                        Optional.ofNullable(entity.getCategoryAttributesMap())
                            .map(toCategoryAttributesMapDto)
                            .orElse(Collections.emptyMap())
                )
                .media(
                        Optional.ofNullable(entity.getCategoryMediaXref())
                            .orElse(Collections.emptyMap()).entrySet().stream()
                                .collect(toMap(Map.Entry::getKey, e -> mediaConverter.createDto(e.getValue().getMedia(), embed, link)))
                )
                .url(entity.getUrl())
                .build();

        dto.add(ControllerLinkBuilder.linkTo(methodOn(CategoryController.class).readOneCategoryById(null, entity.getId(), null, null)).withSelfRel());

        if (link) {

            dto.add(linkTo(methodOn(CategoryController.class).readProductsFromCategory(entity.getId(), null, null)).withRel("products"));

            final List<Link> subcategoriesLinks = Optional.ofNullable(entity.getAllChildCategoryXrefs())
                    .orElse(Collections.emptyList()).stream()
                    .map(CategoryXref::getSubCategory)
                    .map(x -> linkTo(methodOn(CategoryController.class).readOneCategoryById(null, x.getId(), null, null)).withRel("subcategories"))
                    .collect(toList());

            dto.add(subcategoriesLinks);

            final List<Link> parentCategoriesLinks = Optional.ofNullable(entity.getAllParentCategoryXrefs())
                    .orElse(Collections.emptyList()).stream()
                    .map(CategoryXref::getCategory)
                    .map(x -> linkTo(methodOn(CategoryController.class).readOneCategoryById(null, x.getId(), null, null)).withRel("parentcategories"))
                    .collect(toList());

            dto.add(parentCategoriesLinks);
        }

        if (embed) {
            final List<CategoryDto> subcategoryDtos = Optional.ofNullable(entity.getAllChildCategoryXrefs())
                    .orElse(Collections.emptyList()).stream()
                    .map(CategoryXref::getSubCategory)
                    .filter(shouldCategoryBeVisible)
                    .map(subcategory -> createDto(subcategory, true, link))
                    .collect(toList());

            if (!CollectionUtils.isEmpty(subcategoryDtos)) {
                dto.add(new EmbeddedResource("subcategories", subcategoryDtos));
            }

            dto.add(new EmbeddedResource(
                    "fulfillmentOptions",
                    Try.of(() -> fulfilmentServiceProxy.readFulfillmentOptionsWithPricesAvailableByFulfillmentType(entity.getFulfillmentType()))
                            .map(fulfillmentOptionsMapConverter::createDto)
                            .get()
            ));

        }

        return dto;
    }

    @Override
    public Category createEntity(final CategoryDto dto) {
        return updateEntity(catalogService.createCategory(), dto);
    }

    @Override
    public Category updateEntity(final Category categoryEntity, final CategoryDto categoryDto) {

        categoryEntity.setName(categoryDto.getName());
        categoryEntity.setDescription(categoryDto.getDescription());
        categoryEntity.setLongDescription(categoryDto.getLongDescription());
        categoryEntity.setActiveStartDate(
                Optional.ofNullable(categoryDto.getValidFrom())
                        .map(ZonedDateTime::toInstant)
                        .map(Date::from)
                        .orElse(Date.from(Instant.now()))
        );
        categoryEntity.setActiveEndDate(
                Optional.ofNullable(categoryDto.getValidTo())
                        .map(ZonedDateTime::toInstant)
                        .map(Date::from)
                        .orElse(null)
        );

        categoryEntity.setInventoryType(
                Optional.ofNullable(categoryDto.getProductsAvailability())
                        .map(CatalogUtils.toInventoryTypeByAvailability)
                        /* (mst) Remove this if you don't want to have a "default" availability set */
                        .orElse(InventoryType.ALWAYS_AVAILABLE)
        );


        categoryEntity.getCategoryAttributesMap().clear();

        Optional.ofNullable(categoryDto.getAttributes())
                .map(Map::entrySet).orElse(Collections.emptySet()).stream()
                .map(e -> Pair.of(e.getKey(), valueExtractor(categoryEntity).apply(e)))
                .forEach(o -> categoryEntity.getCategoryAttributesMap().put(o.getKey(), o.getValue()));

        categoryEntity.getCategoryMediaXref().clear();

        final Map<String, CategoryMediaXrefImpl> mediaXrefs = Optional.ofNullable(categoryDto.getMedia())
                .map(Map::entrySet).orElse(Collections.emptySet()).stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        mediaDtoEntry -> {
                            final CategoryMediaXrefImpl categoryMediaXref = new CategoryMediaXrefImpl();
                            categoryMediaXref.setCategory(categoryEntity);
                            categoryMediaXref.setKey(mediaDtoEntry.getKey());

                            final Media categoryMedia = new MediaImpl();
                            mediaConverter.updateEntity(categoryMedia, mediaDtoEntry.getValue());

                            categoryMediaXref.setMedia(categoryMedia);

                            return categoryMediaXref;
                        }
                ));

        categoryEntity.getCategoryMediaXref().putAll(mediaXrefs);

        categoryEntity.setUrl(categoryDto.getUrl());

        return categoryEntity;
    }

    private Function<Map<String, CategoryAttribute>, Map<String, String>> toCategoryAttributesMapDto = categoryAttributesMap ->
            categoryAttributesMap.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().toString()));

}
