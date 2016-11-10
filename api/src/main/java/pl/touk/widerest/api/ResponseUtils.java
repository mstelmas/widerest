package pl.touk.widerest.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public class ResponseUtils {
    public static class Statuses {
        public static final ResponseEntity<Void> NO_CONTENT = ResponseEntity.noContent().build();
        public static final ResponseEntity<Void> OK = ResponseEntity.ok().build();
        public static final ResponseEntity<Void> CONFLICT = ResponseEntity.status(HttpStatus.CONFLICT).build();
        public static final ResponseEntity<Void> CREATED = ResponseEntity.status(HttpStatus.CREATED).build();
        public static final ResponseEntity<Void> UNPROCESSABLE_ENTITY = ResponseEntity.unprocessableEntity().build();
        public static final ResponseEntity<Void> NOT_FOUND = ResponseEntity.notFound().build();

        public static <T> ResponseEntity<T> OK(final T body) {
            return ResponseEntity.ok(body);
        }

        public static ResponseEntity<Void> CREATED(final URI uri) {
            return ResponseEntity.created(uri).build();
        }
    }
}
