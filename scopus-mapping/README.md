# scopus-mapping

Maps Scopus identifiers to Cristin identifiers: a Scopus author (AUID) to a Cristin person, and a Scopus affiliation (AFID) to a Cristin institution and unit.
It replaces the PIA lookups in the Scopus import ([NP-52064](https://sikt.atlassian.net/browse/NP-52064)).

Two modules use it, and they must not depend on each other:

- `scopus-import` reads the mappings when a Scopus record is imported.
- `publication-rest` saves a curator's correction when an import candidate becomes a publication.

## Structure

The module is split in three layers under `no.unit.nva.publication.scopusmapping`:

| Package            | Contents                                                                   | May depend on                                                                  |
| ------------------ | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `domain`           | Identifiers, mappings and their rules, and the repository interface (port) | The JDK only                                                                   |
| `application`      | The use cases: look up mappings, correct an author mapping, read history   | `domain`                                                                       |
| `adapter.dynamodb` | The DynamoDB implementation of the repository, and the stored item format  | `domain`, the AWS SDK, Jackson, nva-commons, slf4j and `publication-constants` |

## Identifiers and ids

An identifier is the plain value, such as `194.0.0.0`.
An id is the full, resolvable URI, such as `https://api.nva.unit.no/cristin/organization/194.0.0.0`.
This module only handles and stores identifiers, because an id can be rebuilt from the identifier in each environment.

## Using the module

```java
var repository = new DynamoDbScopusMappingRepository(dynamoDbClient, tableName);
var service = new ScopusMappingService(repository, Clock.systemUTC());

var mappings = service.findCurrentMappings(scopusAuthorIdentifiers, scopusAffiliationIdentifiers);
var corrected =
    service.correctAuthorMapping(
        scopusAuthorIdentifier, cristinPersonIdentifier, correctionContext, username, expectedRevision);
```

- A deleted mapping counts as no mapping.
- A correction must name the revision the curator saw, or none when the curator saw no mapping.
  If the stored mapping has changed, the correction fails with `ScopusMappingConflictException`, and the caller should read the mapping again.
- A correction of an author whose stored mapping can't be read fails with `IllegalStateException`.
  See [How items are read](#how-items-are-read).
- Correcting to the person an author is already mapped to is a confirmation: it saves a new version like any other correction, which clears `needsReview`.

## Stored items

The mappings are items in the import candidates table (the `TABLE_NAME` of the import candidate functions), keyed on `PK0` and `SK0` next to the import candidates:

| `PK0`                       | `SK0`                            | Content                         |
| --------------------------- | -------------------------------- | ------------------------------- |
| `AUTHOR#SCOPUS#{auid}`      | `CURRENT`                        | The current author mapping      |
| `AUTHOR#SCOPUS#{auid}`      | `HISTORY#{updatedAt}#{revision}` | A copy of each written version  |
| `AFFILIATION#SCOPUS#{afid}` | `CURRENT`                        | The current affiliation mapping |
| `AFFILIATION#SCOPUS#{afid}` | `HISTORY#{updatedAt}#{revision}` | A copy of each written version  |

Attributes are plain top-level DynamoDB attributes, not a JSON blob.
Optional attributes are left out, never written as null.
The Scopus identifier is in the key, not in an attribute.
The key decides whether an item is an author or an affiliation mapping, and `type` must agree with it.

| Attribute                      | Type   | On                  | Content                                                                                                             |
| ------------------------------ | ------ | ------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `type`                         | S      | all                 | `ScopusAuthorMapping` or `ScopusAffiliationMapping`                                                                 |
| `cristinPersonIdentifier`      | S      | authors             | Such as `12345`. A deleted mapping keeps its last person.                                                           |
| `cristinInstitutionIdentifier` | S      | affiliations        | The institution as a top-level organization, such as `194.0.0.0`. Never the bare number PIA uses.                   |
| `cristinUnitIdentifier`        | S      | affiliations        | Always set: the sub-unit, such as `194.63.10.0`, or the institution itself for a mapping at institution level.      |
| `scopusName`                   | S      | optional            | The latest name seen in Scopus                                                                                      |
| `orcid`                        | S      | optional, authors   | Such as `0000-0002-1825-0097`                                                                                       |
| `mappingCount`                 | N      | optional            | PIA rows that point to the chosen Cristin identifier. Only on the migrated version.                                 |
| `totalMappingCount`            | N      | optional            | All PIA rows for the Scopus identifier. Only on the migrated version.                                               |
| `needsReview`                  | BOOL   | optional            | Only written when true                                                                                              |
| `source`                       | S      | all                 | `PIA_MIGRATION` or `CURATOR`                                                                                        |
| `updatedAt`                    | S      | all                 | ISO-8601 UTC instant with milliseconds, such as `2026-11-02T08:00:00.000Z`                                          |
| `updatedBy`                    | S      | all                 | The curator's username, or `pia-migration` for the migration                                                        |
| `revision`                     | N      | all                 | 1 for the first version, then one more per write                                                                    |
| `deleted`                      | BOOL   | optional            | Only written when true. A deleted `CURRENT` counts as no mapping.                                                   |
| `scopusPublicationIdentifier`  | S      | optional, `HISTORY` | Written by a curator's correction, together with `sequenceNumber`. Left out on the migrated version and on deletes. |
| `sequenceNumber`               | N      | optional, `HISTORY` | The author's sequence number in that Scopus publication                                                             |
| `candidates`                   | L of M | optional, `HISTORY` | Every candidate PIA had, only on the migrated version. See [Candidates](#candidates).                               |

### Candidates

Each candidate is a map with the Cristin identifiers of that candidate, in the same attribute names and formats as on the mapping itself, and two attributes of its own:

| Attribute          | Type | On      | Content                                                                |
| ------------------ | ---- | ------- | ---------------------------------------------------------------------- |
| `publicationCount` | N    | all     | PIA rows that point to the candidate                                   |
| `lastChanged`      | S    | authors | When PIA last changed the candidate, in the same format as `updatedAt` |

### How items are read

Unknown attributes are ignored, so the migration script can add attributes without breaking the Java side.
Items are read with the same object mapper as the other items in this repository, so a value stored as another DynamoDB type is coerced when it can be: `needsReview` stored as the string `true` is read as true.
`revision` is the exception.
It must be stored as a whole number, because writes are conditioned on it.
`candidates` must be a list of maps whose values are strings, numbers or booleans.
The values are read as text, and a nested list or map in a candidate makes the whole `HISTORY` item malformed.

An item is malformed when it has a missing or wrong `type`, a missing required attribute, a `revision` that isn't a whole number of at least 1, an unknown `source`, an identifier in the wrong format, an institution that isn't top-level, a unit of another institution, or a value that doesn't parse.
Lookups and history log a malformed item as an error and leave it out: a malformed `CURRENT` item counts as no mapping, and a malformed `HISTORY` item is left out of the history.
The other identifiers in the same lookup are not affected.
A correction does not replace a malformed `CURRENT` item.
It fails with an `IllegalStateException` that names the item, and nothing is saved until the item has been repaired by hand.

### The golden file is a contract

[`src/test/resources/scopus-mapping-items.json`](src/test/resources/scopus-mapping-items.json) holds example items in DynamoDB JSON, exactly as `BatchWriteItem` receives them.
`GoldenFileTest` and `GoldenFileHistoryTest` write them to a local table and read every one back through the use cases.
`GoldenFileTest` also checks that the items Java writes for a curator's correction are exactly the ones in the file.
The PIA migration script in nva-aws-cli-tools ([NP-52115](https://sikt.atlassian.net/browse/NP-52115)) tests against a copy of the same file.
That makes the file the contract between the two sides: any change to it is a contract change, and must be made in both repositories.
