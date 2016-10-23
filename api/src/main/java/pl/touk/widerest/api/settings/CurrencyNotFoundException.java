package pl.touk.widerest.api.settings;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such currency")
public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String message) {
        super(message);
    }
}
