Feature: File approval when republishing a publication

  A pending file without a file approval ticket stays locked in "pending approval" with
  nothing to approve it through. Republishing must leave every pending file approvable.

  Background:
    Given a published publication

  Rule: Each pending file is covered by exactly one approval ticket after republishing

    Scenario: A file uploaded while unpublished gets an approval ticket
      Given the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      When the publication is republished
      Then institution "A" has a file approval ticket covering 1 file
      And the publication has 1 file approval ticket

    Scenario: A pending file that lacked a ticket before unpublishing gets one
      Given institution "A" has 1 pending file without an approval ticket
      And the publication is unpublished
      When the publication is republished
      Then institution "A" has a file approval ticket covering 1 file
      And the publication has 1 file approval ticket

    Scenario: A pending file with an approval ticket does not get a second ticket
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      When the publication is republished
      Then institution "A" has a file approval ticket covering 1 file
      And the publication has 1 file approval ticket

    Scenario: Republishing without pending files creates no approval ticket
      Given the publication is unpublished
      When the publication is republished
      Then the publication has 0 file approval tickets

  Rule: Pending files are grouped into one approval ticket per uploading institution

    Scenario: Files from the same institution share a single ticket
      Given the publication is unpublished
      And institution "A" uploads 2 files while the publication is unpublished
      When the publication is republished
      Then institution "A" has a file approval ticket covering 2 files
      And the publication has 1 file approval ticket

    Scenario: Each uploading institution gets its own ticket
      Given the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      And institution "B" uploads 2 files while the publication is unpublished
      When the publication is republished
      Then institution "A" has a file approval ticket covering 1 file
      And institution "B" has a file approval ticket covering 2 files
      And the publication has 2 file approval tickets

    Scenario: Files uploaded while unpublished join the institution's existing ticket
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      When the publication is republished
      Then institution "A" has a file approval ticket covering 2 files
      And the publication has 1 file approval ticket

    Scenario: Files join the institution's existing ticket, which is approved automatically
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      And the customer publishes files without curator approval
      When the publication is republished
      Then institution "A" has a completed file approval ticket approving 2 files
      And the publication has 1 file approval ticket
