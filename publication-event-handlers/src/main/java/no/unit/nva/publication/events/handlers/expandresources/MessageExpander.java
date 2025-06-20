package no.unit.nva.publication.events.handlers.expandresources;

import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;

/**
 * Expander for {@link Message} entities.
 * <p>
 * Actually expands the related {@link no.unit.nva.publication.model.business.TicketEntry} the {@link Message} is
 * modified.
 * </p>
 */
public class MessageExpander extends AbstractEntityExpander {

    @Override
    public boolean canExpand(Class<? extends Entity> entityClass) {
        return Message.class.isAssignableFrom(entityClass);
    }
}
