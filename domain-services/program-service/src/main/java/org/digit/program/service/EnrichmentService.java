package org.digit.program.service;

import lombok.extern.slf4j.Slf4j;
import org.digit.program.configuration.ProgramConfiguration;
import org.digit.program.constants.Status;
import org.digit.program.models.Program;
import org.digit.program.utils.IdGenUtil;
import org.egov.common.contract.models.AuditDetails;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.digit.program.constants.ProgramConstants.ID_NAME;

@Service
@Slf4j
public class EnrichmentService {

    private final IdGenUtil idGenUtil;
    private final ProgramConfiguration configs;

    public EnrichmentService(IdGenUtil idGenUtil, ProgramConfiguration configs) {
        this.idGenUtil = idGenUtil;
        this.configs = configs;
    }

    public void enrichProgramForCreate(Program program) {
        log.info("Enrich Program for Create");
        program.setId(UUID.randomUUID().toString());
        program.setProgramCode(idGenUtil.getIdList(RequestInfo.builder().build(), program.getLocationCode(), configs.getIdgen().get(ID_NAME), "", 1).get(0));
        AuditDetails auditDetails = AuditDetails.builder().createdTime(System.currentTimeMillis()).lastModifiedTime(System.currentTimeMillis()).build();
        program.setAuditDetails(auditDetails);
        org.digit.program.models.Status status = new org.digit.program.models.Status();
        status.setStatusCode(Status.RECEIVED);
        program.setStatus(status);
    }

    public Program enrichProgramForUpdate(Program program) {
        log.info("Enrich Program for Update");
        program.getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
        return program;
    }

}
