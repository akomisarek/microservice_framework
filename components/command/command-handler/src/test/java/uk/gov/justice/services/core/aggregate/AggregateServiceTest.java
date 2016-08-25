package uk.gov.justice.services.core.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.extension.EventFoundEvent;
import uk.gov.justice.services.eventsourcing.common.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.SnapshotService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Unit tests for the {@link AggregateService} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AggregateServiceTest {

    private static final UUID STREAM_ID = randomUUID();

    @Mock
    private Logger logger;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private EventStream eventStream;

    @InjectMocks
    private AggregateService aggregateService;

    @Mock
    private SnapshotService snapshotService;


    // Replay delta events since snapshot was taken

    // Apply commands on the aggregate

    // Notes:
    // Check whether the most recent event created is replayed into the Aggregate
    // so that the snapshot has the most recetnly played events

    //Make sure an snapshotstrategy is injected on the SnapshotService


    @Test
    public void shouldGetTheLatestSnapshotWhenAvailable() {
        // Intercept and ask snapshot service to return an optional snapshot
        when(eventStream.read()).thenReturn(Stream.empty());
        when(eventStream.getId()).thenReturn(STREAM_ID);

        Optional<AggregateSnapshot> aggregateSnapshot = snapshotService.getLatestSnapshot(STREAM_ID);

        //assertThat(aggregateSnapshot, notNullValue());
       // assertThat(aggregateSnapshot.get().getStreamId(),notNullValue());
       // assertEquals(aggregateSnapshot.get().getStreamId(),STREAM_ID);

        //verify(logger).trace("Recreating aggregate for instance {} of aggregate type {}", STREAM_ID, RecordingAggregate.class);

    }

    @Test
    public void shouldCreateANewSnapshotWhenNotAvailable() {
        // Intercept and ask snapshot service to return an optional snapshot

    }

    @Test
    public void shouldCreateAggregateFromEmptyStream() {
        when(eventStream.read()).thenReturn(Stream.empty());
        when(eventStream.getId()).thenReturn(STREAM_ID);

        RecordingAggregate aggregate = aggregateService.get(eventStream, RecordingAggregate.class);

        assertThat(aggregate, notNullValue());
        assertThat(aggregate.recordedEvents, empty());
        verify(logger).trace("Recreating aggregate for instance {} of aggregate type {}", STREAM_ID, RecordingAggregate.class);
    }

    @Test
    public void shouldCreateAggregateFromSingletonStream() {
        JsonObject eventPayloadA = mock(JsonObject.class);
        EventA eventA = mock(EventA.class);
        when(jsonObjectToObjectConverter.convert(eventPayloadA, EventA.class)).thenReturn(eventA);
        when(eventStream.read()).thenReturn(Stream.of(envelopeFrom(metadataWithRandomUUID("eventA"), eventPayloadA)));
        when(eventStream.getId()).thenReturn(STREAM_ID);

        aggregateService.register(new EventFoundEvent(EventA.class, "eventA"));

        RecordingAggregate aggregate = aggregateService.get(eventStream, RecordingAggregate.class);

        assertThat(aggregate, notNullValue());
        assertThat(aggregate.recordedEvents, hasSize(1));
        assertThat(aggregate.recordedEvents.get(0), equalTo(eventA));
        verify(logger).info("Registering event {}, {} with AggregateService", "eventA", EventA.class);
        verify(logger).trace("Recreating aggregate for instance {} of aggregate type {}", STREAM_ID, RecordingAggregate.class);
    }

    @Test
    public void shouldCreateAggregateFromStreamOfTwo() {
        JsonObject eventPayloadA = mock(JsonObject.class);
        JsonObject eventPayloadB = mock(JsonObject.class);
        EventA eventA = mock(EventA.class);
        EventB eventB = mock(EventB.class);
        when(jsonObjectToObjectConverter.convert(eventPayloadA, EventA.class)).thenReturn(eventA);
        when(jsonObjectToObjectConverter.convert(eventPayloadB, EventB.class)).thenReturn(eventB);
        when(eventStream.read()).thenReturn(Stream.of(
                envelopeFrom(metadataWithRandomUUID("eventA"), eventPayloadA),
                envelopeFrom(metadataWithRandomUUID("eventB"), eventPayloadB)));
        when(eventStream.getId()).thenReturn(STREAM_ID);

        aggregateService.register(new EventFoundEvent(EventA.class, "eventA"));
        aggregateService.register(new EventFoundEvent(EventB.class, "eventB"));

        RecordingAggregate aggregate = aggregateService.get(eventStream, RecordingAggregate.class);

        assertThat(aggregate, notNullValue());
        assertThat(aggregate.recordedEvents, hasSize(2));
        assertThat(aggregate.recordedEvents.get(0), equalTo(eventA));
        assertThat(aggregate.recordedEvents.get(1), equalTo(eventB));
        verify(logger).info("Registering event {}, {} with AggregateService", "eventA", EventA.class);
        verify(logger).info("Registering event {}, {} with AggregateService", "eventB", EventB.class);
        verify(logger).trace("Recreating aggregate for instance {} of aggregate type {}", STREAM_ID, RecordingAggregate.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionForUnregisteredEvent() {
        JsonObject eventPayloadA = mock(JsonObject.class);
        EventA eventA = mock(EventA.class);
        when(jsonObjectToObjectConverter.convert(eventPayloadA, EventA.class)).thenReturn(eventA);
        when(eventStream.read()).thenReturn(Stream.of(envelopeFrom(metadataWithRandomUUID("eventA"), eventPayloadA)));

        aggregateService.get(eventStream, RecordingAggregate.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForNonInstantiatableEvent() {
        JsonObject eventPayloadA = mock(JsonObject.class);
        EventA eventA = mock(EventA.class);
        when(jsonObjectToObjectConverter.convert(eventPayloadA, EventA.class)).thenReturn(eventA);
        when(eventStream.read()).thenReturn(Stream.of(envelopeFrom(metadataWithRandomUUID("eventA"), eventPayloadA)));

        aggregateService.register(new EventFoundEvent(EventA.class, "eventA"));

        aggregateService.get(eventStream, PrivateAggregate.class);
    }

    public static class RecordingAggregate implements Aggregate {

        List<Object> recordedEvents = new ArrayList<>();

        @Override
        public Object apply(Object event) {
            recordedEvents.add(event);
            return event;
        }
    }

    private static class PrivateAggregate implements Aggregate {

        @Override
        public Object apply(Object event) {
            return event;
        }
    }

    @Event("eventA")
    public static class EventA {

    }

    @Event("eventB")
    public static class EventB {

    }
}
