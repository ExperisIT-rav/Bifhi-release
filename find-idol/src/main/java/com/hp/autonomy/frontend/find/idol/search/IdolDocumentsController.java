/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.idol.search;

import com.autonomy.aci.client.services.AciErrorException;
import com.hp.autonomy.frontend.find.core.search.DocumentsController;
import com.hp.autonomy.frontend.find.core.search.QueryRestrictionsBuilder;
import com.hp.autonomy.searchcomponents.core.search.*;
import com.hp.autonomy.searchcomponents.idol.search.IdolSearchResult;
import com.hp.autonomy.types.idol.QsElement;
import com.hp.autonomy.types.requests.Documents;
import com.hp.autonomy.types.requests.idol.actions.query.QuerySummaryElement;
import com.hp.autonomy.types.requests.idol.actions.query.params.PrintParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.hp.autonomy.frontend.find.idol.search.IdolRelatedConceptsController;

import java.io.Serializable;
import java.util.*;

@Controller
@RequestMapping(DocumentsController.SEARCH_PATH)
public class IdolDocumentsController extends DocumentsController<String, IdolSearchResult, AciErrorException> {
    @Autowired
    public IdolDocumentsController(final DocumentsService<String, IdolSearchResult, AciErrorException> documentsService, final QueryRestrictionsBuilder<String> queryRestrictionsBuilder) {
        super(documentsService, queryRestrictionsBuilder);
    }

    @Override
    protected <T> T throwException(final String message) throws AciErrorException {
        throw new AciErrorException(message);
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    @RequestMapping(value = QUERY_PATH, method = RequestMethod.GET)
    @ResponseBody
    public Documents<IdolSearchResult> query(@RequestParam(TEXT_PARAM) final String text,
                              @RequestParam(value = RESULTS_START_PARAM, defaultValue = "1") final int resultsStart,
                              @RequestParam(MAX_RESULTS_PARAM) final int maxResults,
                              @RequestParam(SUMMARY_PARAM) final String summary,
                              @RequestParam(value = INDEXES_PARAM, required = false) final List<String> index,
                              @RequestParam(value = FIELD_TEXT_PARAM, defaultValue = "") final String fieldText,
                              @RequestParam(value = SORT_PARAM, required = false) final String sort,
                              @RequestParam(value = MIN_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime minDate,
                              @RequestParam(value = MAX_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime maxDate,
                              @RequestParam(value = HIGHLIGHT_PARAM, defaultValue = "false") final boolean highlight,
                              @RequestParam(value = AUTO_CORRECT_PARAM, defaultValue = "false") final boolean autoCorrect) throws AciErrorException {

        if(index == null) {
            return new Documents<IdolSearchResult>(new ArrayList<IdolSearchResult>(), 0, null, null, null, null);
        }


        final SearchRequest<String> searchRequest = parseRequestParamsToObject(text, resultsStart, maxResults, summary, index, fieldText, sort, minDate, maxDate, highlight, autoCorrect);

        // Get all results as index (not with all the field values)
        Documents<IdolSearchResult> resultIndex = documentsService.queryTextIndex(searchRequest);

        try {
            List<IdolSearchResult> results = resultIndex.getDocuments();
            GetContentRequestIndex<String> getContentRequestIndex = null;
            Set<GetContentRequestIndex<String>> getContentRequestIndexSet = null;
            GetContentRequest<String> getContentRequest = null;
            List<IdolSearchResult> partialResultsWithAllFields = null;
//            HashMap<String, HashSet<String>> resultsReference = new HashMap<String, HashSet<String>>();
            HashSet<String> referenceSet = null;
            Documents<IdolSearchResult> completeResults = null;
            List<String> indexResultsIndex = new ArrayList<String>();
            List<String> referenceResultsIndex = new ArrayList<String>();
            String ref = null;
            String ind = null;
            getContentRequestIndexSet = new HashSet<GetContentRequestIndex<String>>();

            for (IdolSearchResult result : results) {

                indexResultsIndex.add(result.getIndex());
                referenceResultsIndex.add(result.getReference());
//                System.out.println("index = "+result.getIndex());

//                if (resultsReference.containsKey(result.getIndex())) {
//                    referenceSet = resultsReference.get(result.getIndex());
//                    resultsReference.remove(result.getIndex());
//                    referenceSet.add(result.getReference());
//                    resultsReference.put(result.getIndex(), referenceSet);
//                } else {
//                    referenceSet = new HashSet<String>();
//                    referenceSet.add(result.getReference());
//                    resultsReference.put(result.getIndex(), referenceSet);
//                }
            }

//            for (String key : resultsReference.keySet()) {
//                getContentRequestIndexSet = new HashSet<GetContentRequestIndex<String>>();
//                for (String reference : resultsReference.get(key)) {
//                    if (reference.endsWith(".docx") || reference.endsWith(".doc") || reference.endsWith(".pdf")) {
//                        getContentRequestIndexSet.add(new GetContentRequestIndex<>((String) key, Collections.singleton(reference)));
//                    }
//                }
//                getContentRequest = new GetContentRequest<>(getContentRequestIndexSet, PrintParam.All.name());
//
//                if (partialResultsWithAllFields == null) {
//                    partialResultsWithAllFields = documentsService.getDocumentContent(getContentRequest);
//                } else {
//                    partialResultsWithAllFields.addAll(documentsService.getDocumentContent(getContentRequest));
//                }
//            }

            for (int i=0; i<referenceResultsIndex.size(); i++){
                ref = referenceResultsIndex.get(i).toLowerCase();
                ind = indexResultsIndex.get(i);

                if (ref .endsWith(".docx") || ref.endsWith(".doc") || ref.endsWith(".pdf")) {
                    getContentRequestIndexSet.add(new GetContentRequestIndex<>(ind, Collections.singleton(ref)));
                }


            }

            getContentRequest = new GetContentRequest<>(getContentRequestIndexSet, PrintParam.All.name());
            partialResultsWithAllFields = documentsService.getDocumentContent(getContentRequest);

            for (Iterator<IdolSearchResult> it = partialResultsWithAllFields.iterator(); it.hasNext(); ) {
                IdolSearchResult document = it.next();
                String reference = document.getReference().toLowerCase();
                if (!(reference.endsWith(".docx") || reference.endsWith(".doc") || reference.endsWith(".pdf"))) {
                    it.remove();
                }
            }

            completeResults = new Documents<IdolSearchResult>(partialResultsWithAllFields, partialResultsWithAllFields.size(), resultIndex.getExpandedQuery(), resultIndex.getSuggestion(), resultIndex.getAutoCorrection(), resultIndex.getWarnings());

            return completeResults;
        } catch (NullPointerException e) {
            return new Documents<IdolSearchResult>(new ArrayList<IdolSearchResult>(), resultIndex.getTotalResults(), resultIndex.getExpandedQuery(), resultIndex.getSuggestion(), resultIndex.getAutoCorrection(), resultIndex.getWarnings());

        }
    }
}
