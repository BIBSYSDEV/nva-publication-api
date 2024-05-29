package no.unit.nva.expansion;

import no.unit.nva.expansion.model.ExpandedJournal;
import no.unit.nva.model.contexttypes.Journal;

public interface JournalExpansionService {


    ExpandedJournal expandJournal(Journal journal);

}
