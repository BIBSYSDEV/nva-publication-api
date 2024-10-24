package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import static java.util.Objects.nonNull;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Course;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Degree.Builder;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;

public class DegreeMerger extends PublicationContextMerger {

    @JacocoGenerated
    public DegreeMerger(Record record) {
        super(record);
    }

    public Degree merge(Degree degree, PublicationContext publicationContext)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        if (publicationContext instanceof Degree newDegree) {
            return new Builder()
                       .withIsbnList(getIsbnList(degree.getIsbnList(), newDegree.getIsbnList()))
                       .withSeries(getSeries(degree.getSeries(), newDegree.getSeries()))
                       .withPublisher(getPublisher(degree.getPublisher(), newDegree.getPublisher()))
                       .withSeriesNumber(getNonNullValue(degree.getSeriesNumber(), newDegree.getSeriesNumber()))
                       .withCourse(getCourse(degree.getCourse(), newDegree.getCourse()))
                       .build();
        } else {
            return degree;
        }
    }

    private static Course getCourse(Course oldCourse, Course newCourse) {
        return nonNull(oldCourse) ? oldCourse : newCourse;
    }
}
