package pl.touk.widerest.catalog;

import org.broadleafcommerce.core.catalog.domain.Sku;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.base.DtoTestType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class ProductControllerTest extends ApiTestBase {

    private HttpHeaders httpRequestHeader;
    private HttpEntity<String> httpRequestEntity;

    @Before
    public void initProductTests() {
        this.httpRequestHeader = new HttpHeaders();
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);
        //tmp
        serverPort = String.valueOf(8080);
        cleanupProductTests();
    }

    @Test
    public void addingNewProductIncreasesProductsCountAndSavedValuesAreValidTest() {

        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        //when
        ResponseEntity<?> remoteAddProductEntity = addNewTestProduct(productDto);

        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));

        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);

        ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(
                remoteAddProductEntity.getHeaders().getLocation().toString(),
                HttpMethod.GET, httpRequestEntity, ProductDto.class, serverPort);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        ProductDto receivedProductDto = receivedProductEntity.getBody();

        /* TODO: (mst) implement a reasonable ProductDto's equalTo() method instead of
         *       doing this bs:
         */
        //then
        assertThat(receivedProductDto.getName(), equalTo(productDto.getName()));
        assertThat(receivedProductDto.getDescription(), equalTo(productDto.getDescription()));
        assertThat(receivedProductDto.getModel(), equalTo(productDto.getModel()));
        assertThat(receivedProductDto.getDefaultSku().getSalePrice().longValue(), equalTo(productDto.getDefaultSku().getSalePrice().longValue()));
        assertThat(receivedProductDto.getDefaultSku().getQuantityAvailable(), equalTo(productDto.getDefaultSku().getQuantityAvailable()));
        /* ... */

    }

    /* Duplicate check */
    @Test
    public void addingDuplicateProductDoesNotIncreaseProductsCount() {
        long currentProductCount = getRemoteTotalProductsCount();

        ProductDto testProduct = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<?> retEntity = addNewTestProduct(testProduct);
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        try {
            retEntity = addNewTestProduct(testProduct);
            fail();
        } catch(HttpClientErrorException httpClientException) {
            assertThat(httpClientException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            assertThat(getRemoteTotalProductsCount(), equalTo(currentProductCount + 1));
        }

    }


    @Test
    public void addingNewProductWihoutDefaultSKUCausesExceptionTest() {
        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto productWihtoutDefaultSkuDto = DtoTestFactory.getTestProductWithoutDefaultSKU();

        try {
            //when
            ResponseEntity<?> retEntity = addNewTestProduct(productWihtoutDefaultSkuDto);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            //then
            assertThat(httpClientException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
            assertThat(currentProductsCount, equalTo(getRemoteTotalProductsCount()));
        }
    }

    @Test
    public void successfullyDeletingNewlyCreatedProductTest() {
        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto defaultProduct = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<?> retEntity = addNewTestProduct(defaultProduct);

        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));

        //when
        oAuth2AdminRestTemplate().delete(retEntity.getHeaders().getLocation().toString(), 1);

        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount));
    }

    /* -----------------------------SKUS TESTS----------------------------- */

    @Test
    public void addingNewSkuAfterCreatingProductWithDefaultSku() {
        ProductDto productWithDefaultSKU = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<?> addedProductEntity = addNewTestProduct(productWithDefaultSKU);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        String createdProductUrlString = addedProductEntity.getHeaders().getLocation().toString();
        long productId = getIdFromLocationUrl(createdProductUrlString);

        assertThat(getRemoteTotalSkusForProductCount(productId), equalTo(1L));


        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku();

        ResponseEntity<?> addedSkuEntity = addNewSKUToProduct(productId, additionalSkuDto);
        assertThat(getRemoteTotalSkusForProductCount(productId), equalTo(2L));
    }



    /* -----------------------------END OF TESTS----------------------------- */
    private void cleanupProductTests() {
        removeRemoteTestProducts();
    }

    private ResponseEntity<?> addNewTestProduct(ProductDto productDto) {
        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        return remoteAddProductEntity;
    }

    private ResponseEntity<?> addNewTestProduct() throws HttpClientErrorException {

        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.PRODUCTS_URL, productDto, null, serverPort);

        return remoteAddProductEntity;
    }

    private ResponseEntity<?> addNewSKUToProduct(long productId, SkuDto skuDto) {
        ResponseEntity<SkuDto> remoteAddSkuEntity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCT_BY_ID_SKUS, skuDto, null, serverPort, productId);

        return remoteAddSkuEntity;
    }

    private long getRemoteTotalProductsCount() {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private long getRemoteTotalSkusForProductCount(long productId) {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(SKUS_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private long getLocalTotalSkus() {
        return catalogService.findAllSkus().stream().count();
    }

    private Resource<ProductDto> getProductWithMultipleSkus() {


        ResponseEntity<Resource<ProductDto>[]> receivedProductsEntity =
                hateoasRestTemplate().exchange(PRODUCTS_URL,
                        HttpMethod.GET, httpRequestEntity,
                        new ParameterizedTypeReference<Resource<ProductDto>[]>() {},
                        serverPort);

        Resource<ProductDto> resultProduct = null;

        for(Resource<ProductDto> p : receivedProductsEntity.getBody()) {
            if(p.getContent().getSkus().stream().count() >= 2) {
                resultProduct = p;
                break;
            }
        }

        return resultProduct;
    }

    private void removeRemoteTestProducts() {

        ResponseEntity<ProductDto[]> receivedProductEntity = hateoasRestTemplate().exchange(PRODUCTS_URL,
                HttpMethod.GET, httpRequestEntity, ProductDto[].class, serverPort);

        assertNotNull(receivedProductEntity);
        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        for(ProductDto testProduct : receivedProductEntity.getBody()) {
            if(testProduct.getName().startsWith(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME)) {
                oAuth2AdminRestTemplate().delete(testProduct.getId().getHref(), 1);
            }
        }
    }


}
