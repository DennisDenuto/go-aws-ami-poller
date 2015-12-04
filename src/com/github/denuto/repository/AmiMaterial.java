package com.github.denuto.repository;

import com.github.denuto.repository.models.PackageMaterialProperties;
import com.github.denuto.repository.models.PackageMaterialProperty;
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
import java.util.Map;

import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.badRequest;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.success;

@Extension
public class AmiMaterial implements GoPlugin {
    Logger logger = Logger.getLoggerFor(AmiMaterial.class);
    private Gson gson = new Gson();

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
                packageMaterialPropertyMap.put("Region", packageMaterialProperty);
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
                packageMaterialProperty.withDisplayName("Package Spec");
                packageMaterialProperty.withDisplayOrder("0");

                packageMaterialPropertyMap.put("PACKAGE_SPEC", packageMaterialProperty);
                return success(gson.toJson(packageMaterialPropertyMap));
            }
        };
    }

    private MessageHandler validateRepositoryConfiguration() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                ValidateRepositoryConfigurationMessage validateRepositoryConfigurationMessage = gson.fromJson(request.requestBody(), ValidateRepositoryConfigurationMessage.class);
                PackageMaterialProperty region = validateRepositoryConfigurationMessage.getRepositoryConfiguration().getProperty("REGION");

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
