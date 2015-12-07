package com.github.denuto.repository;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.fatboyindustrial.gsonjodatime.Converters;
import com.github.denuto.repository.models.*;
import com.github.denuto.repository.services.AmazonEC2ClientFactory;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.*;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.badRequest;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.success;
import static java.lang.String.format;
import static org.joda.time.DateTime.parse;

@Extension
public class AmiMaterial implements GoPlugin {
    public static final String VALIDATE_REPO_CONFIG_INVALID_REGION_MSG = "[{ \"key\": \"REGION\", \"message\" : \"Invalid AWS REGION found: %s\"}]";
    public static final String VALIDATE_REPO_CONFIG_MISSING_REGION_KEY_MSG = "[{ \"key\": \"REGION\", \"message\" : \"Region is required\"}]";
    public static final String VALIDATE_PACKAGE_CONFIG_INVALID_AMI_NAME_MSG = "[{ \"key\": \"AMI_SPEC\", \"message\" : \"AMI spec specified is invalid (must be between 3 and 128 characters long)\"}]";
    public static final String VALIDATE_PACKAGE_CONFIG_INVALID_ARCH_VALUE = "[{ \"key\": \"ARCH\", \"message\" : \"Architecture value incorrect. (i386 | x86_64)\"}]";
    public static final Comparator<Image> IMAGE_DESC_DATE_ORDER_COMPARATOR = new Comparator<Image>() {
        @Override
        public int compare(Image o1, Image o2) {
            return parse(o2.getCreationDate()).compareTo(parse(o1.getCreationDate()));
        }
    };
    public static final Comparator<Image> IMAGE_ASC_DATE_ORDER_COMPARATOR = new Comparator<Image>() {
        @Override
        public int compare(Image o1, Image o2) {
            return parse(o1.getCreationDate()).compareTo(parse(o2.getCreationDate()));
        }
    };
    Logger logger = Logger.getLoggerFor(AmiMaterial.class);
    private Gson gson = Converters.registerDateTime(new GsonBuilder()).create();

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
            case "check-package-connection":
                return checkPackageConnection().handle(goPluginApiRequest);
            case "latest-revision":
                return latestRevision().handle(goPluginApiRequest);
            case "latest-revision-since":
                return latestRevisionSince().handle(goPluginApiRequest);
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

                AmazonEC2Client amazonEC2Client = AmazonEC2ClientFactory.newInstance(validateRepositoryConfigurationMessage.getRepositoryConfiguration().getProperty("REGION").value());
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

    private MessageHandler checkPackageConnection() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                ValidatePackageConfigurationMessage validatePackageConfigurationMessage = gson.fromJson(request.requestBody(), ValidatePackageConfigurationMessage.class);
                AmazonEC2Client amazonEC2Client = AmazonEC2ClientFactory.newInstance(validatePackageConfigurationMessage.getRepositoryConfiguration().getProperty("REGION").value());

                DescribeImagesRequest describeImagesRequest = buildDescribeImageRequestFromPackageConfiguration(validatePackageConfigurationMessage.getPackageConfiguration());
                if (!amazonEC2Client.describeImages(describeImagesRequest).getImages().isEmpty()) {
                    return success("{\n" +
                            "    \"status\": \"success\",\n" +
                            "    \"messages\": [\n" +
                            "        \"Successfully found package\"\n" +
                            "    ]\n" +
                            "}");
                }

