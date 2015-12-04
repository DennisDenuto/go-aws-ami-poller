package com.github.denuto.repository.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ValidateRepositoryConfigurationMessage {

    @Expose
    @SerializedName("repository-configuration")
    private Map<String, PackageMaterialProperty> repositoryConfigurationMap;


    public PackageMaterialProperties getRepositoryConfiguration() {
        return new PackageMaterialProperties(repositoryConfigurationMap);
    }

    @Override
    public String toString() {
        return "ValidateRepositoryConfigurationMessage{" +
                "repositoryConfigurationMap=" + repositoryConfigurationMap +
                '}';
    }
}
