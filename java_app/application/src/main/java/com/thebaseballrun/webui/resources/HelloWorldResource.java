
package com.thebaseballrun.webui.resources;


import com.google.common.base.Optional;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.thebaseballrun.webui.core.Saying;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

        java.nio.file.Path outputPath = Paths.get(tmpDir.toAbsolutePath().toString(), temp_upload_file_name);

        Files.copy(fileInputStream, outputPath);

        String fileWeSaved = outputPath.toAbsolutePath().toString();


        return fileWeSaved;
    }

    private void importFileToPostGISshp2pgsql(String fileWeSaved) throws IOException, InterruptedException {
        // now build up the ogr2ogr command
        // ogr2ogr -f "PostgreSQL" PG:"host=yourhost user=youruser dbname=yourdb
        //                              password=yourpass" inputfilename.kml
        List<String> args = new ArrayList<String>();
        args.add ("shp2pgsql"); // command name
        args.add ("-W");
        args.add ("latin1");
        args.add (fileWeSaved);

        ProcessBuilder shp2pgsql = new ProcessBuilder (args);
        //shp2pgsql.redirectErrorStream(true);

        // export PGPASSWORD=docker
        // psql -h postgis_db -U docker -w uploaded_data
        List<String> psqlArgs = new ArrayList<String>();
        psqlArgs.add("psql");
        psqlArgs.add ("-h");
        psqlArgs.add ("postgis_db");
        psqlArgs.add("-U");
        psqlArgs.add("docker");
        psqlArgs.add("-w");
        psqlArgs.add("uploaded_data");
        ProcessBuilder psqlImport = new ProcessBuilder(psqlArgs);
        psqlImport.redirectErrorStream(true);
        psqlImport.environment().put("PGPASSWORD", "docker");

        Process psqlProcess = psqlImport.start();



        OutputStream theInputForPsql = psqlProcess.getOutputStream();

        Process shp2Process = shp2pgsql.start();
        //int otherProcessResult = shp2Process.waitFor();
        //int processResult = psqlProcess.waitFor();

        InputStream theShp2PsqlResults = shp2Process.getInputStream();

        InputStreamReader resultsReader = new InputStreamReader(theShp2PsqlResults);
        OutputStreamWriter sqlWriter = new OutputStreamWriter(theInputForPsql);


        // make sure the buffer is big enough to hold long INSERT commands.
        // psql would fail if we don't write whole lines at a time.
        BufferedReader bufferedReader = new BufferedReader(resultsReader, 1024*128);
        BufferedWriter bufferedWriter = new BufferedWriter(sqlWriter, 1024*128);

        int count = 0;
        String inLine;
        while ((inLine = bufferedReader.readLine()) != null) {
            //LOGGER.info("Read in {} chars from sql dump: loop {}", inLine.length(), ++count);
            bufferedWriter.write(inLine);
        }

        bufferedWriter.flush();
        bufferedWriter.close();
        bufferedReader.close();


        // preint out the command output
        BufferedReader is;  // reader for output of process
        String line;
        // getInputStream gives an Input stream connected to
        // the process standard output. Just use it to make
        // a BufferedReader to readLine() what the program writes out.
        is = new BufferedReader(new InputStreamReader(psqlProcess.getInputStream()), 1024*1024);

        StringBuilder commandOutput = new StringBuilder("");
        while ((line = is.readLine()) != null)
            commandOutput.append(line);

        LOGGER.info("Result of command {}: {}", args.toString(), commandOutput.toString());

    }

    private void importFileToPostGISogr2ogr(String fileWeSaved) throws IOException, InterruptedException {
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
            String extension = FilenameUtils.getExtension(importFile);

            // if we get a shp file, use shp2pgsql instead of ogr2ogr
            if (extension.toLowerCase().equals("shp")) {
                try {
                    importFileToPostGISshp2pgsql(importFile);
                }
                catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    throw ex;
                }
            } else {
                importFileToPostGISogr2ogr(importFile);
            }
        }

        // using the incoming file name as table name is probably not a good idea.
        // should generate some id for the geom table name, and maintain a table mapping
        // ids to the incoming filenames, and other metadata.  could possibly use this
        // for some kind of search functionality.  then the POST upload handler would
        // return a path to the data as json (or whatever other format)

        return Response.status(200).build();

    }

}
