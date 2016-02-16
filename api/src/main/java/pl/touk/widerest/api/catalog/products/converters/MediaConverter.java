package pl.touk.widerest.api.catalog.products.converters;

import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.media.domain.MediaImpl;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.catalog.dto.MediaDto;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Component
public class MediaConverter implements Converter<Media, MediaDto> {
    @Override
    public MediaDto createDto(final Media media, boolean embed) {
        return MediaDto.builder()
                .title(media.getTitle())
                .url(media.getUrl())
                .altText(media.getAltText())
                .tags(media.getTags())
                .build();
    }

    @Override
    public Media createEntity(final MediaDto mediaDto) {
        final Media media = new MediaImpl();
        return updateEntity(media, mediaDto);
    }

    @Override
    public Media updateEntity(final Media media, final MediaDto mediaDto) {
        media.setTitle(mediaDto.getTitle());
        media.setTags(mediaDto.getTags());
        media.setAltText(mediaDto.getAltText());
        media.setUrl(mediaDto.getUrl());

        return media;
    }

    @Override
    public Media partialUpdateEntity(final Media media, final MediaDto mediaDto) {
        if(mediaDto.getTitle() != null) {
            media.setTitle(mediaDto.getTitle());
        }

        if(mediaDto.getTags() != null) {
            media.setTags(mediaDto.getTags());
        }

        if(mediaDto.getAltText() != null) {
            media.setAltText(mediaDto.getAltText());
        }

        if(mediaDto.getUrl() != null) {
            media.setUrl(mediaDto.getUrl());
        }

        return media;
    }
}