                logger.error("describe image request: " + describeImagesRequest.toString());
                return success("{\n" +
                        "    \"status\": \"failure\",\n" +
                        "    \"messages\": [\n" +
                        "        \"No images found\"\n" +
                        "    ]\n" +
                        "}");
            }


        };
    }

    private MessageHandler latestRevision() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                ValidatePackageConfigurationMessage validatePackageConfigurationMessage = gson.fromJson(request.requestBody(), ValidatePackageConfigurationMessage.class);
                AmazonEC2Client amazonEC2Client = AmazonEC2ClientFactory.newInstance(validatePackageConfigurationMessage.getRepositoryConfiguration().getProperty("REGION").value());

                List<Image> images = amazonEC2Client.describeImages(buildDescribeImageRequestFromPackageConfiguration(validatePackageConfigurationMessage.getPackageConfiguration())).getImages();

                Collections.sort(images, IMAGE_DESC_DATE_ORDER_COMPARATOR);

                Image latestImage = Iterables.getFirst(images, null);
                if (latestImage != null) {
                    return success(String.format("{\n" +
                            "    \"revision\": \"%s\",\n" +
                            "    \"timestamp\": \"%s\",\n" +
                            "    \"user\": \"%s\",\n" +
                            "    \"revisionComment\": \"%s\",\n" +
                            "    \"trackbackUrl\": \"%s\",\n" +
                            "    \"data\": {\n" +
                            "    }\n" +
                            "}", latestImage.getImageId(), latestImage.getCreationDate(), latestImage.getOwnerId(), latestImage.getDescription(), ""));
                }

                return success("");
            }
        };
    }

    private MessageHandler latestRevisionSince() {
        return new MessageHandler() {
            @Override
            public GoPluginApiResponse handle(GoPluginApiRequest request) {
                final LatestPackageRevisionSinceMessage latestRevisionSinceMessage = gson.fromJson(request.requestBody(), LatestPackageRevisionSinceMessage.class);
                AmazonEC2Client amazonEC2Client = AmazonEC2ClientFactory.newInstance(latestRevisionSinceMessage.getRepositoryConfiguration().getProperty("REGION").value());

                List<Image> images = amazonEC2Client.describeImages(buildDescribeImageRequestFromPackageConfiguration(latestRevisionSinceMessage.getPackageConfiguration())).getImages();
                Collections.sort(images, IMAGE_ASC_DATE_ORDER_COMPARATOR);

                Image latestImage = Iterables.getFirst(
                        filter(images, new Predicate<Image>() {
                            @Override
                            public boolean apply(Image input) {
                                return parse(input.getCreationDate()).compareTo(latestRevisionSinceMessage.getPreviousRevision().getTimestamp()) > 0;
                            }
                        }), null);

                if (latestImage != null) {
                    return success(String.format("{\n" +
                            "    \"revision\": \"%s\",\n" +
                            "    \"timestamp\": \"%s\",\n" +
                            "    \"user\": \"%s\",\n" +
                            "    \"revisionComment\": \"%s\",\n" +
                            "    \"trackbackUrl\": \"%s\",\n" +
                            "    \"data\": {\n" +
                            "    }\n" +
                            "}", latestImage.getImageId(), latestImage.getCreationDate(), latestImage.getOwnerId(), latestImage.getDescription(), ""));
                }

                return success("");

            }
        };
    }

    private DescribeImagesRequest buildDescribeImageRequestFromPackageConfiguration(PackageMaterialProperties packageConfiguration) {
        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
        List<Filter> filters = new ArrayList<>();
        filters.add(new Filter("state", newArrayList("available")));
        addPackageConfigToEC2Filter(filters, packageConfiguration.getProperty("AMI_SPEC"), "name");
        addPackageConfigToEC2Filter(filters, packageConfiguration.getProperty("TAG_KEY"), "tag-key");
        addPackageConfigToEC2Filter(filters, packageConfiguration.getProperty("TAG_VALUE"), "tag-value");
        addPackageConfigToEC2Filter(filters, packageConfiguration.getProperty("ARCH"), "architecture");

        describeImagesRequest.withFilters(filters);
        return describeImagesRequest;
    }

    private void addPackageConfigToEC2Filter(List<Filter> filters, PackageMaterialProperty packageMaterialProperty, String filterKey) {
        if (packageMaterialProperty != null && !packageMaterialProperty.value().isEmpty()) {
            filters.add(new Filter(filterKey, newArrayList(packageMaterialProperty.value())));
        }
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", new ArrayList<String>() {{
            add("1.0");
        }});
    }
}
