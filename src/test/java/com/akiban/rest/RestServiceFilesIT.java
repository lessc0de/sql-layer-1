/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.akiban.rest;

import com.akiban.server.service.text.FullTextIndexService;
import com.akiban.http.HttpConductor;
import com.akiban.junit.NamedParameterizedRunner;
import com.akiban.junit.Parameterization;
import com.akiban.server.service.is.BasicInfoSchemaTablesService;
import com.akiban.server.service.is.BasicInfoSchemaTablesServiceImpl;
import com.akiban.server.service.servicemanager.GuicedServiceManager;
import com.akiban.server.test.it.ITBase;
import com.akiban.server.types3.mcompat.mfuncs.WaitFunctionHelpers;
import com.akiban.sql.RegexFilenameFilter;
import com.akiban.util.Strings;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static com.akiban.util.JsonUtils.readTree;
import static org.junit.Assert.assertEquals;

@RunWith(NamedParameterizedRunner.class)
public class RestServiceFilesIT extends ITBase {
    private static final Logger LOG = LoggerFactory.getLogger(RestServiceFilesIT.class.getName());
    private static final File RESOURCE_DIR = new File(
            "src/test/resources/" + RestServiceFilesIT.class.getPackage().getName().replace('.', '/')
    );
    
    public static final String SCHEMA_NAME = "test";

    private static class CaseParams {
        public final String subDir;
        public final String caseName;
        public final String requestMethod;
        public final String requestURI;
        public final String requestBody;
        public final String expectedHeader;
        public final String expectedResponse;
        public final boolean expectedIgnore;
        public final String checkURI;
        public final String checkExpected;

        private CaseParams(String subDir, String caseName,
                           String requestMethod, String requestURI, String requestBody,
                           String expectedHeader, String expectedResponse, boolean expectedIgnore,
                           String checkURI, String checkExpected) {
            this.subDir = subDir;
            this.caseName = caseName;
            this.requestMethod = requestMethod;
            this.requestURI = requestURI;
            this.requestBody = requestBody;
            this.expectedHeader = expectedHeader;
            this.expectedResponse = expectedResponse;
            this.expectedIgnore = expectedIgnore;
            this.checkURI = checkURI;
            this.checkExpected = checkExpected;
        }
    }

    protected final CaseParams caseParams;
    protected final HttpClient httpClient;

    public RestServiceFilesIT(CaseParams caseParams) throws Exception {
        this.caseParams = caseParams;
        this.httpClient = new HttpClient();
        httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        httpClient.setMaxConnectionsPerAddress(10);
        httpClient.start();
    }

    @Override
    protected GuicedServiceManager.BindingsConfigurationProvider serviceBindingsProvider() {
        return super.serviceBindingsProvider()
                .bindAndRequire(RestService.class, RestServiceImpl.class)
                .bindAndRequire(BasicInfoSchemaTablesService.class, BasicInfoSchemaTablesServiceImpl.class);
    }

    @Override
    protected Map<String, String> startupConfigProperties() {
        return uniqueStartupConfigProperties(RestServiceFilesIT.class);
    }

