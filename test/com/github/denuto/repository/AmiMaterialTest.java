package com.github.denuto.repository;


import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.github.denuto.repository.services.AmazonEC2ClientFactory;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AmazonEC2ClientFactory.class)
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

    @Test
    public void shouldGenerateErrorIfArchitectureIsNotCorrectValue() throws Exception {
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "validate-package-configuration");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"abcdef\"}," +
                "       \"ARCH\":{\"value\":\"123\"}" +
                "   }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].key", "ARCH");
        assertJsonValue(goPluginApiResponse.responseBody(), "$[0].message", "Architecture value incorrect. (i386 | x86_64)");
    }

    @Test
    public void happyCaseCheckingRepositoryConnection() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.dryRun(any(DryRunSupportedRequest.class))).thenReturn(new DryRunResult(true, null, "", null));
        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "check-repository-connection");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        PowerMockito.verifyStatic();
        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertExists(goPluginApiResponse.responseBody(), "$.status");
        assertExists(goPluginApiResponse.responseBody(), "$.messages");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.status", "success");
    }

    @Test
    public void shouldGenerateFailureIfUnableToConnectToAmazonRegionSpecified() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.dryRun(any(DryRunSupportedRequest.class))).thenReturn(new DryRunResult(false, null, "some error msg from amazon", null));
        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("region-without-permission-to-run-against")).willReturn(amazonEC2ClientMock);
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "check-repository-connection");
        goPluginApiRequest.setRequestBody("{\"repository-configuration\":{\"REGION\":{\"value\":\"region-without-permission-to-run-against\"}}}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        PowerMockito.verifyStatic();
        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertExists(goPluginApiResponse.responseBody(), "$.status");
        assertExists(goPluginApiResponse.responseBody(), "$.messages");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.status", "failure");
    }

    @Test
    public void happyCaseCheckingPackageConnection() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(new Image().withImageId("ami-123456")));
        ArgumentCaptor<DescribeImagesRequest> argument = ArgumentCaptor.forClass(DescribeImagesRequest.class);
        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "check-package-connection");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"amispec\"}," +
                "       \"TAG_KEY\":{\"value\":\"tagkey\"}," +
                "       \"TAG_VALUE\":{\"value\":\"tagvalue\"}," +
                "       \"ARCH\":{\"value\":\"arch\"}" +
                "   }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        verify(amazonEC2ClientMock).describeImages(argument.capture());
        PowerMockito.verifyStatic();
        assertThat(argument.getValue().getFilters(), hasItems(
                new Filter("name", Lists.newArrayList("amispec")),
                new Filter("tag-key", Lists.newArrayList("tagkey")),
                new Filter("tag-value", Lists.newArrayList("tagvalue")),
                new Filter("architecture", Lists.newArrayList("arch"))
        ));
        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertExists(goPluginApiResponse.responseBody(), "$.status");
        assertExists(goPluginApiResponse.responseBody(), "$.messages");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.status", "success");
    }

    @Test
    public void shouldGenerateFailureIfNoImagesAreReturned() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult());
        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "check-package-connection");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"amispec\"}," +
                "       \"TAG_KEY\":{\"value\":\"tagkey\"}," +
                "       \"TAG_VALUE\":{\"value\":\"tagvalue\"}," +
                "       \"ARCH\":{\"value\":\"arch\"}" +
                "   }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertExists(goPluginApiResponse.responseBody(), "$.status");
        assertExists(goPluginApiResponse.responseBody(), "$.messages");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.status", "failure");
    }

    @Test
    public void shouldGenerateAnEc2DescribeImageRequestsBasedOnlyOnPackageConfigurationPresent() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(new Image().withImageId("ami-123456")));
        ArgumentCaptor<DescribeImagesRequest> argument = ArgumentCaptor.forClass(DescribeImagesRequest.class);
        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);
        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "check-package-connection");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"amispec\"}" +
                "   }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        verify(amazonEC2ClientMock).describeImages(argument.capture());
        PowerMockito.verifyStatic();
        assertThat(argument.getValue().getFilters(), not(hasItems(
                new Filter("tag-key", new ArrayList<String>()),
                new Filter("tag-value", new ArrayList<String>()),
                new Filter("architecture", new ArrayList<String>())
        )));
        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertExists(goPluginApiResponse.responseBody(), "$.status");
        assertExists(goPluginApiResponse.responseBody(), "$.messages");
        assertJsonValue(goPluginApiResponse.responseBody(), "$.status", "success");
    }

    @Test
    public void happyCaseGettingLatestRevision() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(
                new Image()
                        .withImageId("ami-2")
                        .withName("amispec 2")
                        .withOwnerId("ownerid1")
                        .withCreationDate("2015-11-12T18:04:29.000Z"),
                new Image()
                        .withImageId("ami-1")
                        .withName("amispec 1")
                        .withOwnerId("ownerid1")
                        .withCreationDate("2015-11-12T18:04:28.000Z")
        ));

        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);

        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "latest-revision");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"amispec\"}" +
                "   }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        prettyPrint(goPluginApiResponse.responseBody());
        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$.revision", "ami-2");
        assertExists(goPluginApiResponse.responseBody(), "$.timestamp");
        assertExists(goPluginApiResponse.responseBody(), "$.user");
        assertExists(goPluginApiResponse.responseBody(), "$.revisionComment");
        assertExists(goPluginApiResponse.responseBody(), "$.trackbackUrl");
    }

    @Test
    public void shouldGenerateEmptyResponseToIndicateCouldNotFindPackageWhenGettingLatestPackageAndCannotFindPackage() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages());

        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);

        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "latest-revision");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"amispec\"}" +
                "   }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertThat(goPluginApiResponse.responseBody(), is(""));
    }

    @Test
    public void happyCaseGettingLatestRevisionSince() throws Exception {
        AmazonEC2Client amazonEC2ClientMock = Mockito.mock(AmazonEC2Client.class);
        when(amazonEC2ClientMock.describeImages(any(DescribeImagesRequest.class))).thenReturn(new DescribeImagesResult().withImages(
                new Image()
                        .withImageId("ami-3")
                        .withName("amispec 3")
                        .withOwnerId("ownerid1")
                        .withCreationDate("2015-11-12T18:04:30.000Z"),
                new Image()
                        .withImageId("ami-2")
                        .withName("amispec 2")
                        .withOwnerId("ownerid1")
                        .withCreationDate("2015-11-12T18:04:29.000Z"),
                new Image()
                        .withImageId("ami-1")
                        .withName("amispec 1")
                        .withOwnerId("ownerid1")
                        .withCreationDate("2015-11-12T18:04:28.000Z")
        ));

        PowerMockito.mockStatic(AmazonEC2ClientFactory.class);
        given(AmazonEC2ClientFactory.newInstance("us-east-1")).willReturn(amazonEC2ClientMock);

        DefaultGoPluginApiRequest goPluginApiRequest = new DefaultGoPluginApiRequest("package-repository", "1.0", "latest-revision-since");
        goPluginApiRequest.setRequestBody("" +
                "{" +
                "   \"repository-configuration\":{\"REGION\":{\"value\":\"us-east-1\"}}, " +
                "   \"package-configuration\":" +
                "   {" +
                "       \"AMI_SPEC\":{\"value\":\"amispec\"}" +
                "   }," +
                " \"previous-revision\": " +
                "   {\n" +
                "        \"revision\": \"ami-1\",\n" +
                "        \"timestamp\": \"2015-11-12T18:04:28.000Z\",\n" +
                "        \"data\": {\n" +
                "        }\n" +
                "    }" +
                "}");

        GoPluginApiResponse goPluginApiResponse = amiMaterial.handle(goPluginApiRequest);

        prettyPrint(goPluginApiResponse.responseBody());
        assertThat(goPluginApiResponse.responseCode(), is(200));
        assertJsonValue(goPluginApiResponse.responseBody(), "$.revision", "ami-2");
        assertExists(goPluginApiResponse.responseBody(), "$.timestamp");
        assertExists(goPluginApiResponse.responseBody(), "$.user");
        assertExists(goPluginApiResponse.responseBody(), "$.revisionComment");
        assertExists(goPluginApiResponse.responseBody(), "$.trackbackUrl");
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