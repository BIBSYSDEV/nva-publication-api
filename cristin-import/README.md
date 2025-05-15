# cristin-import

Imports data (metadata only) from Cristin CRIS (Current Research Information System).

The initialization handler in this module is triggered by a "button" (event producing handler); a patch handler is provided to update existing resources. Additionally, a rerun handler is provided to rerun failed updates.

## Data processing

The module processes files that contain a meta-format in JSON that is pre-processed (simplified), homogenized data from Cristin, and imports these as Publications in NVA. As a side effect, the handlers produce import/update reports.

Various integrity checks and deduplication occur in this process.

## Notes

1. This module will be removed once all relevant institutional archives have been imported.
2. The origin data is produced by Cristin. 

# How to run cristin import

See Jira Confluence: [Kjøring av cristin-import lambdaer](https://unit.atlassian.net/l/cp/J51wfD3G)