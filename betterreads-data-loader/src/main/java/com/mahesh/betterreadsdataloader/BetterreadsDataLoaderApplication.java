package com.mahesh.betterreadsdataloader;

import java.nio.file.Path;

import javax.annotation.PostConstruct;

import com.mahesh.betterreadsdataloader.author.Author;
import com.mahesh.betterreadsdataloader.author.AuthorRepository;
import com.mahesh.betterreadsdataloader.connection.DataStaxAstraProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterreadsDataLoaderApplication {
   
    @Autowired
    AuthorRepository authorRepository;
	public static void main(String[] args) {
		SpringApplication.run(BetterreadsDataLoaderApplication.class, args);
	}

    @PostConstruct
    public void start(){
        Author author = Author.builder().Id("id").name("name").personalName("personalname").build();
        authorRepository.save(author);
        System.out.println("Application is Started");
    }

	@Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties) {
        Path bundle = astraProperties.getSecureConnectBundle().toPath();
        return builder -> builder.withCloudSecureConnectBundle(bundle);
    }

}
