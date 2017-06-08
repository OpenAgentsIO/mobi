package org.matonto.itests.rest;

/*-
 * #%L
 * itests-etl
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matonto.itests.support.KarafTestSupport;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class RESTIT extends KarafTestSupport {

    private static Boolean setupComplete = false;

    @Inject
    protected static BundleContext thisBundleContext;

    @Before
    public synchronized void setup() throws Exception {
        if (setupComplete) return;

        String ontology = "test-ontology.ttl";
        Files.copy(getBundleEntry(thisBundleContext, "/" + ontology), Paths.get(ontology));

        String vocabulary = "test-vocabulary.ttl";
        Files.copy(getBundleEntry(thisBundleContext, "/" + vocabulary), Paths.get(vocabulary));

        waitForService("(&(objectClass=org.matonto.catalog.rest.impl.CatalogRestImpl))", 10000L);
        waitForService("(&(objectClass=org.matonto.ontology.rest.impl.OntologyRestImpl))", 10000L);
        waitForService("(&(objectClass=org.matonto.ontology.orm.impl.ThingFactory))", 10000L);
        waitForService("(&(objectClass=org.matonto.rdf.orm.conversion.ValueConverterRegistry))", 10000L);

        setupComplete = true;
    }

    private Optional<String> ontologyId = Optional.empty();
    private Optional<String> vocabularyId = Optional.empty();

    @Test
    public void testUploadOntology() throws Exception {
        HttpEntity entity = createFormData("/test-ontology.ttl", "Test Ontology");
        try (CloseableHttpResponse response = uploadFile(entity)) {
            assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder json = new StringBuilder();
            String input;

            while ((input = br.readLine()) != null) {
                json.append(input);
            }
            JSON
        }
    }

    @Test
    public void testDeleteOntology() throws Exception {

    }

    @Test
    public void testUploadVocabulary() throws Exception {
        HttpEntity entity = createFormData("/test-vocabulary.ttl", "Test Vocabulary");
        try (CloseableHttpResponse response = uploadFile(entity)) {
            assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
        }
    }

    @Test
    public void testDeleteVocabulary() throws Exception {

    }


    private HttpEntity createFormData(String filename, String title) throws IOException {
        InputStream ontology = getBundleEntry(thisBundleContext, filename);
        MultipartEntityBuilder mb = MultipartEntityBuilder.create();
        mb.addBinaryBody("file", ontology, ContentType.APPLICATION_OCTET_STREAM, filename);
        mb.addTextBody("title", title);
        mb.addTextBody("description", "Test");
        mb.addTextBody("keywords", "Test");
        return mb.build();
    }

    private CloseableHttpResponse uploadFile(HttpEntity entity) throws IOException {
        CloseableHttpResponse response;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("https://localhost:8443/matontorest/ontologies");
            post.setEntity(entity);
            response = client.execute(post);
        }
        return response;
    }
}
