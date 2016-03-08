package pl.touk.widerest.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.ApiTestUtils;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
public class CatalogTest extends ApiTestBase {


    /* (mst) Tests involving the entire catalog (eg: create a category -> create a product -> add it to the category ->
             add 2 additional SKUS -> ...
             go here
     */

    @Before
    public void initCatalogTests() {
        /* uncomment the following for "local" testing */
        //serverPort = String.valueOf(8080);

        cleanupCatalogTests();
    }


    /*
     *  1. Adds a new category
     *  2. Adds a new product to that category
     *  3. Validates that the default SKU, that comes with that product, contains proper values
     *  4. Adds a new product without category
     *  5. Removes both products and a category + validates everything is in the same state as at the beginning
     */
    @Test
    @Transactional
    public void exemplaryCatalogFlow1Test() {

        final long currentGlobalProductCount = apiTestCatalogLocal.getTotalProductsCount();
        final long currentGlobalCategoriesCount = apiTestCatalogLocal.getTotalCategoriesCount();

        // when: 1) adding a new test category
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        final ResponseEntity<?> remoteAddCategoryEntity = apiTestCatalogManager.addTestCategory(categoryDto);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(remoteAddCategoryEntity);

        // then: 1) the new category should not have any products
        final long currentProductsInCategoryRemoteCount = apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId);
        assertThat(currentProductsInCategoryRemoteCount, equalTo(0L));

