package com.saucelabs.saucerest;

import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.apache.commons.lang.SerializationUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class SauceRESTTest extends TestCase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SauceREST sauceREST;
    private MockHttpURLConnection urlConnection;

    public class MockOutputStream extends OutputStream {
        public StringBuffer output = new StringBuffer();

        @Override
        public void write(int b) throws IOException {
            output.append((char) b);
        }

        @Override
        public String toString() {
            return output.toString();
        }
    }

    private class MockHttpURLConnection extends HttpURLConnection {
        private URL realURL;
        private InputStream mockInputStream;
        private OutputStream mockOutputStream;

        /**
         * Constructor for the HttpURLConnection.
         */
        protected MockHttpURLConnection() throws MalformedURLException {
            super(new URL("http://fake.site/"));
            try {
                this.mockInputStream = new ByteArrayInputStream("".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            this.mockOutputStream = new MockOutputStream();
        }

        @Override
        public void disconnect() {

        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {

        }

        @Override
        public InputStream getInputStream() throws IOException {
            return mockInputStream;
        }

        public void setInputStream(InputStream mockInputStream) {
            this.mockInputStream = mockInputStream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return mockOutputStream;
        }

        public void setOutputStream(OutputStream mockOutputStream) {
            this.mockOutputStream = mockOutputStream;
        }

        public URL getRealURL() {
            return realURL;
        }

        public void setRealURL(URL realURL) {
            this.realURL = realURL;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        @Override
        public int getResponseCode() throws IOException {
            return this.responseCode;
        }
    }


    @Before
    public void setUp() throws Exception {
        urlConnection = new MockHttpURLConnection();
        this.sauceREST = new SauceREST("fakeuser", "fakekey") {
            @Override
            public HttpURLConnection openConnection(URL url) throws IOException {
                SauceRESTTest.this.urlConnection.setRealURL(url);
                return SauceRESTTest.this.urlConnection;
            }
        };
    }

    @Test
    public void testUserAgent() throws Exception {
        String agent = this.sauceREST.getUserAgent();
        assertNotNull(agent);
        assertThat(agent, not(CoreMatchers.containsString("/null")));
    }

    @Test
    public void testConfirmSerializable() throws Exception {
        SauceREST original = new SauceREST(null, null);
        SauceREST copy = (SauceREST) SerializationUtils.clone(original);
        assertEquals(original, copy);
    }

    @Test
    public void testDoJSONPOST_Created() throws Exception {
        urlConnection.setInputStream(new ByteArrayInputStream(
            "{\"id\": \"29cee6f11f5e4ec6b8b62e98f79bba6f\"}".getBytes("UTF-8")
        ));
        urlConnection.setResponseCode(201);
        this.sauceREST.doJSONPOST(new URL("http://example.org/blah"), new JSONObject());
    }

    @Test(expected=SauceException.NotAuthorized.class)
    public void testDoJSONPOST_NotAuthorized() throws Exception {
        urlConnection.setResponseCode(401);

        thrown.expect(SauceException.NotAuthorized.class);
        this.sauceREST.doJSONPOST(new URL("http://example.org/blah"), new JSONObject());
    }

    @Test
    public void testGetSupportedPlatforms_appium() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(getClass().getResource("/appium.json").openStream());

        String results = sauceREST.getSupportedPlatforms("appium");
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/info/platforms/appium");
    }

    @Test
    public void testRecordCI() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream(
            "{\"id\": \"29cee6f11f5e4ec6b8b62e98f79bba6f\"}".getBytes("UTF-8")
        ));
        sauceREST.recordCI("jenkins", "1.1");
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/stats/ci");
        String output = this.urlConnection.getOutputStream().toString();
        assertEquals(JSONValue.parse(output), JSONValue.parse("{\"platform_version\":\"1.1\",\"platform\":\"jenkins\"}"));
    }


    @Test
    public void testGetUser() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(getClass().getResource("/user_test.json").openStream());
        String userInfo = sauceREST.getUser();
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/users/" + this.sauceREST.getUsername() + "");
    }

    @Test
    public void testGetStoredFiles() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream(
            "[]".getBytes("UTF-8")
        ));
        String userInfo = sauceREST.getStoredFiles();
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/storage/" + this.sauceREST.getUsername() + "");
    }

    @Test
    public void testUpdateJobInfo() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream(
            "[]".getBytes("UTF-8")
        ));
        HashMap<String, Object> updates = new HashMap<String, Object>();
        updates.put("public", "shared");
        sauceREST.updateJobInfo("12345", updates);
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/12345");

        String output = this.urlConnection.getOutputStream().toString();
        assertEquals(JSONValue.parse(output), JSONValue.parse("{\"public\":\"shared\"}"));
    }


    public void testGetTunnels() throws Exception {
        urlConnection.setResponseCode(200);
        String userInfo = sauceREST.getTunnels();
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/" + this.sauceREST.getUsername() + "/tunnels");
    }

    public void testGetTunnelInformation() throws Exception {
        urlConnection.setResponseCode(200);
        String userInfo = sauceREST.getTunnelInformation("1234-1234-1231-123-123");
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/" + this.sauceREST.getUsername() + "/tunnels/1234-1234-1231-123-123");
    }

    public void testGetActivity() throws Exception {
        urlConnection.setResponseCode(200);
        String userInfo = sauceREST.getActivity();
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/" + this.sauceREST.getUsername() + "/activity");
    }

    public void testGetConcurrency() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(getClass().getResource("/users_halkeye_concurrency.json").openStream());

        String concurencyInfo = sauceREST.getConcurrency();
        assertEquals(this.urlConnection.getRealURL().getPath(), "/rest/v1/users/" + this.sauceREST.getUsername() + "/concurrency");
        assertNull(this.urlConnection.getRealURL().getQuery());
        assertEquals(concurencyInfo, "{\"timestamp\": 1447392030.111457, \"concurrency\": {\"halkeye\": {\"current\": {\"overall\": 0, \"mac\": 0, \"manual\": 0}, \"remaining\": {\"overall\": 100, \"mac\": 100, \"manual\": 5}}}}");
    }

    @Test
    public void testUploadFile() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ \"md5\": \"abc123445213242\" }".getBytes("UTF-8")));

        sauceREST.uploadFile(
            new ByteArrayInputStream("".getBytes("UTF-8")),
            "gavin.txt",
            true
        );
        assertEquals(
            "/rest/v1/storage/" + this.sauceREST.getUsername() + "/gavin.txt",
            this.urlConnection.getRealURL().getPath()
        );
        assertEquals(
            "overwrite=true",
            this.urlConnection.getRealURL().getQuery()
        );
    }

    @Test
    public void testStopJob() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.stopJob("123");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/123/stop",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());
    }

    @Test
    public void testGetJobInfo() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.getJobInfo("123");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/123",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());
    }

    @Test
    public void testRetrieveResults() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.retrieveResults("fakePath");
        assertEquals(
            "/rest/v1/fakePath",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());
    }

    @Test
    public void testDownload() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.downloadLog("1234", "location");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/1234/assets/selenium-server.log",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());

        sauceREST.downloadVideo("1234", "location");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/1234/assets/video.flv",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());
    }

    @Test
    public void testJobFailed() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.jobFailed("1234");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/1234",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());
        String output = this.urlConnection.getOutputStream().toString();
        assertEquals(JSONValue.parse(output), JSONValue.parse("{\"passed\":false}"));
    }

    @Test
    public void testJobPassed() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.jobPassed("1234");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs/1234",
            this.urlConnection.getRealURL().getPath()
        );
        assertNull(this.urlConnection.getRealURL().getQuery());
        String output = this.urlConnection.getOutputStream().toString();
        assertEquals(JSONValue.parse(output), JSONValue.parse("{\"passed\":true}"));
    }

    @Test
    public void testGetFullJobs() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.getFullJobs();
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs",
            this.urlConnection.getRealURL().getPath()
        );
        assertEquals("full=true&limit=20", this.urlConnection.getRealURL().getQuery());

        sauceREST.getFullJobs(50);
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/jobs",
            this.urlConnection.getRealURL().getPath()
        );
        assertEquals("full=true&limit=50", this.urlConnection.getRealURL().getQuery());
    }

    @Test
    public void testBuildFullJobs() throws Exception {
        urlConnection.setResponseCode(200);
        urlConnection.setInputStream(new ByteArrayInputStream("{ }".getBytes("UTF-8")));

        sauceREST.getBuildFullJobs("fakePath");
        assertEquals(
            "/rest/v1/" + this.sauceREST.getUsername() + "/build/fakePath/jobs",
            this.urlConnection.getRealURL().getPath()
        );
        assertEquals("full=1", this.urlConnection.getRealURL().getQuery());
    }

    /*
    public void testAddAuthenticationProperty() throws Exception {

    }

    public void testOpenConnection() throws Exception {

    }

    public void testGetPublicJobLink() throws Exception {

    }

    public void testEncodeAuthentication() throws Exception {

    }

    public void testDeleteTunnel() throws Exception {

    }

    public void testGetTunnels() throws Exception {

    }

    public void testGetTunnelInformation() throws Exception {

    }
    */
}
