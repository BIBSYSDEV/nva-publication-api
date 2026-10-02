Feature: File approval when republishing a publication

  Republishing gives every file waiting for approval a pending file approval ticket.
  The tickets are built from the publication as it is now.
  Each uploading institution gets one ticket.
  Unfinished tickets from before are hidden, not reopened.
  Files that are already approved or rejected are left as they are.

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

    Scenario: Files from the same institution but different customers get separate tickets
      Given the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      And a user from institution "A" at another customer uploads 1 file while the publication is unpublished
      When the publication is republished
      Then institution "A" has 2 pending file approval tickets

    Scenario: A file without an uploader institution goes to the publication owner's institution
      Given the publication is unpublished
      And a file without an uploader institution is uploaded while the publication is unpublished
      When the publication is republished
      Then the publication owner's institution has a pending file approval ticket covering 1 file

  Rule: Files that are already approved or rejected get no new approval ticket

    Scenario: An approved file is left out of the new ticket
      Given institution "A" has 1 approved file
      And the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      When the publication is republished
      Then institution "A" has a pending file approval ticket covering 1 file
      And the publication has 1 pending file approval ticket

    Scenario: A rejected file gets no approval ticket
      Given institution "A" has 1 rejected file
      And the publication is unpublished
      When the publication is republished
      Then the publication has 0 pending file approval tickets

  Rule: Unfinished approval tickets are replaced by new tickets for the current files

    Scenario: A ticket created while unpublished is set aside and replaced
      Given the publication is unpublished
      And institution "A" has 1 pending file with an approval ticket
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending file approval ticket covering 1 file
      And the publication has 1 pending file approval ticket

    Scenario: A ticket is replaced even when nothing changed
      Given institution "A" has 1 pending file with an approval ticket
      And the approval ticket of institution "A" is assigned to a curator
      And the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending file approval ticket covering 1 file
      And the pending file approval ticket of institution "A" has no curator

    Scenario: A ticket is replaced when a file was added
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And institution "A" uploads 1 file while the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And institution "A" has a pending file approval ticket covering 2 files
      And the publication has 1 pending file approval ticket

    Scenario: A ticket is set aside when all its files were removed
      Given institution "A" has 1 pending file with an approval ticket
      And the publication is unpublished
      And a file from institution "A" is removed while the publication is unpublished
      When the publication is republished
      Then the original approval ticket of institution "A" is set aside
      And the publication has 0 pending file approval tickets

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
