# datacite-commons

Provides a handler to allow reservation of DOIs (non-findable DOIs for publications with status "Draft")

## Why does this module exist?

This is a workaround that short-circuits the usual asynchronous process for generating DOIs.

This is an optimization for a frontend design that requires a synchronous response.

## Notes

1. The code that otherwise handles DOI creation and update can be found in: [https://github.com/BIBSYSDEV/nva-doi-registrar-client](https://github.com/BIBSYSDEV/nva-doi-registrar-client)