        // when: 2) adding a new test product (with default SKU) into the category
        final ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());

        final ResponseEntity<?> remoteAddProduct1Entity = apiTestCatalogManager.addTestProduct(productDto);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testProductId1 = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        em.clear();

        // then: 2a) number of products in the test category should increase
        assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 1));


        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));

        final ResponseEntity<ProductDto> receivedProductEntity = hateoasRestTemplate().exchange(
                ApiTestUrls.PRODUCT_BY_ID_URL, HttpMethod.GET,
                testHttpRequestEntity.getTestHttpRequestEntity(), ProductDto.class, serverPort, testProductId1);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final Link defaultSkuLink = receivedProductEntity.getBody().getLink("default-sku");

        // then: 2b) product's default SKU should have proper values
        final ResponseEntity<SkuDto> receivedSkuEntity = restTemplate.exchange(
                defaultSkuLink.getHref(), HttpMethod.GET,
                testHttpRequestEntity.getTestHttpRequestEntity(), SkuDto.class, serverPort);

        assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final SkuDto receivedSkuDto = receivedSkuEntity.getBody();

        assertThat(receivedSkuDto.getName(), containsString(productDto.getName()));
        assertThat(receivedSkuDto.getQuantityAvailable(), equalTo(productDto.getQuantityAvailable()));
        assertThat(receivedSkuDto.getActiveStartDate(), equalTo(productDto.getValidFrom()));

        // when: 3) adding another product without category
        final ProductDto productDto2 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        final ResponseEntity<?> remoteAddProduct2Entity = apiTestCatalogManager.addTestProduct(productDto2);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testProductId2 = ApiTestUtils.getIdFromEntity(remoteAddProduct2Entity);

        // then: 3) total number of products should increase BUT the number of products in test category should remain unchanged
        assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 2));
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));

        // when: 4) removing both products from catalog
        oAuth2AdminRestTemplate().delete(ApiTestUrls.PRODUCTS_URL + "/" + testProductId1, serverPort);
        oAuth2AdminRestTemplate().delete(ApiTestUrls.PRODUCTS_URL + "/" + testProductId2, serverPort);

        em.clear();

        // then: 4) total number of products in catalog should decrease by 2 AND total number of products in the test
        //          category should drop down to 0 (no products in category)
        assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount));
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount));


        // when: 5) removing test category from catalog
        oAuth2AdminRestTemplate().delete(ApiTestUrls.CATEGORIES_URL + "/" + testCategoryId, serverPort);

        // then: 5) the total number of categories should decrease by 1
        em.clear();

        assertThat(apiTestCatalogLocal.getTotalCategoriesCount(), equalTo(currentGlobalCategoriesCount));
    }


    /* 1. Adds TEST_CATEGORIES_COUNT new categories
       2. Adds a new product and inserts it into all test categories
     * 3. Removes test categories from catalog and checks:
     *                 a) if the test product still exists
     *                 b) if the categories count referenced by test product decreases
     */
    @Test
    @Transactional
    public void removingCategoriesFromCatalogDoesNotRemoveProductThatIsInThemTest() {

        // when: 1) creating TEST_CATEGORIES_COUNT categories
        final long TEST_CATEGORIES_COUNT = 3;

        final long currentTotalCategoriesCount = apiTestCatalogLocal.getTotalCategoriesCount();

        final List<Long> newCategoriesIds = new ArrayList<>();

        ResponseEntity<?> remoteAddCategoryEntity;

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            remoteAddCategoryEntity = apiTestCatalogManager.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT));

            assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newCategoriesIds.add(ApiTestUtils.getIdFromEntity(remoteAddCategoryEntity));
        }

        // then: 1) total number of categories should increase by TEST_CATEGORIES_COUNT and all of the categories
        //          should not have any products in them
        assertThat(apiTestCatalogLocal.getTotalCategoriesCount(), equalTo(currentTotalCategoriesCount + TEST_CATEGORIES_COUNT));

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(newCategoriesIds.get(i)), equalTo(0L));
        }

        // when: 2) adding a new test product
        final long currentTotalProductsCount = apiTestCatalogLocal.getTotalProductsCount();

        final ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        final ResponseEntity<?> remoteAddProduct1Entity = apiTestCatalogManager.addTestProduct(productDto);

        // then: 2) total number of products should increase
        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentTotalProductsCount + 1));

        final long testProductId = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        // when: 3) inserting test product into all TEST_CATEGORIES_COUNT categories
        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            apiTestCatalogManager.addProductToCategoryReference(newCategoriesIds.get(i), testProductId);
        }

        // then: 3) product's categories number should be equal to TEST_CATEGORIES_COUNT (=> it has been inserted into all of them)
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT));

        ResponseEntity<ProductDto> receivedProductEntity;

        // when: 4) deleting each of TEST_CATEGORIES_COUNT categories
        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            oAuth2AdminRestTemplate().delete(ApiTestUrls.CATEGORY_BY_ID_URL, serverPort, newCategoriesIds.get(i));

            em.clear();

            // then: 4a) product's categories number should decrease (by 1) on each category deletion
            receivedProductEntity = restTemplate.exchange(
                    ApiTestUrls.PRODUCT_BY_ID_URL,
                    HttpMethod.GET, testHttpRequestEntity.getTestHttpRequestEntity(), ProductDto.class, serverPort, testProductId);

            assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));
            assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT - (i + 1)));
        }

        // then: 4b) product's categories number should equal to 0 after all categories have been removed
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(testProductId), equalTo(0L));
    }

    @Test
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deletingProductRemovesAllSkusAndCategoriesReferencesTest() {
        // when: 1) creating a new test category and adding a new test product to it
        final long currentGlobalProductCount = apiTestCatalogLocal.getTotalProductsCount();
        final long currentGlobalCategoryCount = apiTestCatalogLocal.getTotalCategoriesCount();

        //add test category
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        final ResponseEntity<?> remoteAddCategoryEntity = apiTestCatalogManager.addTestCategory(categoryDto);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(apiTestCatalogLocal.getTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(remoteAddCategoryEntity);

        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(0L));

        final ResponseEntity<?> remoteAddProduct1Entity = apiTestCatalogManager.addTestProduct(DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT));

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 1));

        final long testProductId = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(0L));

        apiTestCatalogManager.addProductToCategoryReference(testCategoryId, testProductId);

        // then: 1) total number of all products as well as products in test category should increase by 1
        try {
            apiTestCatalogManager.addProductToCategoryReference(testCategoryId, testProductId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            em.clear();
            assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
            assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        }

        // when: 2) creating TEST_SKUS_COUNT additional SKUs and adding them to test product
        final long TEST_SKUS_COUNT = 5;

        final long currentSkusForProductCount = apiTestCatalogLocal.getTotalSkusForProductCount(testProductId);

        for(int i = 0; i < TEST_SKUS_COUNT; i++) {
            oAuth2AdminRestTemplate().postForEntity(
                    ApiTestUrls.PRODUCT_BY_ID_SKUS,
                    DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT),
                    null,
                    serverPort,
                    testProductId);
        }

        em.clear();

        // then: 2) total number of SKUs for test product should increase by TEST_SKUS_COUNT
        assertThat(apiTestCatalogLocal.getTotalSkusForProductCount(testProductId), equalTo(currentSkusForProductCount + TEST_SKUS_COUNT));

        // when: 3) deleting test product
        oAuth2AdminRestTemplate().delete(ApiTestUrls.PRODUCT_BY_ID_URL, serverPort, testProductId);

        em.clear();

        // then: 3) total number of products should decrease by 1 and test category should not reference
        //          any products any longer
        assertThat(apiTestCatalogLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount));

        final ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(0L));
    }

    @Test
    @Transactional
    public void modifyingExistingCategoryDoesNotAffectItsProductsTest() {

        final int PRODUCT_COUNT = 4;

        // when: 1) adding a test category
        final CategoryDto testCategory = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        final long currentGlobalCategoryCount = apiTestCatalogLocal.getTotalCategoriesCount();

        final ResponseEntity<?> newCategoryEntity = apiTestCatalogManager.addTestCategory(testCategory);

        // then: 1) total number of categories should increase
        assertThat(newCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(apiTestCatalogLocal.getTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(newCategoryEntity);

        // when: 2) creating and inserting PRODUCT_COUNT products into test category
        ProductDto productDto;
        ResponseEntity<?> remoteAddProductEntity;

        for(int i = 0; i < PRODUCT_COUNT; i++) {
            productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
            productDto.setCategoryName(testCategory.getName());

            remoteAddProductEntity = apiTestCatalogManager.addTestProduct(productDto);

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        }

        // then: 2) total number of products in test category should be equal to PRODUCT_COUNT
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));

        // when: 3) modifying test category values
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);
        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(newCategoryEntity.getHeaders().getLocation().toString(), categoryDto, serverPort);

        // then: 3) test category does not "lose" its product references
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));
    }

    @Test
    @Transactional
    public void modifyingfExistingCategoryDoesNotBreakReferencesToAndFromProductsTest() {
        // when: 1) adding a new category with a new product
        final CategoryDto testCategory = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        final ResponseEntity<?> newCategoryEntity = apiTestCatalogManager.addTestCategory(testCategory);
        assertThat(newCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(newCategoryEntity);

        final ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        productDto.setCategoryName(testCategory.getName());

        final ResponseEntity<?> remoteAddProduct1Entity = apiTestCatalogManager.addTestProduct(productDto);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testProductId1 = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        // then: 1) Total number of products in test category equals 1
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(testProductId1), equalTo(1L));

        // when: 2) modifying test category and adding 3 attributes to it
        testCategory.setDescription("ModifiedTestCategoryDescription2");
        testCategory.setName("ModifiedTestCategoryName2");
        testCategory.setLongDescription("ModifiedTestCategoryLongDescription2");

        final Map<String, String> categoryAttributes = new HashMap<>();
        categoryAttributes.put("size", String.valueOf(99));
        categoryAttributes.put("color", "red");
        categoryAttributes.put("length", String.valueOf(12.222));
        testCategory.setAttributes(categoryAttributes);

        oAuth2AdminRestTemplate().put(ApiTestUrls.CATEGORY_BY_ID_URL, testCategory, serverPort, testCategoryId);

        // then: 2) modification does not change category's products
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(testProductId1), equalTo(1L));
    }


    @Test
    @Transactional
    public void creatingAndDeletingCategoriesReferencesDoesNotAffectActualEntitiesTest() {
        // when: 1) adding 2 test categories and 3 test products
        final ResponseEntity<?> newCategoryEntity1 = apiTestCatalogManager.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT));
        assertThat(newCategoryEntity1.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long testCategoryId1 = ApiTestUtils.getIdFromEntity(newCategoryEntity1);

        final ResponseEntity<?> newCategoryEntity2 = apiTestCatalogManager.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT));
        assertThat(newCategoryEntity2.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long testCategoryId2 = ApiTestUtils.getIdFromEntity(newCategoryEntity2);

        final List<Long> newProductsIds = new ArrayList<>();

        ResponseEntity<?> remoteAddProductEntity;

        for(int i = 0; i < 3; i++) {
            remoteAddProductEntity = apiTestCatalogManager.addTestProduct(DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT));

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newProductsIds.add(ApiTestUtils.getIdFromEntity(remoteAddProductEntity));
        }

        // then: 1) both categories do not "include" any of the test products yet
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(0L));
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(0L));

        // when: 2) adding the 1st product to the 1st category twice
        apiTestCatalogManager.addProductToCategoryReference(testCategoryId1, newProductsIds.get(0));

        em.clear();

        // then: 2) API should only add that product once
        try {
            apiTestCatalogManager.addProductToCategoryReference(testCategoryId1, newProductsIds.get(0));
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(1L));
        }

        // when: 3) adding 2nd product to the 1st category
        apiTestCatalogManager.addProductToCategoryReference(testCategoryId1, newProductsIds.get(1));

        em.clear();

        // then: 3) 1st category should now have 2 product references while both of those products should reference only 1 category
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(2L));

        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(1L));


        // when: 4) adding 3rd product to both categories
        apiTestCatalogManager.addProductToCategoryReference(testCategoryId1, newProductsIds.get(2));
        apiTestCatalogManager.addProductToCategoryReference(testCategoryId2, newProductsIds.get(2));

        em.clear();

        // then: 4)
        //         - 3rd product should now reference both categories
        //         - 1st category should reference all 3 products
        //         - 2nd category should reference only 1 product
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));

        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(3L));
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(1L));

        // when: 5) removing reference to 2nd product from 1st category twice
        apiTestCatalogManager.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(1));

        em.clear();

        // then: 5)
        //         - 1st category should reference 2 products
        //         - 1st product should reference 1 category
        //         - 2nd product should not reference any category
        //         - 3rd product should reference both categories
        //         - API should throw an error on removing non existent product reference from category
        try {
            apiTestCatalogManager.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(1));
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(2L));
        }

        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(0L));

        // when: 6a) removing reference to 1st product from 1st category
        apiTestCatalogManager.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(0));

        // when: 6b) adding 1st and 2nd product to 2nd category
        apiTestCatalogManager.addProductToCategoryReference(testCategoryId2, newProductsIds.get(0));
        apiTestCatalogManager.addProductToCategoryReference(testCategoryId2, newProductsIds.get(1));

        em.clear();

        // then: 6)
        //        - 1st category should reference only 1 product
        //        - 2nd category should reference all 3 products
        //        - 1st and 2nd products should reference only 1 category
        //        - 3rd product should reference both categories
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(3L));

        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(1L));
        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));

        // when: 7) removing reference to 3rd product from 1st category
        apiTestCatalogManager.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(2));

        em.clear();

        // then: 7)
        //         - 1st category should not have any product references
        //         - 2nd category should reference to all 3 products
        //         - 3rd product should only reference 1 category
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(0L));
        assertThat(apiTestCatalogLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(3L));

        assertThat(apiTestCatalogLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(1L));
    }


    /* ------------------ HELPER METHODS -------------------*/

    private void cleanupCatalogTests() {
        removeLocalTestProducts();
        removeLocalTestCategories();
    }

}