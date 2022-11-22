package no.sikt.nva.brage.migration.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.sikt.nva.brage.migration.model.Language;
import no.sikt.nva.brage.migration.model.Record;
import no.sikt.nva.brage.migration.model.content.ContentFile;
import no.sikt.nva.brage.migration.model.content.ResourceContent;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;

public class NvaBrageMigrationDataGenerator {

    private final Record brageRecord;
    private final Publication correspondingNvaPublication;

    private NvaBrageMigrationDataGenerator(Builder builder) {
        brageRecord = createBrageRecord(builder);
        correspondingNvaPublication = createCorrespondingNvaPublication(builder);
    }

    public Record getBrageRecord() {
        return brageRecord;
    }

    public Publication getCorrespondingNvaPublication() {
        return correspondingNvaPublication;
    }

    private Publication createCorrespondingNvaPublication(Builder builder) {
        var publication = new Publication.Builder()
                              .withDoi(builder.getDoi())
                              .withHandle(builder.getHandle())
                              .withEntityDescription(createEntityDescription(builder))
                              .withResourceOwner(createResourceOwner(builder))
                              .build();
        return publication;
    }

    private EntityDescription createEntityDescription(Builder builder) {
        return new EntityDescription.Builder()
                   .withLanguage(builder.getLanguage().getNva())
                   .build();
    }

    private ResourceOwner createResourceOwner(Builder builder) {
        return new ResourceOwner(null, builder.getCustomerUri());
    }

    private Record createBrageRecord(Builder builder) {
        var record = new Record();
        record.setCustomerId(builder.getCustomerUri());
        record.setDoi(builder.getDoi());
        record.setId(builder.getHandle());
        record.setContentBundle(new ResourceContent(builder.getContentFiles()));
        record.setLanguage(builder.getLanguage());
        return record;
    }

    public static class Builder {

        private URI handle;

        private URI doi;
        private List<ContentFile> contentFiles;

        private URI customerUri;

        private Language language;

        public Language getLanguage() {
            return language;
        }

        public URI getHandle() {
            return handle;
        }

        public URI getDoi() {
            return doi;
        }

        public List<ContentFile> getContentFiles() {
            return contentFiles;
        }

        public URI getCustomerUri() {
            return customerUri;
        }

        public Builder withHandle(URI handle) {
            this.handle = handle;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withContentFiles(List<ContentFile> contentFiles) {
            this.contentFiles = contentFiles;
            return this;
        }

        public NvaBrageMigrationDataGenerator build() {
            if (Objects.isNull(handle)) {
                handle = randomHandle();
            }
            if (Objects.isNull(doi) && randomBoolean()) {
                doi = randomDoi();
            }
            if (Objects.isNull(customerUri)) {
                customerUri = randomUri();
            }
            if (Objects.isNull(language)) {
                language = randomLanguage();
            }
            return new NvaBrageMigrationDataGenerator(this);
        }

        private Language randomLanguage() {
            var someWeirdNess = randomInteger(3);
            switch (someWeirdNess) {
                case 0:
                    return new Language(List.of("nob"),
                                        UriWrapper.fromUri(LanguageMapper.LEXVO_URI_PREFIX + "nob").getUri());
                case 1:
                    return new Language(null, UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri());
                default:
                    return new Language(List.of("norsk", "svensk"),
                                        UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri());
            }
        }

        private URI randomHandle() {
            return UriWrapper.fromUri("http://hdl.handle.net/11250/" + randomInteger()).getUri();
        }
    }
}
