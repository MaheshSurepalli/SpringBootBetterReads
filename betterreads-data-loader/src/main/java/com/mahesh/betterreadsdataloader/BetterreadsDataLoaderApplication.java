package com.mahesh.betterreadsdataloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import javax.annotation.PostConstruct;

import com.mahesh.betterreadsdataloader.author.Author;
import com.mahesh.betterreadsdataloader.author.AuthorRepository;
import com.mahesh.betterreadsdataloader.connection.DataStaxAstraProperties;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.Stream;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterreadsDataLoaderApplication {
   
    @Autowired
    AuthorRepository authorRepository;
    @Value("${datadump.location.author}")
    private String authorDumpLocation;
    @Value("${datadump.location.works}")
    private String worksDumpLocation;

	public static void main(String[] args) {
		SpringApplication.run(BetterreadsDataLoaderApplication.class, args);
	}

    private void initAuthors() throws JSONException{
       
       try(FileInputStream fis = new FileInputStream(authorDumpLocation)){
           Scanner sc = new Scanner(fis);
           while(sc.hasNextLine()){
               JSONObject line = new JSONObject(sc.nextLine().substring(sc.nextLine().indexOf("{")));
               Author author = Author.builder().Id(line.optString("key").replace("/authors/", ""))
                                .name(line.optString("name"))
                                .personalName(line.optString("personal_name"))
                                .build();
                authorRepository.save(author);
           }
           sc.close();
       }
       catch(IOException e){
           e.printStackTrace();
       }

    }
    private void initWorks(){

    }
    @PostConstruct
    public void start() throws JSONException{
        initAuthors();
        initWorks();
    }

	@Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties) {
        Path bundle = astraProperties.getSecureConnectBundle().toPath();
        return builder -> builder.withCloudSecureConnectBundle(bundle);
    }

}
