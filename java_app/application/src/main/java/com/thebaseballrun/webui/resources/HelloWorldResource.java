
package com.thebaseballrun.webui.resources;


import com.google.common.base.Optional;
import com.codahale.metrics.annotation.Timed;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.thebaseballrun.webui.core.Saying;
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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @FormDataParam("file") final InputStream fileInputStream,
            @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader) throws IOException, InterruptedException {


        final String temp_upload_file_name = contentDispositionHeader.getFileName();
        //final String temp_upload_file_name = UUID.randomUUID().toString();
        java.nio.file.Path tmpDir = Files.createTempDirectory("temp_geo_data");
        java.nio.file.Path outputPath = Paths.get(tmpDir.toAbsolutePath().toString(), temp_upload_file_name);

        Files.copy(fileInputStream, outputPath);

        /*
        // createdb -p 5432 -h postgis_db -e uploaded_data
        List<String> creat_db_args = new ArrayList<String>();
        creat_db_args.add ("createdb"); // command name
        creat_db_args.add ("-p 5432");
        creat_db_args.add ("-h postgis_db");
        creat_db_args.add ("-e uploaded_data");
        ProcessBuilder create_db_pb = new ProcessBuilder (creat_db_args);
        Process create_db_p = create_db_pb.start();
        int createDbResult = create_db_p.waitFor();

        LOGGER.info("Result of createdb command {}: {}", creat_db_args.toArray().toString(), createDbResult);
        */


        // now build up the ogr2ogr command
        // ogr2ogr -f "PostgreSQL" PG:"host=yourhost user=youruser dbname=yourdb
        //                              password=yourpass" inputfilename.kml
        List<String> args = new ArrayList<String>();
        args.add ("ogr2ogr"); // command name
        args.add ("-f");
        args.add ("PostgreSQL");
        args.add ("PG:host=postgis_db user=docker dbname=uploaded_data password=docker");
        args.add (outputPath.toAbsolutePath().toString());

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

        return Response.status(200).build();

    }

}
