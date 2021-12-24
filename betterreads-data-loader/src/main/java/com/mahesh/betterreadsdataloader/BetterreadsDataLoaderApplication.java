package com.mahesh.betterreadsdataloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.annotation.PostConstruct;
import javax.swing.text.html.ListView;

import com.mahesh.betterreadsdataloader.author.Author;
import com.mahesh.betterreadsdataloader.author.AuthorRepository;
import com.mahesh.betterreadsdataloader.book.Book;
import com.mahesh.betterreadsdataloader.book.BookRepository;
import com.mahesh.betterreadsdataloader.connection.DataStaxAstraProperties;

import org.json.JSONArray;
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

    @Autowired
    BookRepository bookRepository;
    

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
               String lineObject = sc.nextLine();
               JSONObject line = new JSONObject(lineObject.substring(lineObject.indexOf("{")));
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
    private void initWorks() throws JSONException{
         DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        try(FileInputStream fis = new FileInputStream(worksDumpLocation)){
            Scanner sc = new Scanner(fis);
            while(sc.hasNextLine()){
                String lineObject = sc.nextLine();
                JSONObject line = new JSONObject(lineObject.substring(lineObject.indexOf("{")));
                JSONObject descriptionObj = line.optJSONObject("description");
                String description = descriptionObj!=null?descriptionObj.optString("value"):null;
                JSONObject createdObj = line.optJSONObject("created");
                LocalDate published = createdObj!=null?LocalDate.parse(createdObj.optString("value"),dateFormat):null;
                JSONArray covers = line.optJSONArray("covers");
                List<String> coverIds = new ArrayList<>();
                JSONArray authorsArray = line.optJSONArray("authors");
                List<String> authorIds = new ArrayList<>();
                List<String> authorNames = new ArrayList<>();
                if(covers!=null)
                {
                    for(int i=0;i<covers.length();i++)
                    {
                        coverIds.add(covers.getString(i));
                    }
                }
                if(authorsArray!=null)
                {
                    for(int i=0;i<authorsArray.length();i++)
                    {
                        String authorObj = authorsArray.optJSONObject(i).optJSONObject("author").optString("key");
                        String authorId = authorObj!=null?authorObj.replace("/authors/", ""):null;
                        if(authorId!=null)
                        {
                            authorIds.add(authorId);
                            Optional<Author> author =authorRepository.existsById(authorId)?
                                    authorRepository.findById(authorId):null;
                            authorNames.add(author!=null?author.get().getName():"UNKNOWN");
                        }
                        
                    }
                }
                Book book = Book.builder()
                            .Id(line.optString("key").replace("/works/", ""))
                            .name(line.optString("title"))
                            .description(description)
                            .coverIds(coverIds)
                            .authorNames(authorNames)
                            .authorId(authorIds)
                           .publishedDate(published)
                            .build();
                bookRepository.save(book);
            }
            sc.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }

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
