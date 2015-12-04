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
        assertExists(goPluginApiResponse.responseBody(), "$.REGION.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.REGION.display-order");
    }

    @Test
    public void happyCaseGeneratingPackageConfiguration() throws Exception {
        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(new DefaultGoPluginApiRequest("package-repository", "1.0", "package-configuration"));

        assertThat(goPluginApiResponse.responseCode(), is(200));

        prettyPrint(goPluginApiResponse.responseBody());
        assertExists(goPluginApiResponse.responseBody(), "$.AMI_SPEC.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.AMI_SPEC.display-order");
        assertExists(goPluginApiResponse.responseBody(), "$.AMI_SPEC.required");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.AMI_SPEC.display-name", "AMI name spec");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.AMI_SPEC.display-order", "0");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.AMI_SPEC.required", true);

        assertExists(goPluginApiResponse.responseBody(), "$.ARCH.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.ARCH.display-order");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.ARCH.display-name", "Architecture (i386 | x86_64)");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.ARCH.display-order", "1");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.ARCH.required", false);

        assertExists(goPluginApiResponse.responseBody(), "$.TAG_KEY.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.TAG_KEY.display-order");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.TAG_KEY.display-name", "The key of a tag assigned to the resource. This filter is independent of the tag-value filter.");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.TAG_KEY.display-order", "2");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.TAG_KEY.required", false);

        assertExists(goPluginApiResponse.responseBody(), "$.TAG_VALUE.display-name");
        assertExists(goPluginApiResponse.responseBody(), "$.TAG_VALUE.display-order");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.TAG_VALUE.display-name", "The value of a tag assigned to the resource. This filter is independent of the tag-key filter.");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.TAG_VALUE.display-order", "3");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.TAG_VALUE.required", false);
    }

    @Test
    public void happyCaseGeneratingValidateRepositoryConfiguration() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-repository-configuration");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertThat(goPluginApiResponse.responseBody(), is(""));
    }

    @Test
    public void shouldGenerateAnErrorIfRegionSpecifiedIsNotAValidAWSRegion() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-repository-configuration");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-9\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].key", "REGION");
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].message", "Invalid AWS REGION found: us-east-9");
    }

    @Test
    public void shouldGenerateErrorIfRegionIsNotSpecifiedAtAll() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-repository-configuration");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].key", "REGION");
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].message", "Region is required");
    }

    @Test
    public void happyCaseGeneratingSuccessForValidatePackageConfiguration() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-package-configuration");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, \"package-configuration\":{\"AMI_SPEC\":{\"value\":\"ami-1234\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertThat(goPluginApiResponse.responseBody(), is(""));
    }

    @Test
    public void shouldGenerateErrorIfPackageConfigurationContainsInvalidAmiNameDueToBeingTooShort() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-package-configuration");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, \"package-configuration\":{\"AMI_SPEC\":{\"value\":\"a\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].key", "AMI_SPEC");
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].message", "AMI spec specified is invalid (must be between 3 and 128 characters long)");
    }

    @Test
    public void shouldGenerateErrorIfPackageConfigurationContainsInvalidAmiNameDueToHavingTooManyCharacters() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-package-configuration");

        String longAmiName = buildLongString(130);
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, \"package-configuration\":{\"AMI_SPEC\":{\"value\":\"" + longAmiName + "\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].key", "AMI_SPEC");
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].message", "AMI spec specified is invalid (must be between 3 and 128 characters long)");
    }

    private String buildLongString(int size) {
        String longAmiName = "";
        for (int i = 0; i < size; i++) {
            longAmiName += "a";
        }
        return longAmiName;
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