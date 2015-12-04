package com.github.denuto.repository;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.github.denuto.repository.models.PackageMaterialProperty;
import com.github.denuto.repository.models.ValidatePackageConfigurationMessage;
import com.github.denuto.repository.models.ValidateRepositoryConfigurationMessage;
import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.badRequest;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.success;
import static java.lang.String.format;

@Extension
public class AmiMaterial implements GoPlugin {
    public static final String VALIDATE_REPO_CONFIG_INVALID_REGION_MSG = "[{ \"key\": \"REGION\", \"message\" : \"Invalid AWS REGION found: %s\"}]";
    public static final String VALIDATE_REPO_CONFIG_MISSING_REGION_KEY_MSG = "[{ \"key\": \"REGION\", \"message\" : \"Region is required\"}]";
    public static final String VALIDATE_PACKAGE_CONFIG_INVALID_AMI_NAME_MSG = "[{ \"key\": \"AMI_SPEC\", \"message\" : \"AMI spec specified is invalid (must be between 3 and 128 characters long)\"}]";
    public static final String VALIDATE_PACKAGE_CONFIG_INVALID_ARCH_VALUE = "[{ \"key\": \"ARCH\", \"message\" : \"Architecture value incorrect. (i386 | x86_64)\"}]";
    Logger logger = Logger.getLoggerFor(AmiMaterial.class);
    private Gson gson = new Gson();
    public static final List<String> REGIONS = new ArrayList<String>() {{
        add("eu-west-1");
        add("ap-southeast-1");
        add("ap-southeast-2");
        add("eu-central-1");
        add("ap-northeast-1");
        add("us-east-1");
        add("sa-east-1");
        add("us-west-1");
        add("us-west-2");
    }};

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        String requestName = goPluginApiRequest.requestName();

        switch (requestName) {
            case "repository-configuration":
                return repositoryConfigurationsMessageHandler().handle(goPluginApiRequest);
            case "package-configuration":
                return packageConfiguration().handle(goPluginApiRequest);
            case "validate-repository-configuration":
                return validateRepositoryConfiguration().handle(goPluginApiRequest);
            case "validate-package-configuration":
                return validatePackageConfiguration().handle(goPluginApiRequest);
            case "check-repository-connection":
                return checkRepositoryConnection().handle(goPluginApiRequest);
            default:
                logger.error("request name :" + requestName);
                logger.error(goPluginApiRequest.requestBody());
                logger.error(goPluginApiRequest.extension());
                logger.error(goPluginApiRequest.requestHeaders().toString());
                logger.error(goPluginApiRequest.requestParameters().toString());
                logger.error("-------------------------------------------");

                return badRequest("unknown for now");
        }
    }


    private MessageHandler repositoryConfigurationsMessageHandler() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                Map<String, PackageMaterialProperty> packageMaterialPropertyMap = new HashMap<>();

                PackageMaterialProperty packageMaterialProperty = new PackageMaterialProperty();
                packageMaterialProperty.withDisplayName("Region");
                packageMaterialProperty.withDisplayOrder("0");
                packageMaterialPropertyMap.put("REGION", packageMaterialProperty);
                return success(gson.toJson(packageMaterialPropertyMap));
            }
        };
    }

    public MessageHandler packageConfiguration() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                Map<String, PackageMaterialProperty> packageMaterialPropertyMap = new HashMap<>();
                addPackageMaterialProperty(packageMaterialPropertyMap, "AMI name spec", "0", true, "AMI_SPEC");
                addPackageMaterialProperty(packageMaterialPropertyMap, "Architecture (i386 | x86_64)", "1", false, "ARCH");
                addPackageMaterialProperty(packageMaterialPropertyMap, "The key of a tag assigned to the resource. This filter is independent of the tag-value filter.", "2", false, "TAG_KEY");
                addPackageMaterialProperty(packageMaterialPropertyMap, "The value of a tag assigned to the resource. This filter is independent of the tag-key filter.", "3", false, "TAG_VALUE");
                return success(gson.toJson(packageMaterialPropertyMap));
            }

            private void addPackageMaterialProperty(Map<String, PackageMaterialProperty> packageMaterialPropertyMap, String displayName, String displayOrder, boolean required, String key) {
                PackageMaterialProperty packageMaterialProperty = new PackageMaterialProperty();
                packageMaterialProperty.withDisplayName(displayName);
                packageMaterialProperty.withDisplayOrder(displayOrder);
                packageMaterialProperty.withRequired(required);

                packageMaterialPropertyMap.put(key, packageMaterialProperty);
            }
        };
    }

    private MessageHandler validateRepositoryConfiguration() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                ValidateRepositoryConfigurationMessage validateRepositoryConfigurationMessage = gson.fromJson(request.requestBody(), ValidateRepositoryConfigurationMessage.class);
                PackageMaterialProperty regionMaterialProperty = validateRepositoryConfigurationMessage.getRepositoryConfiguration().getProperty("REGION");

                if (regionMaterialProperty != null) {
                    String region = regionMaterialProperty.value();
                    if (REGIONS.contains(region)) {
                        return success("");
                    }
                    return success(format(VALIDATE_REPO_CONFIG_INVALID_REGION_MSG, region));
                }
                return success(VALIDATE_REPO_CONFIG_MISSING_REGION_KEY_MSG);
            }
        };
    }

    private MessageHandler validatePackageConfiguration() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                ValidatePackageConfigurationMessage validatePackageConfigurationMessage = gson.fromJson(request.requestBody(), ValidatePackageConfigurationMessage.class);
                PackageMaterialProperty amiSpec = validatePackageConfigurationMessage.getPackageConfiguration().getProperty("AMI_SPEC");
                PackageMaterialProperty arch = validatePackageConfigurationMessage.getPackageConfiguration().getProperty("ARCH");

                if (amiSpec.value().length() <= 3 || amiSpec.value().length() >= 128) {
                    return success(VALIDATE_PACKAGE_CONFIG_INVALID_AMI_NAME_MSG);
                } else if (arch != null && !arch.value().equals("i386") && !arch.value().equals("x86_64")) {
                    return success(VALIDATE_PACKAGE_CONFIG_INVALID_ARCH_VALUE);
                }

                return success("");
            }
        };
    }

    private MessageHandler checkRepositoryConnection() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                ValidateRepositoryConfigurationMessage validateRepositoryConfigurationMessage = gson.fromJson(request.requestBody(), ValidateRepositoryConfigurationMessage.class);

                AmazonEC2Client amazonEC2Client = new AmazonEC2Client();
                amazonEC2Client.withRegion(Regions.fromName(validateRepositoryConfigurationMessage.getRepositoryConfiguration().getProperty("REGION").value()));
                DryRunResult<DescribeImagesRequest> describeImagesRequestDryRunResult = amazonEC2Client.dryRun(new DescribeImagesRequest());

                if (describeImagesRequestDryRunResult.isSuccessful()) {
                    return success("{\n" +
                            "    \"status\": \"success\",\n" +
                            "    \"messages\": [\n" +
                            "        \"Successfully connected to region\"\n" +
                            "    ]\n" +
                            "}");
                }
                return success("{\n" +
                        "    \"status\": \"failure\",\n" +
                        "    \"messages\": [\n" +
                        "        \"" + describeImagesRequestDryRunResult.getMessage() + "\"\n" +
                        "    ]\n" +
                        "}");
            }
        };
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", new ArrayList<String>() {{
            add("1.0");
        }});
    }
}
