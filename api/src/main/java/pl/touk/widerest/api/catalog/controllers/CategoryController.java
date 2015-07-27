package pl.touk.widerest.api.catalog.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/catalog/categories")
@Api(value = "categories", description = "Category catalog endpoint")
public class CategoryController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    /* GET /categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all categories",
            notes = "Gets a list of all available (non-archived) categories in the catalog",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of categories list", response = CategoryDto.class)
    })
    public List<CategoryDto> readAllCategories() {
        return catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .collect(Collectors.toList());
    }

    /* POST /categories */
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new category",
            notes = "Adds a new category to the catalog. It does take duplicates (same name and description) into account. " +
                    "Returns an URL to the newly added category in the Location field of the HTTP response header",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new category entry successfully created"),
            @ApiResponse(code = 409, message = "Category already exists")
    })
    public ResponseEntity<?> saveOneCategory(@RequestBody CategoryDto categoryDto) {

        long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
                .filter(x -> x.getDescription().equals(categoryDto.getDescription()))
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();

        if(duplicatesCount > 0) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        Category createdCategoryEntity = catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategoryEntity.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
    }

    /* GET /categories/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all categories",
            notes = "Gets a number of all, non-archived categories available in the catalog",
            response = Long.class
    )
    public Long getAllCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }


    /* GET /categories/{id} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single category details",
            notes = "Gets details of a single non-archived category specified by its ID",
            response = CategoryDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of category details", response = CategoryDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist or is marked as archived")
    })
    public CategoryDto readOneCategoryById(@PathVariable(value="categoryId") Long categoryId) {

        Category categoryEntity = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        if(((Status)categoryEntity).getArchived() == 'Y') {
            throw new ResourceNotFoundException("Cannot find category with ID: " + categoryId + ". Category marked as archived");
        }

        return DtoConverters.categoryEntityToDto.apply(categoryEntity);
    }

    /* DELETE /categories/id */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing category",
            notes = "Removes an existing category from catalog by marking it as archived",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful removal of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist or is already marked as archived")
    })
    public void removeOneCategoryById(@PathVariable(value="categoryId") Long categoryId) {

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> {
                    catalogService.removeCategory(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete category with ID: " + categoryId + ". Category does not exist"));
    }

    /* PUT /categories/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing category",
            notes = "Updates an existing category with new details",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void changeOneCategory(@PathVariable(value = "categoryId") Long categoryId, @RequestBody CategoryDto categoryDto) {

        Category categoryToUpdate = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                                    .filter(CatalogUtils::archivedCategoryFilter)
                                    .orElseThrow(() -> new ResourceNotFoundException("Cannot change category with ID " + categoryId + ". Category not found"));

        /* (mst) UGLY but temporal */
        if(categoryDto.getDescription() != null) {
            categoryToUpdate.setDescription(categoryDto.getDescription());
        }

        if(categoryDto.getName() != null) {
            categoryToUpdate.setName(categoryDto.getName());
        }

        if(categoryDto.getLongDescription() != null) {
            categoryToUpdate.setLongDescription(categoryDto.getLongDescription());
        }

        catalogService.saveCategory(categoryToUpdate);
    }


    /* GET /categories/{id}/products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get products in a category",
            notes = "Gets a list of all products belonging to a specified category",
            response = ProductDto.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all products in a given category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public List<ProductDto> readProductsFromCategory(@PathVariable(value="categoryId") Long categoryId) {

        return getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    /* POST /categories/{id}/products */
    /*
     * TODO: (mst) What if the product has defaultSKU set but no entry in allSkus list? (= copy?)
     */
    //@PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @PreAuthorize("permitAll")
    @Transactional
    @RequestMapping(value = "/{categoryId}/products", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a product to the category",
            notes = "Adds a product to the specified category and returns" +
                    " an URL to it in the Location field of the HTTP response header",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Specified product successfully added"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<?> saveOneProductInCategory(@PathVariable(value="categoryId") Long categoryId,
                                                      @RequestBody ProductDto productDto) {

        Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        Sku defaultSku = Optional.ofNullable(productDto.getDefaultSku())
                .map(DtoConverters.skuDtoToEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Default SKU for product not provided"));

         /* TODO: creating Product Bundles */
         
         /* what if both Product and SKU return null?! */
        //Product newProduct = catalogService.createProduct(ProductType.PRODUCT);


        Product newProduct = DtoConverters.productDtoToEntity.apply(productDto);
         /* this one is probably redundant */
        newProduct.setDefaultSku(defaultSku);
         /* Include information about Categories */
        newProduct.setCategory(category);

        newProduct = catalogService.saveProduct(newProduct);



        HttpHeaders responseHeaders = new HttpHeaders();

        /* TODO: (mst) Return url to the product itself? (/catalog/products/{productId}) */
        responseHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{categoryId}/products/{productId}")
                .buildAndExpand(categoryId, newProduct.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeaders, HttpStatus.CREATED);
    }


    /* GET /categories/{id}/products/{productId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products/{productId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get details of a product from a category",
            notes = "Gets details of a specific product in a given category",
            response = ProductDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ProductDto readOneProductFromCategory(@PathVariable(value="categoryId") Long categoryId,
                                                 @PathVariable(value = "productId") Long productId) {

        return this.getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getId() == productId)
                .findAny()
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find product with id: " + productId + " in category: " + categoryId));

    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO: test if this actually works
    /* PUT /categories/{id}/products/{productId} */
    //@PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products/{productId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update details of a single product in a category",
            notes = "Updates details of the specific product in a given category",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful update of product details"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void changeOneProductFromCategory(@PathVariable(value="categoryId") Long categoryId,
                                             @PathVariable(value = "productId") Long productId,
                                             @RequestBody ProductDto productDto) {

        this.getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getId() == productId)
                .findAny()
                .map(e -> {
                    /* TODO:  Check if products category and categoryId match */
                    productDto.setProductId(productId);
                    catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find product with id: " + categoryId + " in category: " + categoryId));

    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /* DELETE /categories/{id}/products/{productId} */
    @Transactional
    //@PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products/{productId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a product from a category",
            notes = "Removes a product from a specific category",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Specified product successfully removed from a category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void removeOneProductFromCategory(@PathVariable(value="categoryId") Long categoryId,
                                             @PathVariable(value = "productId") Long productId) {

        Product productToDelete = getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getId() == productId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist in category ID: " + categoryId));

    	/* (mst) do we need to manually delete all SKUS ? */
        catalogService.removeProduct(productToDelete);

    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all products in a specific category",
            notes = "Gets a number of all products belonging to a specified category",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products count"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public Long getAllProductsInCategoryCount(@PathVariable(value = "categoryId") Long categoryId) {

    	/*
    	Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .map(e -> {
                    if (!CatalogUtils.archivedCategoryFilter(e)) {
                        return null;
                    } else return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find category with id: " + categoryId));
                */
        //return category.getAllProductXrefs().stream().count();

        return getProductsFromCategoryId(categoryId).stream().count();

    }


    private List<Product> getProductsFromCategoryId(Long categoryId) throws ResourceNotFoundException {

        Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return category.getAllProductXrefs().stream().map(CategoryProductXref::getProduct).collect(Collectors.toList());
    }

}