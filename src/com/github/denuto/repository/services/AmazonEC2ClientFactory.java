package com.github.denuto.repository.services;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class AmazonEC2ClientFactory {

    private AmazonEC2ClientFactory() {
    }

    public static AmazonEC2Client newInstance(String region) {
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client();
        amazonEC2Client.withRegion(Regions.fromName(region));
        return amazonEC2Client;
    }
}
