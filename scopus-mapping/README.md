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
