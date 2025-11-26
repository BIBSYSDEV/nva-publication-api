package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.sikt.nva.scopus.conversion.model.AuthorIdentifiers;
import no.sikt.nva.scopus.conversion.model.CristinPersonContainer;
import no.unit.nva.publication.external.services.cristin.CristinConnection;
import no.unit.nva.publication.external.services.cristin.CristinPerson;
import no.sikt.nva.scopus.paralleliseutils.ParallelizeListProcessing;

public class CristinPersonRetriever {

    private final CristinConnection cristinConnection;
    private final PiaConnection piaConnection;

    public CristinPersonRetriever(CristinConnection cristinConnection, PiaConnection piaConnection) {
        this.cristinConnection = cristinConnection;
        this.piaConnection = piaConnection;
    }

    public Map<AuthorIdentifiers, CristinPerson> retrieveCristinPersons(List<AuthorGroupTp> authorGroupTps) {
        var scopusAuthors = getUniqueAuthors(authorGroupTps);
        var scopusAuthorsMappedToCristinIdentifiers = getCristinIdentifiers(scopusAuthors);
        var cristinPersonRepresentationContainingData = getCristinPersons(scopusAuthorsMappedToCristinIdentifiers);
        return onlyCristinPersons(cristinPersonRepresentationContainingData);
    }

    private static List<AuthorIdentifiers> getUniqueAuthors(List<AuthorGroupTp> authorGroupTps) {
        return authorGroupTps.stream()
                   .map(AuthorGroupTp::getAuthorOrCollaboration)
                   .flatMap(List::stream)
                   .filter(authorOrCollaboration -> authorOrCollaboration instanceof AuthorTp)
                   .map(AuthorTp.class::cast)
                   .map(authorTp -> new AuthorIdentifiers(authorTp.getAuid(), authorTp.getOrcid()))
                   .distinct()
                   .toList();
    }

    private Map<AuthorIdentifiers, CristinPerson> onlyCristinPersons(
        List<CristinPersonContainer> cristinPersonRepresentationContainingData) {
        return cristinPersonRepresentationContainingData.stream()
                   .filter(cristinPersonContainer -> nonNull(cristinPersonContainer.cristinPerson()))
                   .collect(Collectors.toMap(CristinPersonContainer::authorIdentifiers,
                                             CristinPersonContainer::cristinPerson));
    }

    private List<CristinPersonContainer> getCristinPersons(
        List<CristinPersonContainer> scopusAuthorsMappedToCristinIdentifiers) {
        return ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(scopusAuthorsMappedToCristinIdentifiers,
                                                                              this::getCristinPersonData);
    }

    private List<CristinPersonContainer> getCristinIdentifiers(List<AuthorIdentifiers> scopusAuthors) {
        return ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(scopusAuthors, this::retrieveCristinUri);
    }

    private CristinPersonContainer getCristinPersonData(CristinPersonContainer cristinPersonContainer) {
        var cristinPerson = cristinConnection.getCristinPersonByCristinId(cristinPersonContainer.cristinId());
        if (cristinPerson.isEmpty() && nonNull(cristinPersonContainer.authorIdentifiers().orcid())) {
            cristinPerson = cristinConnection.getCristinPersonByOrcId(
                cristinPersonContainer.authorIdentifiers().orcid());
        }
        return new CristinPersonContainer(cristinPersonContainer.authorIdentifiers(),
                                          cristinPersonContainer.cristinId(), cristinPerson.orElse(null));
    }

    private CristinPersonContainer retrieveCristinUri(AuthorIdentifiers authorIdentifiers) {
        var cristinId =
            Optional.ofNullable(authorIdentifiers.scopusAuid())
                .flatMap(piaConnection::getCristinPersonIdentifier)
                .orElse(null);
        return new CristinPersonContainer(authorIdentifiers, cristinId, null);
    }
}
