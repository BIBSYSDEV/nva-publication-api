package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import java.util.ArrayList;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.time.duration.DefinedDuration;
import no.unit.nva.model.time.duration.Duration;
import no.unit.nva.model.time.duration.UndefinedDuration;

public final class MusicPerformanceMerger extends PublicationInstanceMerger<MusicPerformance> {

    public MusicPerformanceMerger(MusicPerformance musicPerformance) {
        super(musicPerformance);
    }

    @Override
    public MusicPerformance merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof MusicPerformance newMusicPerformance) {
            return new MusicPerformance(mergeCollections(this.publicationInstance.getManifestations(), newMusicPerformance.getManifestations(), ArrayList::new),
                                        getDuration(this.publicationInstance.getDuration(), newMusicPerformance.getDuration()));
        } else {
            return this.publicationInstance;
        }
    }

    private static Duration getDuration(Duration oldDuration, Duration newDuration) {
        if (oldDuration instanceof DefinedDuration definedDuration) {
            return definedDuration;
        }
        if (newDuration instanceof DefinedDuration definedDuration) {
            return definedDuration;
        }
        if (oldDuration instanceof UndefinedDuration undefinedDuration) {
            return undefinedDuration;
        } else {
            return newDuration;
        }
    }
}
