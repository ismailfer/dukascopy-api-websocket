package com.ismail.dukascopy.config;

import java.time.Clock;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;

import lombok.Data;

/**
 * Configuration
 * 
 * @author ismail
 * @since 20220617
 */
@Data
@ConfigurationProperties("dukascopy")
public class DukasConfig
{
    private String credentialJnlp;

    private String credentialUsername;

    private String credentialPassword;

    private String subscriptionInstruments;

    private int wsServerPort;
    
    private long lifecycleWait;
    
    private long connectinWait;

    
    @Bean
    public Clock clock()
    {
        return Clock.systemUTC();
    }
    
    @Bean
    public IClient client() throws ReflectiveOperationException
    {
        return ClientFactory.getDefaultInstance();
    }
}
