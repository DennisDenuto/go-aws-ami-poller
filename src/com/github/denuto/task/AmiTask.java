package com.github.denuto.task;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

@Extension
public class AmiTask implements GoPlugin {
    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        return null;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return null;
    }
}
