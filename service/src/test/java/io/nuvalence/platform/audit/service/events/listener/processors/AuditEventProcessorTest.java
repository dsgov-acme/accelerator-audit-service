package io.nuvalence.platform.audit.service.events.listener.processors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.events.event.AuditEvent;
import io.nuvalence.events.exception.EventProcessingException;
import io.nuvalence.platform.audit.service.domain.AuditEventEntity;
import io.nuvalence.platform.audit.service.events.listener.AuditEventProcessor;
import io.nuvalence.platform.audit.service.generated.models.AuditEventRequest;
import io.nuvalence.platform.audit.service.mapper.AuditEventMapper;
import io.nuvalence.platform.audit.service.service.AuditEventService;
import io.nuvalence.platform.audit.service.utils.TestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class AuditEventProcessorTest {

    @Mock private AuditEventMapper eventMapper;
    @Mock private AuditEventService auditEventService;
    @InjectMocks private AuditEventProcessor auditEventProcessor;

    @Test
    public void testExecute() throws IOException, EventProcessingException {
        String eventId = "6950bc28-4c09-43fe-8361-2a26555e92b6";

        AuditEventRequest body = TestUtil.Data.STATE_CHANGE_REQUEST.readJson();
        AuditEventEntity auditEventEntity = TestUtil.Data.STATE_CHANGE_ENTITY.readJson();
        AuditEvent auditEvent = TestUtil.Data.STATE_CHANGE_EVENT.readJson();

        AuditEventEntity eventEntity = TestUtil.Data.STATE_CHANGE_ENTITY.readJson();
        eventEntity.setEventId(UUID.fromString(eventId));

        when(eventMapper.toEntity(auditEvent)).thenReturn(auditEventEntity);
        doNothing().when(auditEventService).saveAuditEvent(any());

        auditEventProcessor.setData(auditEvent);

        // Call the execute method
        auditEventProcessor.execute();

        // Verify that the appropriate methods were called
        verify(eventMapper).toEntity(auditEvent);
        verify(auditEventService).saveAuditEvent(any());
    }
}
