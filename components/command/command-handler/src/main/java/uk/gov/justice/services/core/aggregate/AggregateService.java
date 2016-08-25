package uk.gov.justice.services.core.aggregate;

import static java.lang.String.format;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.extension.EventFoundEvent;
import uk.gov.justice.services.eventsourcing.common.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.common.exception.InvalidSequenceIdException;
import uk.gov.justice.services.eventsourcing.common.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.SnapshotService;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Service for replaying event streams on aggregates.
 */
@ApplicationScoped
public class AggregateService {

    @Inject
    Logger logger;

    @Inject
    SnapshotService snapshotService;

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private ConcurrentHashMap<String, Class<?>> eventMap = new ConcurrentHashMap<>();

    /**
     * Recreate an aggregate of the specified type by replaying the events from an event stream.
     *
     * @param stream the event stream to replay
     * @param clazz  the type of aggregate to recreate
     * @param <T>    the type of aggregate being recreated
     * @return the recreated aggregate
     */
    public <T extends Aggregate> T get(final EventStream stream, final Class<T> clazz)  {

        Optional<AggregateSnapshot> aggregateSnapshot = snapshotService.getLatestSnapshot(stream.getId());

        //if (aggregateSnapshot.isPresent()) {
            // Deserialize the aggregate based on the type (From byte[] to object)
            // Compare the  sequence no (using getCurrentVersion()) from the deserialized aggregate to that of the Command event stream
            // If snapshot is behind the latest version behind then apply the new events on top of the aggregate
            // check with snapshot strategy if a new snapshot to be created if so create one
        //}

        //if (!aggregateSnapshot.isPresent()) {
         //   snapshotService.storeSnapshot(aggregateSnapshot.get());
       // }

        try {
            logger.trace("Recreating aggregate for instance {} of aggregate type {}", stream.getId(), clazz);
            final T aggregate = clazz.newInstance();
            aggregate.apply(stream.read().map(this::convertEnvelopeToEvent));
            return aggregate;

        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(format("Could not instantiate aggregate of class %s", clazz.getName()), ex);
        }
    }

    /**
     * Register method, invoked automatically to register all event classes into the eventMap.
     *
     * @param event identified by the framework to be registered into the event map
     */
    void register(@Observes final EventFoundEvent event) {
        logger.info("Registering event {}, {} with AggregateService", event.getEventName(), event.getClazz());
        eventMap.putIfAbsent(event.getEventName(), event.getClazz());
    }

    private Object convertEnvelopeToEvent(final JsonEnvelope event) {
        final String name = event.metadata().name();
        if (!eventMap.containsKey(name)) {
            throw new IllegalStateException(format("No event class registered for events of type %s", name));
        }

        return jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), eventMap.get(name));
    }
}
