# expansion

In NVAs core data model, it is not necessary to know the exact attributes of every data class (such as Journal or Person).

In some cases, it is nice to know these things (when humans search or perform data analysis, or computers evaluate data for further processing (NVI)). This module creates a view of the data that contains all of the "missing" data that is nice to have for other processes. 

The data is "expanded" in various ways and persisted as a document file by this module.

## Who uses this data?

The data is used by [https://github.com/BIBSYSDEV/nva-search-api](https://github.com/BIBSYSDEV/nva-search-api), [https://github.com/BIBSYSDEV/nva-data-report-api](https://github.com/BIBSYSDEV/nva-data-report-api), [https://github.com/BIBSYSDEV/nva-nvi](https://github.com/BIBSYSDEV/nva-nvi).

## Notes

1. There are several approaches to "expanding" data used here, among others, dereferencing URIs (internal and external), inserting data and manifesting analyses of data. These are done for convenience of re-use of NVA data.