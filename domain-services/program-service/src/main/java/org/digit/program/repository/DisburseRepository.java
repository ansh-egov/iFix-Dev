package org.digit.program.repository;

import org.digit.program.models.disburse.DisburseSearch;
import org.digit.program.models.disburse.Disbursement;
import org.digit.program.models.sanction.Sanction;
import org.digit.program.repository.querybuilder.DisburseQueryBuilder;
import org.digit.program.repository.querybuilder.ExchangeCodeQueryBuilder;
import org.digit.program.repository.rowmapper.DisburseRowMapper;
import org.digit.program.utils.PaginationUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class DisburseRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ExchangeCodeQueryBuilder exchangeCodeQueryBuilder;
    private final DisburseQueryBuilder disburseQueryBuilder;
    private final DisburseRowMapper disburseRowMapper;
    private final SanctionRepository sanctionRepository;
    private final PaginationUtil paginationUtil;

    public DisburseRepository(JdbcTemplate jdbcTemplate, ExchangeCodeQueryBuilder exchangeCodeQueryBuilder,
                              DisburseQueryBuilder disburseQueryBuilder, DisburseRowMapper disburseRowMapper,
                              SanctionRepository sanctionRepository, PaginationUtil paginationUtil) {
        this.jdbcTemplate = jdbcTemplate;
        this.exchangeCodeQueryBuilder = exchangeCodeQueryBuilder;
        this.disburseQueryBuilder = disburseQueryBuilder;
        this.disburseRowMapper = disburseRowMapper;
        this.sanctionRepository = sanctionRepository;
        this.paginationUtil = paginationUtil;
    }

    @Transactional
    public void saveDisburse(Disbursement disbursement, String parentId, Boolean isRoot) {
        List<Object> preparedStmtList = new ArrayList<>();
        String exchangeCodeInsertQuery = exchangeCodeQueryBuilder.buildExchangeCodeDisburseInsertQuery(disbursement, preparedStmtList);
        jdbcTemplate.update(exchangeCodeInsertQuery, preparedStmtList.toArray());

        preparedStmtList = new ArrayList<>();
        String disburseInsertQuery = disburseQueryBuilder.buildDisburseInsertQuery(disbursement, preparedStmtList, parentId);
        jdbcTemplate.update(disburseInsertQuery, preparedStmtList.toArray());

        if (isRoot) {
            preparedStmtList = new ArrayList<>();
            String transactionInsertQuery = disburseQueryBuilder.buildTransactionInsertQuery(disbursement, preparedStmtList);
            jdbcTemplate.update(transactionInsertQuery, preparedStmtList.toArray());
        }
        if (disbursement.getDisbursements() != null) {
            for (Disbursement childDisbursement : disbursement.getDisbursements()) {
                saveDisburse(childDisbursement, disbursement.getId(), false);
            }
        }
    }

    @Transactional
    public void updateDisburse(Disbursement disbursement) {
        List<Object> preparedStmtList = new ArrayList<>();
        String disburseUpdateQuery = disburseQueryBuilder.buildDisburseUpdateQuery(disbursement, preparedStmtList);
        jdbcTemplate.update(disburseUpdateQuery, preparedStmtList.toArray());

        if (disbursement.getDisbursements() != null) {
            for (Disbursement childDisbursement : disbursement.getDisbursements()) {
                updateDisburse(childDisbursement);
            }
        }
    }

    @Transactional
    public void updateDisburseAndSanction(Disbursement disbursement, Sanction sanction) {
        updateDisburse(disbursement);
        sanctionRepository.updateSanctionOnAllocationOrDisburse(Collections.singletonList(sanction));
    }

    @Transactional
    public void createDisburseAndSanction(Disbursement disbursement, Sanction sanction) {
        if (sanction != null)
            sanctionRepository.updateSanctionOnAllocationOrDisburse(Collections.singletonList(sanction));
        saveDisburse(disbursement, null, true);
    }

    public List<Disbursement> searchDisbursements(DisburseSearch disburseSearch) {
        List<Object> preparedStmtList = new ArrayList<>();
        List<Disbursement> disbursements;
        disburseSearch.setPagination(paginationUtil.enrichSearch(disburseSearch.getPagination()));
        String disburseSearchQuery = disburseQueryBuilder.buildDisburseSearchQuery(disburseSearch, preparedStmtList,
                null, true);
        disbursements = jdbcTemplate.query(disburseSearchQuery, preparedStmtList.toArray(), disburseRowMapper);

        if (disbursements == null || disbursements.isEmpty()) {
            return disbursements;
        }
        return setChildDisbursements(disbursements);

    }

    private List<Disbursement> setChildDisbursements(List<Disbursement> disbursements) {
        List<String> parentIds = disbursements.stream().map(Disbursement::getId).collect(Collectors.toList());
        List<Object> preparedStmtList = new ArrayList<>();
        String disburseChildSearchQuery = disburseQueryBuilder.buildDisburseSearchQuery(new DisburseSearch(),
                preparedStmtList, parentIds, false);
        List<Disbursement> childDisbursements = jdbcTemplate.query(disburseChildSearchQuery, preparedStmtList.toArray(),
                disburseRowMapper);

        Map<String, List<Disbursement>> disbursementsMap = childDisbursements.stream()
                .collect(Collectors.groupingBy(Disbursement::getParentId));

        for (Disbursement disbursement : disbursements) {
            if (disbursementsMap.containsKey(disbursement.getId()))
                disbursement.setDisbursements(disbursementsMap.get(disbursement.getId()));
        }
        return disbursements;
    }
}
