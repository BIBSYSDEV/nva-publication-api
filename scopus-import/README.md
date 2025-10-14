# scopus-import

A service for importing and processing publication data from Scopus into the NVA (Norwegian Authority for Research and Higher Education) system.

## Overview

This service imports scholarly publication data from Scopus and transforms it into NVA ImportCandidates. Data is downloaded weekly by [dlr-nva-email-service](https://github.com/BIBSYSDEV/dlr-nva-email-service) to S3, then processed and persisted to the database with associated files.

## Contributor Generation

The `ContributorExtractor` class converts Scopus authors and collaborations into NVA contributors, enriching the data with verified identities and affiliations through integration with:

- **PIA API**
- **Cristin API**

### Important Considerations

Authors in Scopus data may have complex relationships:

- ✓ Same author appears in multiple author groups
- ✓ Same author appears in both author groups and correspondence
- ✓ Same author appears as both individual author and collaboration
- ✓ Same author has multiple affiliations
- ✓ Same author has multiple identifiers (Scopus AUID, ORCID)

## Process Flow

### 1. Organization Enrichment

The `AffiliationGenerator` resolves Scopus affiliations to Cristin organizations through a fallback chain:

1. **PIA Service Lookup** (primary)
   - Maps Scopus affiliation ID (AFID) to Cristin organization
   - Returns verified Norwegian institutions

2. **Cristin Search by Organization Id** (fallback)
    - Fetches organization from Cristin API by id fetched from PIA
    - Used when PIA responded with organization
   
3. **Cristin Search by Organization Name** (fallback)
   - Extracts organization name from Scopus XML
   - Searches in Cristin API for matching institution
   - Used when PIA did not respond with organization

4. **Cristin Search by Country** (fallback)
   - Uses affiliation country from Scopus data
   - Finds country-level organization in Cristin
   - Used when PIA did not respond with organization

5. **No Affiliation** (final fallback)
   - If all searches fail, contributor will have empty affiliations
   - Used when PIA did not respond with organization and all searches failed

### 2. Author Enrichment

The `CristinPersonRetriever` matches Scopus authors to Norwegian researchers through multiple strategies:

1. **By Scopus AUID via PIA** (primary)
   - PIA service maps Scopus author IDs to Cristin person IDs

2. **By Cristin id via Cristin** (fallback)
    - Direct Cristin API lookup using contributor Cristin identifier
    - Used when PIA responded with contributor identifier
   
3. **By ORCID via Cristin** (fallback)
   - Direct Cristin API lookup using ORCID
   - Used when PIA service responded with contributor with ORCID and look up by Cristin id failed

4. **Extract Correspondence Person**
   - Identifies corresponding author from document metadata
   - Used to mark corresponding author status

### 3. Contributor Generation

Process each author group with its associated Cristin organizations, persons, and correspondence data:

#### Author Type Handling

**Individual Authors (AuthorTp)**

Two creation paths based on Cristin match:

1. **Cristin Person Available**
   - Uses verified identity from Cristin person profile
   - Includes active affiliations from Cristin
   - Sets verification status (VERIFIED/NOT_VERIFIED)
   - Preserves Scopus AUID as additional identifier
   - Uses ORCID from Scopus if missing in Cristin profile

2. **Scopus-Only (no Cristin match)**
   - Creates identity from Scopus data
   - Name from preferred name or given/surname
   - ORCID normalized to full URI
   - Scopus AUID as additional identifier
   - Affiliations from organization enrichment results
   - Role: CREATOR

**Group Authors (CollaborationTp)**
- Name from indexed name
- Affiliations from organization enrichment
- Role: OTHER

#### Deduplication Strategy

After all contributors are created, duplicates are identified and merged:

**Deduplication Key (priority order):**
1. Scopus AUID (primary identifier)
2. ORCID (secondary identifier)

**Merging Rules:**
- **Without Cristin ID**: Merge affiliations from duplicate entries
  - Same author appearing in multiple author groups with different affiliations
  - Combines all affiliation data while preserving other fields

- **With Cristin ID**: Keep only first occurrence (no merging)
  - Prevents overwriting verified Cristin affiliation data
  - Cristin person affiliations are authoritative for Norwegian researchers


### Error Handling

The system gracefully handles service failures:
- PIA connection errors are logged but don't fail the import
- Cristin API failures fall back to Scopus-only contributor creation
- Organization search failures result in empty affiliations
- All contributors are preserved regardless of enrichment success
