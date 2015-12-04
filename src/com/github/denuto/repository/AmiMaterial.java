package com.github.denuto.repository;

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
        logger.info("request name :" + requestName);
        logger.info(goPluginApiRequest.requestBody());
        logger.info(goPluginApiRequest.extension());
        logger.info(goPluginApiRequest.requestHeaders().toString());
        logger.info(goPluginApiRequest.requestParameters().toString());
        logger.info("----------------------------------------");

        if (requestName.equals("repository-configuration")) {
            return repositoryConfigurationsMessageHandler().handle(goPluginApiRequest);
        } else if (requestName.equals("package-configuration")) {
            return packageConfiguration().handle(goPluginApiRequest);
        } else if (requestName.equals("validate-repository-configuration")) {
            return validateRepositoryConfiguration().handle(goPluginApiRequest);
        } else if (requestName.equals("validate-package-configuration")) {
            return validatePackageConfiguration().handle(goPluginApiRequest);
        }
        return badRequest("unknown for now");
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
                PackageMaterialProperty packageMaterialProperty = new PackageMaterialProperty();
                packageMaterialProperty.withDisplayName("AMI Spec");
                packageMaterialProperty.withDisplayOrder("0");

                packageMaterialPropertyMap.put("AMI_SPEC", packageMaterialProperty);
                return success(gson.toJson(packageMaterialPropertyMap));
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

                if (amiSpec.value().length() <= 3 || amiSpec.value().length() >= 128) {
                    return success("[{ \"key\": \"AMI_SPEC\", \"message\" : \"AMI spec specified is invalid (must be between 3 and 128 characters long)\"}]");
                }

                return success("");
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
