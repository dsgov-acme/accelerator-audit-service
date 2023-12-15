package io.nuvalence.platform.audit.service.mapper;

import io.nuvalence.events.event.AuditEvent;
import io.nuvalence.events.event.EventMetadata;
import io.nuvalence.events.event.dto.ActivityEventData;
import io.nuvalence.events.event.dto.AuditEventDataBase;
import io.nuvalence.events.event.dto.BusinessObjectMetadata;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.platform.audit.service.domain.ActivityEventEntity;
import io.nuvalence.platform.audit.service.domain.AuditEventEntity;
import io.nuvalence.platform.audit.service.domain.StateChangeEventEntity;
import io.nuvalence.platform.audit.service.domain.enums.TypeEnum;
import io.nuvalence.platform.audit.service.error.ApiException;
import io.nuvalence.platform.audit.service.generated.models.AuditEventRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps entity to Api models using MapStruct library.
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditEventMapper {

    /**
     * Maps AuditEventRequest to AuditEvent.
     * @param request AuditEventRequest
     * @param businessObjectId Unique identifier for a business object of the specified type.
     * @param businessObjectType Type of business object.
     * @return AuditEvent
     */
    @ToAuditEvent
    AuditEvent toAuditEvent(
            AuditEventRequest request, UUID businessObjectId, String businessObjectType);

    /**
     * Sets audit event fields.
     * @param request AuditEventRequest
     * @param businessObjectId Unique identifier for a business object of the specified type.
     * @param businessObjectType Type of business object.
     * @param event AuditEvent
     */
    @AfterMapping
    default void toAuditEvent(
            AuditEventRequest request,
            UUID businessObjectId,
            String businessObjectType,
            @MappingTarget AuditEvent event) {
        EventMetadata metadata = new EventMetadata();
        metadata.setTimestamp(request.getTimestamp());
        metadata.setOriginatorId(request.getRequestContext().getOriginatorId());
        metadata.setType("auditEvent");
        metadata.setUserId(request.getRequestContext().getUserId());
        event.setMetadata(metadata);

        // Set the businessObject property
        BusinessObjectMetadata businessObject = new BusinessObjectMetadata();
        businessObject.setId(businessObjectId);
        businessObject.setType(businessObjectType);
        event.setBusinessObject(businessObject);

        AuditEventDataBase eventData = null;
        if (request.getEventData().getType().equals(TypeEnum.ACTIVITY_EVENT_DATA.getValue())) {
            eventData = new ActivityEventData();
        } else if (request.getEventData()
                .getType()
                .equals(TypeEnum.STATE_CHANGE_EVENT_DATA.getValue())) {

            io.nuvalence.platform.audit.service.generated.models.StateChangeEventData
                    requestEventData =
                            (io.nuvalence.platform.audit.service.generated.models
                                            .StateChangeEventData)
                                    request.getEventData();

            StateChangeEventData stateChangeEventData = new StateChangeEventData();
            stateChangeEventData.setNewState(requestEventData.getNewState());
            stateChangeEventData.setOldState(requestEventData.getOldState());
            eventData = stateChangeEventData;
        }
        eventData.setSchema(request.getEventData().getSchema());
        eventData.setType(request.getEventData().getType());
        eventData.setActivityType(request.getEventData().getActivityType());
        eventData.setData(request.getEventData().getData());
        event.setEventData(eventData);
    }

    @ToAuditEventEntity
    ActivityEventEntity toActivityEventEntity(AuditEvent event);

    /**
     * Sets activity audit event fields.
     *
     * @param event audit event
     * @param entity  entity
     */
    @AfterMapping
    default void toActivityEventEntity(
            AuditEvent event, @MappingTarget ActivityEventEntity entity) {
        if (!(event.getEventData() instanceof ActivityEventData)) {
            throw ApiException.Builder.badRequest(
                    "Invalid eventData type: " + event.getEventData().getClass().getName());
        }

        ActivityEventData activityEventData = (ActivityEventData) event.getEventData();
        entity.setActivityType(activityEventData.getActivityType());
        entity.setData(activityEventData.getData());
    }

    @ToAuditEventEntity
    StateChangeEventEntity toStateChangeEventEntity(AuditEvent event);

    /**
     * Sets state change audit event fields.
     *
     * @param event audit event event
     * @param entity  entity
     */
    @AfterMapping
    default void toStateChangeEventEntity(
            AuditEvent event, @MappingTarget StateChangeEventEntity entity) {
        if (!(event.getEventData() instanceof StateChangeEventData)) {
            throw ApiException.Builder.badRequest(
                    "Invalid eventData type: " + event.getEventData().getClass().getName());
        }

        StateChangeEventData stateChangeEventData = (StateChangeEventData) event.getEventData();
        entity.setActivityType(stateChangeEventData.getActivityType());
        entity.setOldState(stateChangeEventData.getOldState());
        entity.setNewState(stateChangeEventData.getNewState());
        entity.setData(stateChangeEventData.getData());
    }

    /**
     * Retrieves the corresponding {@link TypeEnum} enum value based on the type string
     * from the provided {@link AuditEventRequest}.
     *
     * @param event The {@link AuditEventRequest} containing the type information.
     * @return The {@link TypeEnum} enum value associated with the type string from the request.
     * @throws IllegalArgumentException If the type string from the request does not match
     *                                  any valid {@link TypeEnum} enum value.
     */
    default TypeEnum getEnumFromEvent(AuditEvent event) {
        String type = event.getEventData().getType();
        return TypeEnum.fromValue(type);
    }

    /**
     * Converts event to entity for both activity and state change types.
     *
     * @param event    audit event event
     * @return audit event entity
     */
    default AuditEventEntity toEntity(AuditEvent event) {
        return Optional.ofNullable(event)
                .filter(req -> req.getEventData() != null)
                .filter(req -> req.getEventData().getType() != null)
                .map(
                        req ->
                                req.getEventData()
                                                .getType()
                                                .equals(TypeEnum.ACTIVITY_EVENT_DATA.getValue())
                                        ? toActivityEventEntity(event)
                                        : toStateChangeEventEntity(event))
                .orElse(null);
    }

    @FromAuditEventEntity
    @Mapping(target = "eventData.type", expression = "java(getEnumFromEntity(activityEventEntity))")
    AuditEvent fromActivityEventEntity(ActivityEventEntity entity);

    /**
     * Sets activity audit event fields.
     *
     * @param entity audit event entity
     * @param auditEvent  audit event
     */
    @AfterMapping
    default void fromActivityEventEntity(
            ActivityEventEntity entity, @MappingTarget AuditEvent auditEvent) {
        ActivityEventData activityEventData = new ActivityEventData();
        activityEventData.setSchema(auditEvent.getEventData().getSchema());
        activityEventData.setType(auditEvent.getEventData().getType());
        activityEventData.setActivityType(entity.getActivityType());
        activityEventData.setData(entity.getData());
        auditEvent.setEventData(activityEventData);

        EventMetadata metadata = new EventMetadata();
        metadata.setTimestamp(entity.getTimestamp());
        metadata.setId(entity.getEventId());
        metadata.setOriginatorId(entity.getRequestContext().getOriginatorId());
        metadata.setType("auditEvent");
        metadata.setUserId(entity.getRequestContext().getUserId());
        auditEvent.setMetadata(metadata);
    }

    default String getEnumFromEntity(AuditEventEntity value) {
        return value.getType().getValue();
    }

    @FromAuditEventEntity
    @Mapping(
            target = "eventData.type",
            expression = "java(getEnumFromEntity(stateChangeEventEntity))")
    AuditEvent fromStateChangeEventEntity(StateChangeEventEntity entity);

    /**
     * Sets activity audit event fields.
     *
     * @param entity audit event entity
     * @param auditEvent  auditEvent
     */
    @AfterMapping
    default void fromStateChangeEventEntity(
            StateChangeEventEntity entity, @MappingTarget AuditEvent auditEvent) {
        StateChangeEventData stateChangeEventData = new StateChangeEventData();
        stateChangeEventData.setSchema(auditEvent.getEventData().getSchema());
        stateChangeEventData.setType(auditEvent.getEventData().getType());
        stateChangeEventData.setActivityType(entity.getActivityType());
        stateChangeEventData.setOldState(entity.getOldState());
        stateChangeEventData.setNewState(entity.getNewState());
        stateChangeEventData.setData(entity.getData());
        auditEvent.setEventData(stateChangeEventData);

        EventMetadata metadata = new EventMetadata();
        metadata.setTimestamp(entity.getTimestamp());
        metadata.setId(entity.getEventId());
        metadata.setOriginatorId(entity.getRequestContext().getOriginatorId());
        metadata.setType("auditEvent");
        metadata.setUserId(entity.getRequestContext().getUserId());
        auditEvent.setMetadata(metadata);
    }

    /**
     * Converts entity to model for both activity and state change types.
     *
     * @param entity audit event entity
     * @return audit event model
     */
    default AuditEvent fromEntity(AuditEventEntity entity) {
        return Optional.ofNullable(entity)
                .map(
                        auditEventEntity -> {
                            if (auditEventEntity instanceof ActivityEventEntity) {
                                return fromActivityEventEntity(
                                        (ActivityEventEntity) auditEventEntity);
                            } else {
                                return fromStateChangeEventEntity(
                                        (StateChangeEventEntity) auditEventEntity);
                            }
                        })
                .orElse(null);
    }

    default List<AuditEvent> fromEntities(List<AuditEventEntity> entities) {
        return entities.stream().map(this::fromEntity).toList();
    }

    @ToAuditEventModel
    io.nuvalence.platform.audit.service.generated.models.AuditEvent toAuditEventModel(
            AuditEvent event);

    /**
     * Sets activity audit event fields.
     * @param event audit event
     * @param eventModel audit event model
     */
    @AfterMapping
    default void toAuditEventModel(
            AuditEvent event,
            @MappingTarget
                    io.nuvalence.platform.audit.service.generated.models.AuditEvent eventModel) {
        io.nuvalence.platform.audit.service.generated.models.AuditEventDataBase eventData = null;
        if (eventModel.getEventData().getType().equals(TypeEnum.ACTIVITY_EVENT_DATA.getValue())) {
            eventData =
                    new io.nuvalence.platform.audit.service.generated.models.ActivityEventData();
        } else if (eventModel
                .getEventData()
                .getType()
                .equals(TypeEnum.STATE_CHANGE_EVENT_DATA.getValue())) {

            StateChangeEventData requestEventData = (StateChangeEventData) event.getEventData();

            io.nuvalence.platform.audit.service.generated.models.StateChangeEventData
                    stateChangeEventData =
                            new io.nuvalence.platform.audit.service.generated.models
                                    .StateChangeEventData();
            stateChangeEventData.setNewState(requestEventData.getNewState());
            stateChangeEventData.setOldState(requestEventData.getOldState());
            eventData = stateChangeEventData;
        }
        eventData.setSchema(event.getEventData().getSchema());
        eventData.setType(event.getEventData().getType());
        eventData.setActivityType(event.getEventData().getActivityType());
        eventData.setData(event.getEventData().getData());
        eventModel.setEventData(eventData);
    }
}
