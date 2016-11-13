package allure.report.email

import jdk.nashorn.internal.parser.JSONParser

/**
 * Created by naitse on 11/13/16.
 */
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope;
import groovy.json.JsonSlurper
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Says "Hi" to the user.
 *
 */
@Mojo( name = "report", defaultPhase = LifecyclePhase.SITE, requiresDependencyResolution = ResolutionScope.TEST)
public class ReportRun extends AbstractMojo {

    private String allureSiteDataDirectory = '/target/site/allure-maven-plugin/data/'
    private String currentWorkingDirectory = ''
    private Map total
    private Map features
    private Map environment
    private Map defects

    public void execute() throws MojoExecutionException {
        System.out.print( "#############################################\nEmail report\n#############################################\n" );
        Path currentRelativePath = Paths.get("");
        currentWorkingDirectory = currentRelativePath.toAbsolutePath().toString();
        loadDataFiles()
        getLog().info(total.toString())
    }

    private loadDataFiles(){
        total = loadDataFile('total.json')
        features = loadDataFile('behaviors.json')
        environment = loadDataFile('environment.json')
        defects = loadDataFile('defects.json')
    }

    private loadDataFile(String fileName){

        getReportFileAsObject(fileName)

    }

    private def getReportFileAsObject(String reportFileName) {

        getLog().info("$currentWorkingDirectory$allureSiteDataDirectory$reportFileName")

        InputStream is = new FileInputStream("$currentWorkingDirectory$allureSiteDataDirectory$reportFileName")

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new JsonSlurper().parseText(sb.toString()) as Map
    }



}
