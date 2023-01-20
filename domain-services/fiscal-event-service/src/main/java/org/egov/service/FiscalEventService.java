package org.egov.service;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestHeader;
import org.egov.config.FiscalEventConfiguration;
import org.egov.producer.Producer;
import org.egov.repository.FiscalEventRepository;
import org.egov.util.FiscalEventMapperUtil;
import org.egov.util.FiscalEventUtil;
import org.egov.validator.FiscalEventValidator;
import org.egov.web.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class FiscalEventService {

    @Autowired
    private FiscalEventValidator validator;

    @Autowired
    private FiscalEventEnrichmentService enricher;

    @Autowired
    private Producer producer;

    @Autowired
    private FiscalEventConfiguration eventConfiguration;

    @Autowired
    private FiscalEventRepository eventRepository;

    @Autowired
    private FiscalEventUtil fiscalEventUtil;


    /**
     * Validate , enrich and push the fiscal Event request to topic
     *
     * @param fiscalEventRequest
     * @return
     */
    public FiscalEventRequest fiscalEventsV1PushPost(FiscalEventRequest fiscalEventRequest) {
        validator.validateFiscalEventPushPost(fiscalEventRequest);
        enricher.enrichFiscalEventPushPost(fiscalEventRequest);

        // unbundle the bulk request and push to fiscal-event-post-processor
        // to be removed when deprecating Druid
        if (!CollectionUtils.isEmpty(fiscalEventRequest.getFiscalEvent())) {
            RequestHeader requestHeader = fiscalEventRequest.getRequestHeader();
            for (FiscalEvent fiscalEvent : fiscalEventRequest.getFiscalEvent()) {
                ObjectNode fiscalEventRequestNode = JsonNodeFactory.instance.objectNode();
                fiscalEventRequestNode.putPOJO("requestHeader", requestHeader);
                fiscalEventRequestNode.putPOJO("fiscalEvent", fiscalEvent);

                //push with request header details
                producer.push(eventConfiguration.getFiscalPushRequest(), fiscalEventRequestNode);
            }

            //push to ES sink
            producer.push(eventConfiguration.getFiscalEventESSinkTopic(), fiscalEventRequest);

            //push without request header details
            FiscalEventRequestDTO enrichedFiscalEventRequest = enricher.prepareFiscalEventDTOListForPersister(fiscalEventRequest);
            producer.push(eventConfiguration.getFiscalEventPushToPostgresSink(), enrichedFiscalEventRequest);
        }

        return fiscalEventRequest;
    }


    /**
     * Validate the request, search based on the criteria
     *
     * @param fiscalEventGetRequest
     * @return
     */
    public List<FiscalEvent> fiscalEventsV1SearchPost(FiscalEventGetRequest fiscalEventGetRequest) {
        validator.validateFiscalEventSearchPost(fiscalEventGetRequest);
        Criteria searchCriteria = fiscalEventGetRequest.getCriteria();

        if ((searchCriteria.getIds() == null || searchCriteria.getIds().isEmpty()) && StringUtils.isBlank(searchCriteria.getEventType())
                && StringUtils.isBlank(searchCriteria.getTenantId()) && searchCriteria.getFromEventTime() == null
                && searchCriteria.getToEventTime() == null && (searchCriteria.getReferenceId() == null || searchCriteria.getReferenceId().isEmpty()) && searchCriteria.getFromIngestionTime() == null
                && searchCriteria.getToIngestionTime() == null && StringUtils.isBlank(searchCriteria.getReceiver())) {
            return Collections.emptyList();
        }

        List<String> fiscalEventUuids = eventRepository.searchFiscalEventUuids(searchCriteria);

        if(CollectionUtils.isEmpty(fiscalEventUuids)){
            return new ArrayList<>();
        }

        List<FiscalEvent> fiscalEvents = eventRepository.searchFiscalEvent(Criteria.builder().ids(fiscalEventUuids).build());
        fiscalEventUtil.deduplicateReceivers(fiscalEvents);

        /*
         * if (dereferencedFiscalEvents == null || dereferencedFiscalEvents.isEmpty())
         * return Collections.emptyList();
         *
         * List<FiscalEvent> fiscalEvents =
         * mapperUtil.mapDereferencedFiscalEventToFiscalEvent(dereferencedFiscalEvents);
         */
        return fiscalEvents;
    }
}
