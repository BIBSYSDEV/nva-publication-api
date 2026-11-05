# publication-commons

A collection of shared classes used in more-than-one module.

Includes persistence models for entities in publication-api (Resource, Ticket, IdEntry, ImportCandidate, LogEntry, Message).

## Notes

Because of its size, the Publication model is not included here, but in [publication-model](../publication-model/README.md).

## Scopus id mappings

`ScopusIdentifierMappingRepository` stores which Cristin person a Scopus author id (AUID) maps to, and which Cristin institution and unit a Scopus affiliation id (AFID) maps to.
It replaces the PIA lookups in the Scopus import ([NP-52064](https://sikt.atlassian.net/browse/NP-52064)).
The items live in the import candidates table (the `TABLE_NAME` of the import candidate functions), keyed on `PK0` and `SK0` next to the import candidates:

| `PK0`                       | `SK0`                            | Content                         |
| --------------------------- | -------------------------------- | ------------------------------- |
| `AUTHOR#SCOPUS#{auid}`      | `CURRENT`                        | The current author mapping      |
| `AUTHOR#SCOPUS#{auid}`      | `HISTORY#{updatedAt}#{revision}` | A copy of each written version  |
| `AFFILIATION#SCOPUS#{afid}` | `CURRENT`                        | The current affiliation mapping |
| `AFFILIATION#SCOPUS#{afid}` | `HISTORY#{updatedAt}#{revision}` | A copy of each written version  |

Attributes are plain top-level DynamoDB attributes, not a JSON blob.
Optional attributes are left out, never written as null.
Readers ignore attributes they don't know, so the migration script can add fields without breaking the Java side.
The key decides whether an item is an author or an affiliation mapping, and `type` must agree with it.
Items are read with the same object mapper as the other items in this module, so a value stored as another DynamoDB type is coerced when it can be: `needsReview` stored as the string `true` is read as true.
`revision` is the exception.
It must be stored as a whole number, because writes are conditioned on it.

`candidates` must be a list of maps whose values are strings, numbers or booleans.
The values are read as text, and a nested list or map in a candidate makes the whole `HISTORY` item malformed.

A malformed item (a missing or wrong `type`, a missing required attribute, a `revision` that isn't a whole number, an unknown `source`, an id that isn't a plain number, or a value that doesn't parse) is logged as an error and left out.
A malformed `CURRENT` item counts as no mapping, for reads and writes alike, and a malformed `HISTORY` item is left out of the history.
The other ids in the same read are not affected.
The next curator correction replaces a malformed `CURRENT` item, like a deleted one, and keeps its `scopusName` and `orcid`.
If its `revision` can't be read, that correction fails with an error that names the item, and the item has to be repaired by hand.

| Attribute              | Type   | On                  | Content                                                                                                                      |
| ---------------------- | ------ | ------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `type`                 | S      | all                 | `ScopusAuthorMapping` or `ScopusAffiliationMapping`                                                                          |
| `cristinPersonId`      | S      | authors             | Plain Cristin person id, such as `12345`, never a URI. A deleted mapping keeps its last person.                              |
| `cristinInstitutionId` | S      | affiliations        | Plain Cristin institution id, such as `194`                                                                                  |
| `cristinUnitId`        | S      | affiliations        | Always set: `{institution}.0.0.0` at the top level, otherwise the sub-unit. Starts with `cristinInstitutionId`.              |
| `scopusName`           | S      | optional            | The latest name seen in Scopus                                                                                               |
| `orcid`                | S      | optional, authors   | Plain ORCID id, such as `0000-0002-1825-0097`                                                                                |
| `mappingCount`         | N      | optional            | PIA rows that point to the chosen Cristin id. Only on the migrated version.                                                  |
| `totalMappingCount`    | N      | optional            | All PIA rows for the Scopus id. Only on the migrated version.                                                                |
| `needsReview`          | BOOL   | optional            | Only written when true                                                                                                       |
| `source`               | S      | all                 | `PIA_MIGRATION` or `CURATOR`                                                                                                 |
| `updatedAt`            | S      | all                 | ISO-8601 UTC instant with milliseconds, such as `2026-11-02T08:00:00.000Z`                                                   |
| `updatedBy`            | S      | all                 | The curator's username, or `pia-migration` for the migration                                                                 |
| `revision`             | N      | all                 | 1 for the first version, then one more per write                                                                             |
| `deleted`              | BOOL   | optional            | Only written when true. A deleted `CURRENT` counts as no mapping.                                                            |
| `scopusPublicationId`  | S      | optional, `HISTORY` | Written by curator corrections, together with `sequenceNumber`. Left out on the migrated version and on deletes (follow-up). |
| `sequenceNumber`       | N      | optional, `HISTORY` | The author's sequence number in that Scopus publication                                                                      |
| `candidates`           | L of M | optional, `HISTORY` | Every PIA candidate, only on the migrated version                                                                            |

Mappings are read in bulk with `BatchGetItem` on the `CURRENT` keys.
A curator correction writes a new `HISTORY` item and replaces `CURRENT` in one transaction, conditioned on the revision the curator read.
If the mapping changes in the meantime, the correction fails with a conflict.
Saving the person an author is already mapped to is a confirmation: it writes a new version like any other correction, which clears `needsReview`.

### The golden file is a contract

[`src/test/resources/scopus-mapping-items.json`](src/test/resources/scopus-mapping-items.json) holds example items in DynamoDB JSON, exactly as `BatchWriteItem` receives them.
`ScopusMappingGoldenFileTest` writes them to a local table and reads every one back through the repository.
The PIA migration script in nva-aws-cli-tools ([NP-52115](https://sikt.atlassian.net/browse/NP-52115)) tests against a copy of the same file.
That makes the file the contract between the two sides: any change to it is a contract change, and must be made in both repositories.
