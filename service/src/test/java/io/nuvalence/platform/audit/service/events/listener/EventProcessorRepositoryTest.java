package io.nuvalence.platform.audit.service.events.listener;

import io.nuvalence.events.event.AuditEvent;
import io.nuvalence.events.event.Event;
import io.nuvalence.events.event.EventMetadata;
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
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
public class EventProcessorRepositoryTest {
    @InjectMocks private EventProcessorRepository eventProcessorRepository;

    @Mock private ApplicationContext applicationContext;

    @Test
    public void testGetEventProcessor() throws EventProcessingException {
        AuditEvent event = new AuditEvent();
        event.setMetadata(new EventMetadata());
        event.getMetadata().setType("auditEvent");

        Mockito.when(applicationContext.getBean(AuditEventProcessor.class))
                .thenReturn(new AuditEventProcessor(null, null));

        EventProcessor result = eventProcessorRepository.get(event);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AuditEventProcessor.class, result.getClass());
    }

    @Test()
    public void testGetEventProcessorWithInvalidEventType() {
        Event event = new RoleReportingEvent();
        event.setMetadata(new EventMetadata());
        event.getMetadata().setType("invalidEventType");

        Assertions.assertThrows(
                EventProcessingException.class,
                () -> {
                    eventProcessorRepository.get(event);
                });
    }
}
