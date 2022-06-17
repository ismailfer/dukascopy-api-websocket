package com.ismail.dukascopy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.ismail.dukascopy.config.DukasConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Main Dukascopy Application
 * 
 * @author ismail
 * @since 20220617
 */
@SpringBootApplication
@EnableConfigurationProperties({ DukasConfig.class })
@Slf4j
public class DukasApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(DukasApplication.class, args);

        try
        {
            Thread.sleep(Long.MAX_VALUE);

        }
        catch (Exception e)
        {

        }
    }

}
