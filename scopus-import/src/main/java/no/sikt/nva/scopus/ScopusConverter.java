package no.sikt.nva.scopus;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.sikt.nva.scopus.ScopusConstants.INF_END;
import static no.sikt.nva.scopus.ScopusConstants.INF_START;
import static no.sikt.nva.scopus.ScopusConstants.SUP_END;
import static no.sikt.nva.scopus.ScopusConstants.SUP_START;
import static nva.commons.core.StringUtils.isEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.util.RuntimeHttpUtils;
import jakarta.xml.bind.JAXBElement;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.scopus.generated.AbstractTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationLanguageTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOaLocationsType;
import no.scopus.generated.UpwOpenAccessType;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.ContributorExtractor;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.LanguageExtractor;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.sikt.nva.scopus.conversion.PublicationChannelConnection;
import no.sikt.nva.scopus.conversion.PublicationContextCreator;
import no.sikt.nva.scopus.conversion.PublicationInstanceCreator;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ScopusConverter {

    public static final URI HARDCODED_ID = URI.create(
        "https://api.sandbox.nva.aws.unit" + ".no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934");
    public static final ResourceOwner HARDCODED_RESOURCE_OWNER = new ResourceOwner(new Username("concurrencyT@unit.no"),
                                                                                   URI.create(
                                                                                       "https://www.example.org"));
    public static final String IMPORT_CANDIDATES_FILES_BUCKET = new Environment().readEnv(
        "IMPORT_CANDIDATES_FILES_BUCKET");
    public static final String RIGHTS_RESERVED_LICENSE = "http://rightsstatements.org/vocab/InC/1.0/";
    private static final String CONTENT_DISPOSITION_FILE_NAME_PATTERN = "filename=\"%s\"";
    private final DocTp docTp;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;
    private final PublicationChannelConnection publicationChannelConnection;
    private final S3Client s3Client;

    protected ScopusConverter(DocTp docTp, PiaConnection piaConnection, CristinConnection cristinConnection,
                              PublicationChannelConnection publicationChannelConnection, S3Client s3Client) {
        this.docTp = docTp;
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
        this.publicationChannelConnection = publicationChannelConnection;
        this.s3Client = s3Client;
    }

    public static String extractContentString(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        } else if (content instanceof JAXBElement) {
            return extractContentString(((JAXBElement<?>) content).getValue());
        } else if (content instanceof SupTp) {
            return extractContentString(((SupTp) content).getContent());
        } else if (content instanceof InfTp) {
            return extractContentString(((InfTp) content).getContent());
        } else {
            return ((ArrayList<?>) content).stream()
                       .map(ScopusConverter::extractContentString)
                       .collect(Collectors.joining());
        }
    }

    public static String extractContentAndPreserveXmlSupAndInfTags(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        } else if (content instanceof JAXBElement) {
            return extractContentAndPreserveXmlSupAndInfTags(((JAXBElement<?>) content).getValue());
        } else if (content instanceof SupTp) {
            return SUP_START + extractContentAndPreserveXmlSupAndInfTags(((SupTp) content).getContent()) + SUP_END;
        } else if (content instanceof InfTp) {
            return INF_START + extractContentAndPreserveXmlSupAndInfTags(((InfTp) content).getContent()) + INF_END;
        } else {
            return ((ArrayList<?>) content).stream()
                       .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
                       .collect(Collectors.joining());
        }
    }

    public ImportCandidate generateImportCandidate() {
        return new ImportCandidate.Builder().withPublisher(new Organization.Builder().withId(HARDCODED_ID).build())
                   .withResourceOwner(HARDCODED_RESOURCE_OWNER)
                   .withAdditionalIdentifiers(generateAdditionalIdentifiers())
                   .withEntityDescription(generateEntityDescription())
                   .withModifiedDate(Instant.now())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withAssociatedArtifacts(generateAssociatedArtifacts())
                   .build();
    }

    private static InputStream fetchFile(UpwOaLocationType type) {
        return attempt(() -> RuntimeHttpUtils.fetchFile(UriWrapper.fromUri(type.getUpwUrlForPdf()).getUri(),
                                                        new ClientConfiguration())).orElseThrow();
    }

    private List<AssociatedArtifact> generateAssociatedArtifacts() {
        return getLocations().stream().map(this::saveToS3).toList();
    }

    private List<UpwOaLocationType> getLocations() {
        return Optional.ofNullable(docTp.getMeta().getOpenAccess())
                   .map(OpenAccessType::getUpwOpenAccess)
                   .map(UpwOpenAccessType::getUpwOaLocations)
                   .map(UpwOaLocationsType::getUpwOaLocation)
                   .orElse(List.of());
    }

    private AssociatedArtifact saveToS3(UpwOaLocationType type) {
        var fileName = UriWrapper.fromUri(type.getUpwUrlForPdf()).getLastPathElement();
        var fileIdentifier = randomUUID();
        saveFile(type, fileName, fileIdentifier);
        var head = fetchFileInfo(fileIdentifier);
        return File.builder()
                   .withIdentifier(fileIdentifier)
                   .withName(fileName)
                   .withMimeType(head.contentType())
                   .withSize(head.contentLength())
                   .withLicense(URI.create(RIGHTS_RESERVED_LICENSE))
                   .buildPublishedFile();
    }

    private HeadObjectResponse fetchFileInfo(UUID fileIdentifier) {
        return s3Client.headObject(
            HeadObjectRequest.builder().bucket(IMPORT_CANDIDATES_FILES_BUCKET).key(fileIdentifier.toString()).build());
    }

    private void saveFile(UpwOaLocationType type, String fileName, UUID fileIdentifier) {
        s3Client.putObject(PutObjectRequest.builder()
                               .bucket(IMPORT_CANDIDATES_FILES_BUCKET)
                               .contentDisposition(String.format(CONTENT_DISPOSITION_FILE_NAME_PATTERN, fileName))
                               .key(fileIdentifier.toString())
                               .build(),
                           RequestBody.fromBytes(attempt(() -> fetchFile(type).readAllBytes()).orElseThrow()));
    }

    private Optional<AuthorKeywordsTp> extractAuthorKeyWords() {
        return Optional.ofNullable(extractHead()).map(HeadTp::getCitationInfo).map(CitationInfoTp::getAuthorKeywords);
    }

    private HeadTp extractHead() {
        return docTp.getItem().getItem().getBibrecord().getHead();
    }

    private EntityDescription generateEntityDescription() {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference());
        entityDescription.setMainTitle(extractMainTitle());
        entityDescription.setAbstract(extractMainAbstract());
        entityDescription.setContributors(
            new ContributorExtractor(extractCorrespondence(), extractAuthorGroup(), piaConnection,
                                     cristinConnection).generateContributors());
        entityDescription.setTags(generateTags());
        entityDescription.setPublicationDate(extractPublicationDate());
        entityDescription.setLanguage(new LanguageExtractor(extractCitationLanguages()).extractLanguage());
        return entityDescription;
    }

    private List<CitationLanguageTp> extractCitationLanguages() {
        return docTp.getItem().getItem().getBibrecord().getHead().getCitationInfo().getCitationLanguage();
    }

    private PublicationDate extractPublicationDate() {
        var publicationDate = getDateSortTp();
        return new PublicationDate.Builder().withDay(publicationDate.getDay())
                   .withMonth(publicationDate.getMonth())
                   .withYear(publicationDate.getYear())
                   .build();
    }

    /*
    According to the "SciVerse SCOPUS CUSTOM DATA DOCUMENTATION" dateSort contains the publication date if it exists,
     if not there are several rules to determine what's the second-best date is. See "SciVerse SCOPUS CUSTOM DATA
     DOCUMENTATION" for details.
     */
    private DateSortTp getDateSortTp() {
        return docTp.getItem().getItem().getProcessInfo().getDateSort();
    }

    private String extractMainAbstract() {
        return getMainAbstract().flatMap(this::extractAbstractStringOrReturnNull).map(this::trim).orElse(null);
    }

    private String trim(String string) {
        return Optional.ofNullable(string)
                   .map(s -> s.replaceAll("\\n\\r", StringUtils.SPACE))
                   .map(s -> s.replaceAll(StringUtils.DOUBLE_WHITESPACE, StringUtils.EMPTY_STRING))
                   .orElse(null);
    }

    private Optional<String> returnNullInsteadOfEmptyString(String input) {
        return isEmpty(input.trim()) ? Optional.empty() : Optional.of(input);
    }

    private Optional<String> extractAbstractStringOrReturnNull(AbstractTp abstractTp) {
        return returnNullInsteadOfEmptyString(extractAbstractString(abstractTp));
    }

    private String extractAbstractString(AbstractTp abstractTp) {
        return abstractTp.getPara()
                   .stream()
                   .map(para -> extractContentAndPreserveXmlSupAndInfTags(para.getContent()))
                   .collect(Collectors.joining());
    }

    private Optional<AbstractTp> getMainAbstract() {
        return nonNull(getAbstracts()) ? getAbstracts().stream().filter(this::isOriginalAbstract).findFirst()
                   : Optional.empty();
    }

    private List<AbstractTp> getAbstracts() {
        return nonNull(extractHead().getAbstracts()) ? extractHead().getAbstracts().getAbstract() : null;
    }

    private boolean isOriginalAbstract(AbstractTp abstractTp) {
        return YesnoAtt.Y.equals(abstractTp.getOriginal());
    }

    private List<String> generateTags() {
        return extractAuthorKeyWords().map(this::extractKeywordsAsStrings).orElse(emptyList());
    }

    private List<String> extractKeywordsAsStrings(AuthorKeywordsTp authorKeywordsTp) {
        return authorKeywordsTp.getAuthorKeyword()
                   .stream()
                   .map(this::extractConcatenatedKeywordString)
                   .collect(Collectors.toList());
    }

    private String extractConcatenatedKeywordString(AuthorKeywordTp keyword) {
        return keyword.getContent()
                   .stream()
                   .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
                   .collect(Collectors.joining());
    }

    private String extractMainTitle() {
        return getMainTitleTextTp().map(this::extractMainTitleContent).orElse(null);
    }

    private String extractMainTitleContent(TitletextTp titletextTp) {
        return extractContentAndPreserveXmlSupAndInfTags(titletextTp.getContent());
    }

    private Reference generateReference() {
        Reference reference = new Reference();
        reference.setPublicationContext(
            new PublicationContextCreator(docTp, publicationChannelConnection).getPublicationContext());
        reference.setPublicationInstance(
            new PublicationInstanceCreator(docTp, reference.getPublicationContext()).getPublicationInstance());
        reference.setDoi(extractDOI());
        return reference;
    }

    private URI extractDOI() {
        return nonNull(docTp.getMeta().getDoi()) ? UriWrapper.fromUri(DOI_OPEN_URL_FORMAT)
                                                       .addChild(docTp.getMeta().getDoi())
                                                       .getUri() : null;
    }

    private Optional<TitletextTp> getMainTitleTextTp() {
        return getTitleText().stream().filter(this::isTitleOriginal).findFirst();
    }

    private boolean isTitleOriginal(TitletextTp titletextTp) {
        return titletextTp.getOriginal().equals(YesnoAtt.Y);
    }

    private List<TitletextTp> getTitleText() {
        return extractHead().getCitationTitle().getTitletext();
    }

    private Set<AdditionalIdentifier> generateAdditionalIdentifiers() {
        return Set.of(extractScopusIdentifier());
    }

    private AdditionalIdentifier extractScopusIdentifier() {
        return new AdditionalIdentifier(ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, docTp.getMeta().getEid());
    }

    private List<AuthorGroupTp> extractAuthorGroup() {
        return extractHead().getAuthorGroup();
    }

    private List<CorrespondenceTp> extractCorrespondence() {
        return extractHead().getCorrespondence();
    }
}
