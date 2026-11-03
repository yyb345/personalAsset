package com.example.finance.glossary;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real ES test: GLOSSARY_ES_TEST_URL=http://localhost:19200 mvn -Dtest=GlossaryElasticsearchTest test */
@EnabledIfEnvironmentVariable(named="GLOSSARY_ES_TEST_URL", matches=".+")
class GlossaryElasticsearchTest {
    @Test void percolatorMatchesLocalIncludingBilingualAliases() throws Exception {
        var mapper = new ObjectMapper();
        try (var transport = new RestClientTransport(RestClient.builder(HttpHost.create(System.getenv("GLOSSARY_ES_TEST_URL"))).build(),new JacksonJsonpMapper())) {
            var es = new GlossaryService(Optional.of(new ElasticsearchClient(transport)),mapper,true);
            var local = new GlossaryService(Optional.empty(),mapper,false);
            List<GlossaryTerm> terms;
            try (var input = new ClassPathResource("glossary/terms.json").getInputStream()) {
                terms = mapper.readValue(input,new TypeReference<List<GlossaryTerm>>(){});
            }
            List<String> texts = new ArrayList<>(List.of("😀 LLM使用token和词元，tokenizer不是token。", "<script>LLM</script> RAG self-attention", "没有术语的普通字幕。"));
            terms.forEach(t -> t.names().forEach(n -> texts.add("😀 " + n + "。")));
            for (int offset=0; offset<texts.size(); offset+=80) {
                var batch = texts.subList(offset,Math.min(texts.size(),offset+80));
                var response = es.highlight(batch);
                assertEquals("elasticsearch",response.engine(), "Must actually exercise ES, not fallback");
                assertEquals(local.highlight(batch).results(),response.results(),"Percolator/local mismatch in batch " + offset);
            }
            assertEquals(true,es.status().get("indexReady"));
        }
    }
}
