# scopus-import

Imports data from Scopus (https://scopus.com). Data is downloaded weekly by [https://github.com/BIBSYSDEV/dlr-nva-email-service](https://github.com/BIBSYSDEV/dlr-nva-email-service) to s3.

From s3 data is processed to become ImportCandidates and persisted in database with files.

