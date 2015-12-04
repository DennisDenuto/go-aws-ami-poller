package com.github.denuto.repository;

import com.github.denuto.repository.models.PackageMaterialProperty;
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
        logger.error("request name :" + goPluginApiRequest.requestName());
        logger.error(goPluginApiRequest.requestBody());
        logger.error(goPluginApiRequest.extension());
        logger.error(goPluginApiRequest.requestHeaders().toString());
        logger.error(goPluginApiRequest.requestParameters().toString());
        logger.error("----------------------------------------");

        if (goPluginApiRequest.requestName().equals("repository-configuration")) {
            return repositoryConfigurationsMessageHandler().handle(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals("package-configuration")) {
            return packageConfiguration().handle(goPluginApiRequest);
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


    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", new ArrayList<String>() {{
            add("1.0");
        }});
    }
}
