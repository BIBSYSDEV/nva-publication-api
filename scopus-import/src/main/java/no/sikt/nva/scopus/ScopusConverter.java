package no.sikt.nva.scopus;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.scopus.generated.CitationtypeAtt.ER;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.sikt.nva.scopus.ScopusConstants.INF_END;
import static no.sikt.nva.scopus.ScopusConstants.INF_START;
import static no.sikt.nva.scopus.ScopusConstants.SUP_END;
import static no.sikt.nva.scopus.ScopusConstants.SUP_START;
import static nva.commons.core.StringUtils.isEmpty;
import static nva.commons.core.attempt.Try.attempt;
import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.scopus.generated.AbstractTp;
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationLanguageTp;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.ContributorExtractor;
import no.sikt.nva.scopus.conversion.LanguageExtractor;
import no.sikt.nva.scopus.conversion.PublicationChannelConnection;
import no.sikt.nva.scopus.conversion.PublicationContextCreator;
import no.sikt.nva.scopus.conversion.PublicationInstanceCreator;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.exception.MissingNvaContributorException;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.CustomerList;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ScopusConverter {

    public static final String RESOURCE_OWNER_SIKT = "sikt@20754";
    public static final String CRISTIN_ID_SIKT = "20754.0.0.0";
    public static final String CRISTIN = "cristin";
    public static final String ORGANIZATION = "organization";
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String PROD = "prod";
    private static final Map<String, String> CUSTOMER_MAP = Map.of("sandbox", "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "dev", "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "test", "0baf8fcb-b18d-4c09-88bb-956b4f659103",
                                                                   "prod", "22139870-8d31-4df9-bc45-14eb68287c4a");
    public static final String CUSTOMER = "customer";
    public static final String MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE =
        "None of contributors belongs to NVA customer, all contributors affiliations: ";
    private final DocTp docTp;
    private final IdentityServiceClient identityServiceClient;
    private final PublicationChannelConnection publicationChannelConnection;
    private final ContributorExtractor contributorExtractor;
    private final ScopusFileConverter scopusFileConverter;

    public ScopusConverter(DocTp docTp,
                           PublicationChannelConnection publicationChannelConnection,
                           IdentityServiceClient identityServiceClient,
                           ScopusFileConverter scopusFileConverter, ContributorExtractor contributorExtractor1) {
        this.contributorExtractor = contributorExtractor1;
        this.docTp = docTp;
        this.publicationChannelConnection = publicationChannelConnection;
        this.identityServiceClient = identityServiceClient;
        this.scopusFileConverter = scopusFileConverter;
    }

    public static String extractContentString(Object content) {
        return switch (content) {
            case String string -> string.trim();
            case JAXBElement<?> jaxbElement -> extractContentString(jaxbElement.getValue());
            case SupTp supTp -> extractContentString(supTp.getContent());
            case InfTp infTp -> extractContentString(infTp.getContent());
            default -> convertFromArray((ArrayList<?>) content);
        };
    }

    public static String extractContentAndPreserveXmlSupAndInfTags(Object content) {
        return switch (content) {
            case String string -> string.trim();
            case JAXBElement<?> jaxbElement -> extractContentAndPreserveXmlSupAndInfTags(jaxbElement.getValue());
            case SupTp supTp -> SUP_START + extractContentAndPreserveXmlSupAndInfTags(supTp.getContent()) + SUP_END;
            case InfTp infTp -> INF_START + extractContentAndPreserveXmlSupAndInfTags(infTp.getContent()) + INF_END;
            default -> ((ArrayList<?>) content).stream()
                           .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
                           .collect(Collectors.joining());
        };
    }

    public ImportCandidate generateImportCandidate() {
        var contributorsWithCustomers = getContributors();
        return new ImportCandidate.Builder()
                   .withPublisher(createOrganization())
                   .withResourceOwner(constructResourceOwner())
                   .withAdditionalIdentifiers(generateAdditionalIdentifiers())
                   .withEntityDescription(generateEntityDescription(contributorsWithCustomers.contributors()))
                   .withCreatedDate(Instant.now())
                   .withModifiedDate(Instant.now())
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withAssociatedArtifacts(scopusFileConverter.fetchAssociatedArtifacts(docTp))
                   .withAssociatedCustomers(contributorsWithCustomers.associatedCustomerUris())
                   .build();
    }

    private static String convertFromArray(ArrayList<?> content) {
        return content.stream()
                   .map(ScopusConverter::extractContentString)
                   .collect(Collectors.joining());
    }

    private static URI constructOwnerAffiliation() {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addChild(CRISTIN_ID_SIKT)
                   .getUri();
    }

    private Organization createOrganization() {
        return new Organization.Builder()
                   .withId(UriWrapper.fromHost(API_HOST).addChild(CUSTOMER).addChild(getId()).getUri())
                   .build();
    }

    private static String getId() {
        return CUSTOMER_MAP.keySet()
                   .stream()
                   .filter(API_HOST::contains)
                   .findFirst()
                   .orElse(CUSTOMER_MAP.get(PROD));
    }

    private ResourceOwner constructResourceOwner() {
        return new ResourceOwner(new Username(RESOURCE_OWNER_SIKT), constructOwnerAffiliation());
    }

    private Optional<AuthorKeywordsTp> extractAuthorKeyWords() {
        return Optional.ofNullable(extractHead()).map(HeadTp::getCitationInfo).map(CitationInfoTp::getAuthorKeywords);
    }

    private HeadTp extractHead() {
        return docTp.getItem().getItem().getBibrecord().getHead();
    }

    private EntityDescription generateEntityDescription(List<Contributor> contributors) {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference());
        entityDescription.setMainTitle(extractMainTitle());
        entityDescription.setAbstract(extractMainAbstract());
        entityDescription.setContributors(contributors);
        entityDescription.setTags(generateTags());
        entityDescription.setPublicationDate(extractPublicationDate());
        entityDescription.setLanguage(new LanguageExtractor(extractCitationLanguages()).extractLanguage());
        return entityDescription;
    }

    private ContributorsWithCustomers getContributors() {
        var contributorsOrganizationsWrapper = contributorExtractor.generateContributors(docTp);
        var cristinTopLevelOrgs = contributorsOrganizationsWrapper.topLevelOrgs();
        var customerList = attempt(identityServiceClient::getAllCustomers).orElseThrow();

        var associatedCustomers = getAssociatedCustomers(customerList, cristinTopLevelOrgs);
        if (associatedCustomers.isEmpty()) {
            throw new MissingNvaContributorException(MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE + cristinTopLevelOrgs);
        }
        var customerUris = associatedCustomers.stream().map(CustomerDto::id).toList();
        return new ContributorsWithCustomers(contributorsOrganizationsWrapper.contributors(), customerUris);
    }

    private static Collection<CustomerDto> getAssociatedCustomers(CustomerList customerList,
                                                         Collection<URI> cristinTopLevelOrgs) {
        return customerList.customers().stream()
                   .filter(customer -> cristinTopLevelOrgs.contains(customer.cristinId()))
                   .toList();
    }

    private record ContributorsWithCustomers(List<Contributor> contributors, Collection<URI> associatedCustomerUris) {
    }

    private List<CitationLanguageTp> extractCitationLanguages() {
        return docTp.getItem().getItem().getBibrecord().getHead().getCitationInfo().getCitationLanguage();
    }

    private PublicationDate extractPublicationDate() {
        return getPublicationDateFromOaAccessEffectiveDate()
                   .orElseGet(this::getPublicationDateFromDateSort);
    }

    private PublicationDate getPublicationDateFromDateSort() {
        var dateSort = getDateSortTp();
        return new PublicationDate.Builder()
                   .withDay(dateSort.getDay())
                   .withMonth(dateSort.getMonth())
                   .withYear(dateSort.getYear())
                   .build();
    }

    private Optional<PublicationDate> getPublicationDateFromOaAccessEffectiveDate() {
        return Optional.of(docTp.getMeta())
                   .map(MetaTp::getOpenAccess)
                   .map(OpenAccessType::getOaAccessEffectiveDate)
                   .map(this::toPublicationDate);
    }

    private PublicationDate toPublicationDate(String value) {
        var localDate = attempt(() -> LocalDate.parse(value)).toOptional();
        return localDate.map(ScopusConverter::toPublicationDate).orElse(null);
    }

    private static PublicationDate toPublicationDate(LocalDate date) {
        return new PublicationDate.Builder()
                   .withYear(String.valueOf(date.getYear()))
                   .withMonth(String.valueOf(date.getMonthValue()))
                   .withDay(String.valueOf(date.getDayOfMonth()))
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
                   .map(s -> s.replace("\\n\\r", StringUtils.SPACE))
                   .map(s -> s.replace(StringUtils.DOUBLE_WHITESPACE, StringUtils.EMPTY_STRING))
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
        return getAbstracts().isEmpty()
                   ? Optional.empty()
                   : getAbstracts().stream().filter(this::isOriginalAbstract).findFirst();
    }

    private List<AbstractTp> getAbstracts() {
        return nonNull(extractHead().getAbstracts()) ? extractHead().getAbstracts().getAbstract() : emptyList();
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
                   .toList();
    }

    private String extractConcatenatedKeywordString(AuthorKeywordTp keyword) {
        return keyword.getContent()
                   .stream()
                   .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
                   .collect(Collectors.joining());
    }

    private String extractMainTitle() {
        return getOriginalMainTitleTextTp()
                   .or(this::getCitationTextTpWhenErCitationType)
                   .map(this::extractMainTitleContent)
                   .orElse(null);
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

    private Optional<TitletextTp> getOriginalMainTitleTextTp() {
        return Optional.ofNullable(extractHead())
                   .map(HeadTp::getCitationTitle)
                   .map(CitationTitleTp::getTitletext)
                   .flatMap(text -> text.stream().filter(this::isTitleOriginal).findFirst());
    }

    /**
     * Extracts the title for Errata/Corrigendum (Scopus: ER) type. Because this type is
     * a commentary on another document, the concept of "original title" becomes ambiguous,
     * so the ER type (mapped to JournalCorrigendum in NVA) does not have an "original
     * title" type in Scopus, but rather a "citation title" which we use as the main title.
     */
    private Optional<TitletextTp> getCitationTextTpWhenErCitationType() {
        var citationtypeAtt = getCitationtypeAtt();
        return citationtypeAtt.isPresent() && ER.equals(citationtypeAtt.get())
                   ? getCitationTitleTextTp()
                   : Optional.empty();
    }

    private Optional<TitletextTp> getCitationTitleTextTp() {
        return Optional.ofNullable(extractHead())
            .map(HeadTp::getCitationTitle)
            .map(CitationTitleTp::getTitletext)
            .flatMap(text -> text.stream().filter(this::isTitleNonOriginal).findFirst());
    }

    private Optional<CitationtypeAtt> getCitationtypeAtt() {
        return docTp.getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getCitationInfo()
                   .getCitationType()
                   .stream()
                   .findFirst()
                   .map(CitationTypeTp::getCode);
    }

    private boolean isTitleOriginal(TitletextTp titletextTp) {
        return nonNull(titletextTp) && titletextTp.getOriginal().equals(YesnoAtt.Y);
    }

    private boolean isTitleNonOriginal(TitletextTp titletextTp) {
        return nonNull(titletextTp) && titletextTp.getOriginal().equals(YesnoAtt.N);
    }

    private Set<AdditionalIdentifierBase> generateAdditionalIdentifiers() {
        return Set.of(extractScopusIdentifier());
    }

    private ScopusIdentifier extractScopusIdentifier() {
        return ScopusIdentifier.fromValue(docTp.getMeta().getEid());
    }
}
