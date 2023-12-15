package io.nuvalence.platform.audit.service.events.listener;

import io.nuvalence.events.event.Event;
import io.nuvalence.events.event.RoleReportingEvent;
import io.nuvalence.events.exception.EventProcessingException;
import io.nuvalence.events.subscriber.EventProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventListenerImplTest {
    @InjectMocks private EventListenerImpl eventListener;

    @Mock private EventProcessorRepository eventProcessorGetter;

    @Mock private EventProcessor<RoleReportingEvent> eventProcessor;

    @Test
    public void testOnEvent() throws EventProcessingException {
        Event event = new RoleReportingEvent();
        Mockito.when(eventProcessorGetter.get(event)).thenReturn(eventProcessor);

        eventListener.onEvent(event);

        Mockito.verify(eventProcessor).execute();
    }

    @Test
    public void testOnEventWithErrorInEventProcessing() throws EventProcessingException {
        Event event = new RoleReportingEvent();
        Mockito.when(eventProcessorGetter.get(event)).thenReturn(eventProcessor);
        Mockito.doThrow(new EventProcessingException("Processing error"))
                .when(eventProcessor)
                .execute();

        Assertions.assertThrows(
                EventProcessingException.class,
                () -> {
                    eventListener.onEvent(event);
                });
    }
}