    public static File[] gatherRequestFiles(File dir) {
        File[] result = dir.listFiles(new RegexFilenameFilter(".*\\.(get|put|post|delete|query|explain|patch)"));
        Arrays.sort(result, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        return result;
    }

    private static String dumpFileIfExists(File file) throws IOException {
        if(file.exists()) {
            return Strings.dumpFileToString(file);
        }
        return null;
    }

    private static String trimAndURLEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s.trim().replaceAll("\\s+", " "), "UTF-8");
    }

    @NamedParameterizedRunner.TestParameters
    public static Collection<Parameterization> gatherCases() throws Exception {
        Collection<Parameterization> result = new ArrayList<>();
        for(String subDirName: RESOURCE_DIR.list()) {
            File subDir = new File(RESOURCE_DIR, subDirName);
            if(!subDir.isDirectory()) {
                LOG.warn("Skipping unexpected file: {}", subDir);
                continue;
            }
            for(File requestFile : gatherRequestFiles(subDir)) {
                String inputName = requestFile.getName();
                int dotIndex = inputName.lastIndexOf('.');
                String caseName = inputName.substring(0, dotIndex);

                String basePath = requestFile.getParent() + File.separator + caseName;
                String method = inputName.substring(dotIndex + 1).toUpperCase();
                String uri = Strings.dumpFileToString(requestFile).trim();
                String body = dumpFileIfExists(new File(basePath + ".body"));
                String header = dumpFileIfExists(new File(basePath + ".expected_header"));
                String expected = dumpFileIfExists(new File(basePath + ".expected"));
                boolean expectedIgnore = new File(basePath + ".expected_ignore").exists();
                String checkURI = dumpFileIfExists(new File(basePath + ".check"));
                String checkExpected = dumpFileIfExists(new File(basePath + ".check_expected"));

                if("QUERY".equals(method)) {
                    method = "GET";
                    uri = "/sql/query?q=" + trimAndURLEncode(uri);
                } else if("EXPLAIN".equals(method)) {
                    method = "GET";
                    uri = "/sql/explain?q=" + trimAndURLEncode(uri);
                }

                result.add(Parameterization.create(
                        subDirName + File.separator + caseName,
                        new CaseParams(subDirName, caseName, method, uri, body,
                                       header, expected, expectedIgnore,
                                       checkURI, checkExpected)
                ));
            }
        }
        return result;
    }

    private URL getRestURL(String request) throws MalformedURLException {
        int port = serviceManager().getServiceByClass(HttpConductor.class).getPort();
        String context = serviceManager().getServiceByClass(RestService.class).getContextPath();
        return new URL("http", "localhost", port, context + request);
    }

    private void loadDatabase(String subDirName, FullTextIndexService ftService) throws Exception {
        File subDir = new File(RESOURCE_DIR, subDirName);
        File schemaFile = new File(subDir, "schema.ddl");
        if(schemaFile.exists()) {
            loadSchemaFile(SCHEMA_NAME, schemaFile);
        }
        File spaceFile = new File(subDir, "space.json");
        if(spaceFile.exists()) {
            HttpExchange httpConn = openConnection(getRestURL("/model/apply/" + SCHEMA_NAME), "POST");
            postContents(httpConn, Strings.dumpFileToString(spaceFile).getBytes());
            httpClient.send(httpConn);
            fullyDisconnect(httpConn);
        }
        for (File data : subDir.listFiles(new RegexFilenameFilter(".*\\.dat"))) {
            loadDataFile(SCHEMA_NAME, data);
        }

        String postURI = dumpFileIfExists(new File(subDir, "schema.prepost"));
        if (postURI != null) {
            HttpExchange httpConn = openConnection(getRestURL(postURI.trim()), "POST");
            postContents(httpConn, "[]".getBytes());
            httpClient.send(httpConn);
            fullyDisconnect(httpConn);
        }
        
        // The file should  contain only the name of the wait function
        // (Don't need to make a SELECT node here)
        String waitFunc = dumpFileIfExists(new File(subDir, "background.wait"));
        if (waitFunc != null)
        {
            switch(waitFunc.trim().toLowerCase())
            {
                case "fulltext_maintenance_wait":
                    WaitFunctionHelpers.waitOn(ftService.getBackgroundWorks());
                    break;

                default:
                    throw new UnsupportedOperationException("Unknown Wait Function: " + waitFunc);
            }
        }
    }
    
    public void checkRequest() throws Exception {
        if (caseParams.checkURI != null && caseParams.checkExpected != null) {
            HttpExchange httpConn = openConnection(getRestURL(caseParams.checkURI.trim()), "GET");
            httpClient.send(httpConn);
            httpConn.waitForDone();
            try {
                String actual = getOutput (httpConn);
                compareExpected (caseParams.caseName + " check expected response ", caseParams.checkExpected, actual);
            } finally {
                fullyDisconnect(httpConn);
            }
        }
    }

    private static void postContents(HttpExchange httpConn, byte[] request) throws IOException {
        httpConn.setRequestContentType("application/json");
        httpConn.setRequestHeader("Accept", "application/json");
        httpConn.setRequestContentSource(new ByteArrayInputStream(request));
    }

    @After
    public void finish() throws Exception {
        httpClient.stop();
    }
    
    @Test
    public void testRequest() throws Exception {
        loadDatabase(caseParams.subDir, serviceManager().getServiceByClass(FullTextIndexService.class));
        HttpExchange conn = openConnection(getRestURL(caseParams.requestURI), caseParams.requestMethod);
        try {
            // Request
            if (caseParams.requestMethod.equals("POST") || 
                caseParams.requestMethod.equals("PUT") || 
                caseParams.requestMethod.equals("PATCH")) {
                if (caseParams.requestBody == null) {
                    throw new UnsupportedOperationException ("PUT/POST/PATCH expects request body (<test>.body)");
                }
                LOG.debug(caseParams.requestBody);
                postContents(conn, caseParams.requestBody.getBytes() );
            } // else GET || DELETE

            httpClient.send(conn);
            conn.waitForDone();
            // Response
            String actual = getOutput(conn);
            if(!caseParams.expectedIgnore) {
                compareExpected(caseParams.requestMethod + " response", caseParams.expectedResponse, actual);
            }
            if (caseParams.expectedHeader != null) {
                compareHeaders(conn, caseParams.expectedHeader);
            }
        } finally {
            fullyDisconnect(conn);
        }
        checkRequest();
    }
    
    private HttpExchange openConnection(URL url, String requestMethod) throws IOException, URISyntaxException {
        HttpExchange exchange = new ContentExchange(caseParams.expectedHeader != null);
        exchange.setURI(url.toURI());
        exchange.setMethod(requestMethod);
        return exchange;
    }
     
    private String getOutput(HttpExchange httpConn) throws IOException {
        return ((ContentExchange)httpConn).getResponseContent();
    }
    
    private void compareExpected(String assertMsg, String expected, String actual) throws IOException {
        JsonNode expectedNode = null;
        JsonNode actualNode = null;
        boolean skipNodeCheck = false;
        String expectedTrimmed = (expected != null) ? expected.trim() : "";
        String actualTrimmed = (actual != null) ? actual.trim() : "";
        try {
            if(!expectedTrimmed.isEmpty()) {
                expectedNode = readTree(expected);
            }
            if(!actualTrimmed.isEmpty()) {
                actualNode = readTree(actual);
            }
        } catch(JsonParseException e) {
            // Note: This case handles the jsonp tests. Somewhat fragile, but not horrible yet.
            assertEquals(assertMsg, expectedTrimmed, actualTrimmed);
            skipNodeCheck = true;
        }
        // Try manual equals and then assert strings for pretty print
        if(expectedNode != null && actualNode != null && !expectedNode.equals(actualNode)) {
            assertEquals(assertMsg, expectedNode.toString(), actualNode.toString());
        } else if(!skipNodeCheck) {
            assertEquals(assertMsg, expectedNode, actualNode);
        }
    }
    
    private void compareHeaders (HttpExchange httpConn, String checkHeaders) throws Exception {
        ContentExchange exch = (ContentExchange)httpConn;
        
        String[] headerList = checkHeaders.split (Strings.NL);
        for (String header : headerList) {
            String[] nameValue = header.split(":", 2);
            
            if (nameValue[0].equals("responseCode")) {
                assertEquals ("Headers Response", Integer.parseInt(nameValue[1].trim()), 
                        exch.getResponseStatus());
            } else {
                assertEquals ("Headers check", nameValue[1].trim(),
                        exch.getResponseFields().getStringField(nameValue[0]));
            }
        }
    }

    private void fullyDisconnect(HttpExchange httpConn) throws InterruptedException {
        // If there is a failure, leaving junk in any of the streams can cause cascading issues.
        // Get rid of anything left and disconnect.
        httpConn.waitForDone();
        httpConn.reset();
    }
}