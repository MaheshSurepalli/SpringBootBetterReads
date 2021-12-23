package com.mahesh.betterreadsdataloader.connection;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ConfigurationProperties(prefix="datastax.astra")
public class DataStaxAstraProperties {
    private File secureConnectBundle;
}
