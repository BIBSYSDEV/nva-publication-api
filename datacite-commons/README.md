# datacite-commons

Provides a handler to allow reservation of DOIs (non-findable DOIs for publications with status "Draft")

## Why does this module exist?

This is a short-circuit of a process, which would otherwise be handled as an asynchronous process.  This is an unfortunate optimization for a frontend design that had timing that was considered unacceptable by the end users.

## Notes

1. The code that otherwise handles DOI creation and update can be found in: [https://github.com/BIBSYSDEV/nva-doi-registrar-client](https://github.com/BIBSYSDEV/nva-doi-registrar-client)
