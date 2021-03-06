package pl.touk.widerest.api.common;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryAttribute;
import org.broadleafcommerce.core.catalog.domain.CategoryAttributeImpl;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CatalogUtils {

    public static <T> Predicate<T> isNotArchived() {
        return ((Predicate<T>) object -> !(object instanceof Status))
                .or(object -> ((Status) object).getArchived() != 'Y');
    }

    public static <T> Predicate<T> isUserAthorizedFor(String role) {
        return ignored -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getAuthorities)
                .map(Collection::stream)
                .flatMap(stream -> stream.map(GrantedAuthority::getAuthority).filter(role::equals).findAny())
                .isPresent();
    }

    public static Predicate<Product> shouldProductBeVisible =
        ((Predicate<Product>) Product::isActive).or(isNotArchived().and(isUserAthorizedFor("PERMISSION_READ_PRODUCT")));

    public static Predicate<Category> shouldCategoryBeVisible =
            ((Predicate<Category>) Category::isActive).or(isNotArchived().and(isUserAthorizedFor("PERMISSION_READ_CATEGORY")));

    public static Function<String, InventoryType> toInventoryTypeByAvailability = availability ->
         Optional.ofNullable(InventoryType.getInstance(availability)).orElse(InventoryType.ALWAYS_AVAILABLE);


    public static Function<Map.Entry<String, String>, CategoryAttribute> valueExtractor(Category categoryEntity) {
        return e -> {
            CategoryAttribute a = new CategoryAttributeImpl();
            a.setName(e.getKey());
            a.setValue(e.getValue());
            a.setCategory(categoryEntity);
            return a;
        };
    }

    public static long getIdFromUrl(final URI uri) throws NumberFormatException {
        List<String> pathSegments = UriComponentsBuilder.fromUri(uri).build().getPathSegments();
        return Long.parseLong(pathSegments.get(pathSegments.size() - 1));
    }

    public static long getIdFromUrl(final String categoryPathUrl) throws NumberFormatException {
        List<String> pathSegments = UriComponentsBuilder.fromUriString(categoryPathUrl).build().getPathSegments();
        return Long.parseLong(pathSegments.get(pathSegments.size() - 1));
//
//        final URL categoryPathURL = new URL(categoryPathUrl);
//
//        final String categoryPath = categoryPathURL.getPath();
//
//        final int lastSlashIndex = categoryPath.lastIndexOf('/');
//
//        if(lastSlashIndex < 0 || (lastSlashIndex + 1) >= categoryPath.length()) {
//            throw new DtoValidationException();
//        }
//
//        return Long.parseLong(categoryPath.substring(lastSlashIndex + 1));
    }

    /*
        (mst)

        Limits:
                0 : unlimited

     */
    public static <T> List<T> getSublistForOffset(final List<T> list, final int offset, final int limit) {

        final int listSize = list.size();

        if(listSize == 0) {
            return list;
        }

        /* (mst) Rather than just throwing an exception, we'll set some 'default' values instead */

//        if(offset < 0 || limit < 0) {
//            throw new IllegalArgumentException("Offset/Limit must be >= 0");
//        }

        int offsetParam = offset;
        int limitParam  = limit;

        if(offset < 0) {
            offsetParam = 0;
        }

        if(limit < 0) {
            limitParam = 0;
        }

        if(offsetParam > 0) {

            if(offsetParam >= listSize) {
                return list.subList(0, 0);
            }

            if(limitParam > 0) {
                return list.subList(offsetParam, Math.min(offsetParam + limitParam, listSize));
            } else {
                return list.subList(offsetParam, list.size());
            }
        } else if(limitParam > 0) {
            return list.subList(0, Math.min(limitParam, listSize));
        } else {
            return list.subList(0, listSize);
        }
    }

}