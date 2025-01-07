# publication-event-handlers

This module contains handlers for the many different events associated with Publications and the administration thereof.

In many ways, this is the core of the NVA application since many operations are triggered from this module.

The packages are named according to their purpose. 

  - batch (Batch processing of entries in database)
  - delete (Logical and physical deletion where possible)
  - dynamodbstream (Handling of database change events)
  - expandresources (Triggering of code in [expansion](../expansion/README.md))
  - fanout (Production of further events from database change events)
  - identifiers (Handles hdl.net identifiers)
  - initialization (NOT IN USE)
  - log (Handles log-entry creation)
  - persistence (Handles file persistence operations)
  - recovery (Handles recovery of failed events from persistence to expansion)
  - tickets (Handles events related to tickets)

## Notes

- There are a few candidates for removal/moving here

