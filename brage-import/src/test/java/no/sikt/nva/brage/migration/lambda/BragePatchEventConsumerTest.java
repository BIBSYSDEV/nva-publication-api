package no.sikt.nva.brage.migration.lambda;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BragePatchEventConsumerTest extends ResourcesLocalTest {

    public static final String TIME_STAMP = randomString();
    public static final Context CONTEXT = mock(Context.class);
    private ResourceService resourceService;
    private UriRetriever uriRetriever;
    private BragePatchEventConsumer handler;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        super.init();
        this.s3Client = new FakeS3Client();
        this.resourceService = getResourceServiceBuilder(client).build();
        this.uriRetriever = mock(UriRetriever.class);
        this.handler = new BragePatchEventConsumer(resourceService, s3Client, uriRetriever);
    }

    @Test
    void shouldUpdateChapterPartOfValueWhenSearchApiReturnsPublicationWithTheSameIsbn() throws NotFoundException {
        var isbn = randomIsbn13();
        var existingParentPublication = persistBookWithIsbn(isbn);
        var partOfReport = persistChildAndPartOfReportWithIsbn(isbn);
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 1);
        handler.handleRequest(event, CONTEXT);

        var updatedChild = resourceService.getPublication(partOfReport.getPublication());
        var partOfValue = getPartOfValue(updatedChild);

        assertEquals(SortableIdentifier.fromUri(partOfValue), existingParentPublication.getIdentifier());
    }

    @Test
    void shouldNotUpdateChaptersPartOfValueWhenFetchedPublicationDoesNotContainTheSameIsbnAsChapter() throws NotFoundException {
        var existingParentPublication = persistBookWithIsbn(randomIsbn13());
        var partOfReport = persistChildAndPartOfReportWithIsbn(randomIsbn13());
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 1);
        handler.handleRequest(event, CONTEXT);

        var notUpdatedChild = resourceService.getPublication(partOfReport.getPublication());

        assertNull(getPartOfValue(notUpdatedChild));
    }

    @Test
    void shouldNotUpdateChaptersPartOfValueWhenSearchApiReturnsMultiplePublications() throws NotFoundException {
        var existingParentPublication = persistBookWithIsbn(randomIsbn13());
        var partOfReport = persistChildAndPartOfReportWithIsbn(randomIsbn13());
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 2);
        handler.handleRequest(event, CONTEXT);

        var notUpdatedChild = resourceService.getPublication(partOfReport.getPublication());

        assertNull(getPartOfValue(notUpdatedChild));
    }

    @Test
    void shouldPersistErrorReportWhenCouldNotUpdatePartOfValue() {
        var existingParentPublication = persistBookWithIsbn(randomIsbn13());
        var partOfReport = persistChildAndPartOfReportWithIsbn(randomIsbn13());
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 2);
        handler.handleRequest(event, CONTEXT);

        var report = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"))
            .getFile(UnixPath.of("PART_OF_ERROR").addChild(partOfReport.getPublication().getIdentifier().toString()));

        assertTrue(report.contains("Multiple parents fetched for publication"));
    }

    @Test
    void some() throws JsonProcessingException {
        var s = """
            {
              "type": "PartOfReport",
              "publication": {
                "type": "Publication",
                "identifier": "018fe1ff12da-67f7c399-dd04-4fb7-b592-f1432e46bea9",
                "status": "PUBLISHED",
                "resourceOwner": {
                  "owner": "cicero@7475.0.0.0",
                  "ownerAffiliation": "https://api.dev.nva.aws.unit.no/cristin/organization/7475.0.0.0"
                },
                "publisher": {
                  "type": "Organization",
                  "id": "https://api.dev.nva.aws.unit.no/customer/d22e1273-5dd0-410f-a0d9-356353683a98"
                },
                "createdDate": "2024-06-04T06:45:40.934827370Z",
                "modifiedDate": "2024-06-04T06:45:40.934827370Z",
                "publishedDate": "2024-06-04T06:45:40.934827370Z",
                "entityDescription": {
                  "type": "EntityDescription",
                  "mainTitle": "Energy Politics and Gender",
                  "language": "http://lexvo.org/id/iso639-3/eng",
                  "publicationDate": {
                    "type": "PublicationDate",
                    "year": "2018"
                  },
                  "contributors": [
                    {
                      "type": "Contributor",
                      "identity": {
                        "type": "Identity",
                        "name": "Karina Standal"
                      },
                      "role": {
                        "type": "Creator"
                      },
                      "sequence": 1,
                      "correspondingAuthor": false
                    },
                    {
                      "type": "Contributor",
                      "identity": {
                        "type": "Identity",
                        "name": "Tanja Winther"
                      },
                      "role": {
                        "type": "Creator"
                      },
                      "sequence": 2,
                      "correspondingAuthor": false
                    },
                    {
                      "type": "Contributor",
                      "identity": {
                        "type": "Identity",
                        "name": "Katrine Danielsen"
                      },
                      "role": {
                        "type": "Creator"
                      },
                      "sequence": 3,
                      "correspondingAuthor": false
                    }
                  ],
                  "reference": {
                    "type": "Reference",
                    "publicationContext": {
                      "type": "Anthology"
                    },
                    "publicationInstance": {
                      "type": "NonFictionChapter",
                      "pages": {
                        "type": "Range"
                      }
                    }
                  },
                  "abstract": "Policy makers and scholars often assume gender to be irrelevant in energy politics. However, an increasing body of scholarship and development policies has focused on how gender discrimination has negative effects on women’s access to energy resources and equal contributions to decision-making processes that influence energy issues. This article evaluates four overarching and salient policy and research discourses that frame women’s and men’s positions in benefiting from and participating in decision-making about energy. First, energy has mainly been perceived as gender neutral, ignoring gendered outcomes of energy policies. Second, women have been presented as victims of energy poverty in the global South to instigate donors and action. Third, women’s empowerment in the global South has been presented as instrumental to increasing productivity and economic growth through access to modern sources and uses of energy. These discourses have produced narratives that provide limited imaginaries of women’s agency and relevance to the politics of energy in their lives. The fourth and less familiar discourse has presented women as rights holders of basic services, including access to modern and sustainable energy. This last discourse has provided a tool for examining the deeper unequal structures, as well as holding stakeholders in supply accountable for reproducing gender equality, needed to understand and produce relevant and socially just knowledge.\\nEnergy Politics and Gender"
                },
                "additionalIdentifiers": [
                  {
                    "type": "AdditionalIdentifier",
                    "sourceName": "handle",
                    "value": "https://hdl.handle.net/11250/2754881"
                  },
                  {
                    "type": "AdditionalIdentifier",
                    "sourceName": "Cristin",
                    "value": "1659627"
                  }
                ],
                "associatedArtifacts": [
                  {
                    "type": "PublishedFile",
                    "identifier": "fc971c09-51c5-4d51-aad4-c6fb499c1e52",
                    "name": "A+history+of+possible+futures+2018.pdf",
                    "mimeType": "application/pdf",
                    "size": 345323,
                    "license": "https://rightsstatements.org/page/inc/1.0",
                    "administrativeAgreement": false,
                    "publisherVersion": "PublishedVersion",
                    "rightsRetentionStrategy": {
                      "type": "NullRightsRetentionStrategy",
                      "configuredType": "Unknown"
                    },
                    "publishedDate": "2024-06-04T06:45:41.205230947Z",
                    "uploadDetails": {
                      "type": "UploadDetails",
                      "uploadedBy": "cicero@7475.0.0.0",
                      "uploadedDate": "2024-06-04T06:45:40.935054365Z"
                    },
                    "visibleForNonOwner": true
                  }
                ],
                "modelVersion": "0.21.23"
              },
              "record": {
                "customer": {
                  "name": "cicero",
                  "id": "https://api.dev.nva.aws.unit.no/customer/d22e1273-5dd0-410f-a0d9-356353683a98"
                },
                "resourceOwner": {
                  "owner": "cicero@7475.0.0.0",
                  "ownerAffiliation": "https://api.dev.nva.aws.unit.no/cristin/organization/7475.0.0.0"
                },
                "brageLocation": "95691/406",
                "id": "https://hdl.handle.net/11250/2754881",
                "cristinId": "1659627",
                "publishedDate": {
                  "brage": [
                    "2021-05-11T11:24:06Z"
                  ],
                  "nva": "2021-05-11T11:24:06Z"
                },
                "publisherAuthority": {
                  "brage": [
                    "publishedVersion"
                  ],
                  "nva": "PublishedVersion"
                },
                "type": {
                  "brage": [
                    "Chapter"
                  ],
                  "nva": "Chapter"
                },
                "partOf": "The Oxford Handbook of Energy Politics",
                "publication": {
                  "isbnList": [
                    "9780190861360"
                  ],
                  "publicationContext": {},
                  "partOfSeries": {}
                },
                "entityDescription": {
                  "abstracts": [
                    "Policy makers and scholars often assume gender to be irrelevant in energy politics. However, an increasing body of scholarship and development policies has focused on how gender discrimination has negative effects on women’s access to energy resources and equal contributions to decision-making processes that influence energy issues. This article evaluates four overarching and salient policy and research discourses that frame women’s and men’s positions in benefiting from and participating in decision-making about energy. First, energy has mainly been perceived as gender neutral, ignoring gendered outcomes of energy policies. Second, women have been presented as victims of energy poverty in the global South to instigate donors and action. Third, women’s empowerment in the global South has been presented as instrumental to increasing productivity and economic growth through access to modern sources and uses of energy. These discourses have produced narratives that provide limited imaginaries of women’s agency and relevance to the politics of energy in their lives. The fourth and less familiar discourse has presented women as rights holders of basic services, including access to modern and sustainable energy. This last discourse has provided a tool for examining the deeper unequal structures, as well as holding stakeholders in supply accountable for reproducing gender equality, needed to understand and produce relevant and socially just knowledge.",
                    "Energy Politics and Gender"
                  ],
                  "publicationDate": {
                    "brage": "2018",
                    "nva": {
                      "type": "PublicationDateNva",
                      "year": "2018"
                    }
                  },
                  "mainTitle": "Energy Politics and Gender",
                  "contributors": [
                    {
                      "type": "Contributor",
                      "brageRole": "author",
                      "role": "Creator",
                      "identity": {
                        "type": "Identity",
                        "name": "Karina Standal"
                      },
                      "sequence": 1
                    },
                    {
                      "type": "Contributor",
                      "brageRole": "author",
                      "role": "Creator",
                      "identity": {
                        "type": "Identity",
                        "name": "Tanja Winther"
                      },
                      "sequence": 2
                    },
                    {
                      "type": "Contributor",
                      "brageRole": "author",
                      "role": "Creator",
                      "identity": {
                        "type": "Identity",
                        "name": "Katrine Danielsen"
                      },
                      "sequence": 3
                    }
                  ],
                  "publicationInstance": {},
                  "language": {
                    "brage": [
                      "eng"
                    ],
                    "nva": "http://lexvo.org/id/iso639-3/eng"
                  }
                },
                "recordContent": {
                  "contentFiles": [
                    {
                      "bundleType": "ORIGINAL",
                      "description": "ORIGINAL",
                      "identifier": "fc971c09-51c5-4d51-aad4-c6fb499c1e52",
                      "license": {
                        "brageLicense": null,
                        "nvaLicense": {
                          "license": "https://rightsstatements.org/page/InC/1.0/",
                          "type": "License"
                        }
                      },
                      "filename": "A+history+of+possible+futures+2018.pdf"
                    }
                  ]
                }
              }
            }
            """;
        var report = JsonUtils.dtoObjectMapper.readValue(s, PartOfReport.class);
        var ss = "";
    }
    private void mockSearchApiResponse(Publication publication, int hits) {
        var publicationId = UriWrapper.fromUri(randomUri()).addChild(publication.getIdentifier().toString()).getUri();
        var searchApiResponse = new SearchResourceApiResponse(hits, List.of(new ResourceWithId(publicationId)));
        when(uriRetriever.getRawContent(any(), any()))
            .thenReturn(Optional.of(searchApiResponse.toJsonString()));
    }

    private static URI getPartOfValue(Publication updatedChild) {
        return ((Anthology) updatedChild.getEntityDescription().getReference().getPublicationContext()).getId();
    }

    private PartOfReport persistChildAndPartOfReportWithIsbn(String isbn) {
        var publication = randomPublication(NonFictionChapter.class);
        publication.getEntityDescription().getReference().setPublicationContext(new Anthology());
        var persistedPublication = resourceService.createPublicationFromImportedEntry(publication);
        var record = recordWithIsbn(isbn);
        var partOfReport = new PartOfReport(persistedPublication, record);
        partOfReport.persist(s3Client, TIME_STAMP);
        return partOfReport;
    }

    private static  Record recordWithIsbn(String isbn) {
        var record = new Record();
        record.setId(randomUri());
        record.setCustomer(new Customer(randomString(), randomUri()));
        var recordPublication = new no.sikt.nva.brage.migration.record.Publication();
        recordPublication.setIsbnList(List.of(isbn));
        record.setPublication(recordPublication);
        return record;
    }

    private Publication persistBookWithIsbn(String isbn) {
        var publication = randomPublication(BookAnthology.class);
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        var book = new Book(context.getSeries(), context.getSeriesNumber(), context.getPublisher(), List.of(isbn),
                            context.getRevision());
        publication.getEntityDescription().getReference().setPublicationContext(book);
        return resourceService.createPublicationFromImportedEntry(publication);
    }

    private SQSEvent createSqsEvent(URI location) {
        var eventReference = new EventReference(randomString(), randomString(), location, Instant.now());
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }
}