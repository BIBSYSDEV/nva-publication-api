Feature: File approval when republishing a publication

  A pending file without a file approval ticket stays locked in "pending approval" with
  nothing to approve it through. Republishing works out the tickets from the publication as
  it is now, so every pending file is approvable and no ticket shows an outdated state.

  Background:
    Given a published publication

  Rule: Each pending file is covered by exactly one pending approval ticket after republishing

    Scenario: A file uploaded while unpublished gets an approval ticket
      Given the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      When the publication is republished
      Then institution "A" has a pending file approval ticket covering 1 file
      And the publication has 1 pending file approval ticket

    Scenario: A pending file that lacked a ticket before unpublishing gets one
      Given institution "A" has 1 pending file without an approval ticket
      And the publication is unpublished
      When the publication is republished
      Then institution "A" has a pending file approval ticket covering 1 file
      And the publication has 1 pending file approval ticket

    Scenario: Republishing without pending files creates no approval ticket
      Given the publication is unpublished
      When the publication is republished
      Then the publication has 0 pending file approval tickets

  Rule: Pending files are grouped into one approval ticket per uploading institution

    Scenario: Files from the same institution share a single ticket
      Given the publication is unpublished
      And institution "A" uploads 2 files while the publication is unpublished
      When the publication is republished
      Then institution "A" has a pending file approval ticket covering 2 files
      And the publication has 1 pending file approval ticket

    Scenario: Each uploading institution gets its own ticket
      Given the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      And institution "B" uploads 2 files while the publication is unpublished
      When the publication is republished
      Then institution "A" has a pending file approval ticket covering 1 file
      And institution "B" has a pending file approval ticket covering 2 files
      And the publication has 2 pending file approval tickets

  Rule: An old ticket is reused only when it is identical to the ticket republishing would create

    Scenario: An unchanged ticket is reused and keeps its curator
      Given institution "A" has 1 pending file with an approval ticket
      And the approval ticket of institution "A" is assigned to a curator
      And the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is pending again with the same curator
      And the publication has 1 pending file approval ticket

    Scenario: A ticket is replaced when a file was added
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending file approval ticket covering 2 files
      And the publication has 1 pending file approval ticket

    Scenario: A ticket is replaced when a file was removed
      Given institution "A" has 2 pending files with an approval ticket
      And the publication is unpublished
      And a file from institution "A" is removed while the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending file approval ticket covering 1 file

    Scenario: A ticket is set aside when all its files were removed
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And a file from institution "A" is removed while the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And the publication has 0 pending file approval tickets

    Scenario: A ticket is replaced when a file was changed
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And a file from institution "A" gets a new license while the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending file approval ticket covering 1 file

    Scenario: A ticket is replaced when the publication became a degree
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And the publication is changed into a degree while unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending degree file approval ticket covering 1 file

    Scenario: A ticket is replaced when the customer now publishes files without approval
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And the customer publishes files without curator approval
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a completed file approval ticket approving 1 file
      And the publication has 0 pending file approval tickets
