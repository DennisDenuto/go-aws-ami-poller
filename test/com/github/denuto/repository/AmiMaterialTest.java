package com.github.denuto.repository;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AmiMaterialTest {

    private AmiMaterial amiMaterial;

    @Before
    public void setup() {
        amiMaterial = new AmiMaterial();
    }

    @Test
    public void happyCaseGeneratingRepositoryConfiguration() throws Exception {
        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(new DefaultGoPluginApiRequest("package-repository", "1.0", "repository-configuration"));

        assertThat(goPluginApiResponse.responseCode(), is(200));
        prettyPrint(goPluginApiResponse.responseBody());
        assertExists(goPluginApiResponse.responseBody(), "$.Region.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.Region.display-order");
    }

    @Test
    public void happyCaseGeneratingPackageConfiguration() throws Exception {
        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(new DefaultGoPluginApiRequest("package-repository", "1.0", "package-configuration"));

        assertThat(goPluginApiResponse.responseCode(), is(200));
        prettyPrint(goPluginApiResponse.responseBody());
        assertExists(goPluginApiResponse.responseBody(), "$.PACKAGE_SPEC.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.PACKAGE_SPEC.display-order");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.PACKAGE_SPEC.display-name", "Package Spec");
    }

    @Test
    public void happyCaseGeneratingValidateRepositoryConfiguration() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-repository-configuration");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-99\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertThat(goPluginApiResponse.responseBody(), is(""));
    }

    private <T> void assertJsonValue(String json, String jsonPath, T value) {
        assertThat(JsonPath.compile(jsonPath).<T>read(json), is(value));
    }

    private void assertExists(String json, String jsonPath) {
        try {
            JsonPath.compile(jsonPath).read(json);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public static void prettyPrint(String jsonString) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonString).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(json));
    }

}