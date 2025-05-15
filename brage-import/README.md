# brage-import

Imports data (files and metadata) from Brage (DSpace) institutional archives.

The initialization handler in this module is triggered by a new file event (e.g. S3 event); a patch handler is provided to update existing resources. Additionally, a rollback handler is provided to undo patching.

## Data processing

The module processes files that contain a meta-format in JSON that is pre-processed (cleaned), homogenized data from various semi-structured data sources (Brage), and imports these as Publications in NVA. As a side effect, the handlers produce import/update reports.

Various integrity checks and deduplication occur in this process.

## Notes

1. This module will be removed once all relevant institutional archives have been imported.
2. The origin data is pre-processed by [nva-brage-migration](https://github.com/BIBSYSDEV/nva-brage-migration)
