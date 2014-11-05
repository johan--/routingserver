
package com.thebaseballrun.webui.resources;


import com.google.common.base.Optional;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.FormDataParam;
import com.thebaseballrun.webui.core.Saying;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
    private final String template;
    private final String defaultName;
    private final AtomicLong counter;
    private static Logger LOGGER = LoggerFactory.getLogger(HelloWorldResource.class);
    private final static String[] CONVERTIBLE_TYPES = {"shp", "kml", "json"};
    private final static List<String> convertibleTypes = Lists.newArrayList("shp", "kml", "json");

    public HelloWorldResource(String template, String defaultName) {
        this.template = template;
        this.defaultName = defaultName;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    public Saying sayHello(@QueryParam("name") Optional<String> name) {
        final String value = String.format(template, name.or(defaultName));
        return new Saying(counter.incrementAndGet(), value);
    }

    private String handleInputStream(java.nio.file.Path tmpDir, InputStream fileInputStream, String temp_upload_file_name) throws IOException, InterruptedException {
        LOGGER.info("HANDLING INPUT FILE: {}", temp_upload_file_name);

        //final String temp_upload_file_name = contentDispositionHeader.getFileName();
        //final String temp_upload_file_name = UUID.randomUUID().toString();
        //java.nio.file.Path tmpDir = Files.createTempDirectory("temp_geo_data");
        java.nio.file.Path outputPath = Paths.get(tmpDir.toAbsolutePath().toString(), temp_upload_file_name);

        Files.copy(fileInputStream, outputPath);

        String fileWeSaved = outputPath.toAbsolutePath().toString();


        return fileWeSaved;
    }

    private void importFileToPostGIS(String fileWeSaved) throws IOException, InterruptedException {
        // now build up the ogr2ogr command
        // ogr2ogr -f "PostgreSQL" PG:"host=yourhost user=youruser dbname=yourdb
        //                              password=yourpass" inputfilename.kml
        List<String> args = new ArrayList<String>();
        args.add ("ogr2ogr"); // command name
        args.add ("-skipfailures");
        args.add ("-f");
        args.add ("PostgreSQL");
        args.add ("PG:host=postgis_db user=docker dbname=uploaded_data password=docker");
        args.add (fileWeSaved);
        args.add ("-nlt");
        args.add ("GEOMETRY");


        ProcessBuilder pb = new ProcessBuilder (args);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int processResult = p.waitFor();


        // preint out the command output
        BufferedReader is;  // reader for output of process
        String line;
        // getInputStream gives an Input stream connected to
        // the process standard output. Just use it to make
        // a BufferedReader to readLine() what the program writes out.
        is = new BufferedReader(new InputStreamReader(p.getInputStream()));

        StringBuilder commandOutput = new StringBuilder("");
        while ((line = is.readLine()) != null)
            commandOutput.append(line);

        LOGGER.info("Result of command {}: {}", args.toString(), commandOutput.toString());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            FormDataMultiPart multiPart) throws IOException, InterruptedException {


        List<FormDataBodyPart> fields = multiPart.getFields("file");
        List<String> filesWeCanImport = new ArrayList<>();

        // create one tmp dir, and put all files in one upload into same dir
        java.nio.file.Path tmpDir = Files.createTempDirectory("temp_geo_data");

        for(FormDataBodyPart field : fields){

            String whatWeStored = handleInputStream(tmpDir,field.getValueAs(InputStream.class), field.getContentDisposition().getFileName());
            String extension = FilenameUtils.getExtension(whatWeStored);

            LOGGER.info("for file: {}, using extension: {}", whatWeStored, extension);

            // if we see a file type we can convert, add it to the list
            if (convertibleTypes.contains(extension.toLowerCase())) {
                LOGGER.info("COOL!  Let's try to import file {}", whatWeStored);
                filesWeCanImport.add(whatWeStored);
            }
        }


        for (String importFile : filesWeCanImport) {
            importFileToPostGIS(importFile);
        }

        return Response.status(200).build();

    }

}
