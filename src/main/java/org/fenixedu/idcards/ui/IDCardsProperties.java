package org.fenixedu.idcards.ui;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class IDCardsProperties {

    @ConfigurationManager(description = "ID Card Configurations")
    public interface ConfigurationProperties {
        @ConfigurationProperty(
                key = "sibs.webService.username",
                description = "UserName used to communicate with the SIBS Web Service, which returns the ID card production state.")
        public String sibsWebServiceUsername();

        @ConfigurationProperty(
                key = "sibs.webService.password",
                description = "Password used to communicate with the SIBS Web Service, which returns the ID card production state.")
        public String sibsWebServicePassword();
    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
