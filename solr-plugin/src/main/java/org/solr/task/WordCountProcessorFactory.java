package org.solr.task;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import java.io.IOException;

public class WordCountProcessorFactory extends UpdateRequestProcessorFactory {

    @Override
    public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
        return new UpdateRequestProcessor(next) {
            @Override
            public void processAdd(AddUpdateCommand cmd) throws IOException {
                SolrInputDocument doc = cmd.getSolrInputDocument();

                String fieldName = "another_field";
                String countFieldName = "number_of_words_in_another_field";

                if (doc.containsKey(fieldName)) {
                    Object val = doc.getFieldValue(fieldName);
                    if (val instanceof String) {
                        int wordCount = ((String) val).trim().split("\\s+").length;
                        doc.setField(countFieldName, wordCount);
                    }
                }

                super.processAdd(cmd);
            }
        };
    }
}
