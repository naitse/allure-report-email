package allure.report.email

import groovy.json.JsonSlurper

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by naitse on 11/13/16.
 */

class FileUtils {
    private String allureSiteDataDirectory = '/target/site/allure-maven-plugin/data/'
    private String currentWorkingDirectory = ''

    FileUtils() {
        Path currentRelativePath = Paths.get("");
        currentWorkingDirectory = currentRelativePath.toAbsolutePath().toString();
    }

    public def getReportFileAsObject(String reportFileName) {

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
