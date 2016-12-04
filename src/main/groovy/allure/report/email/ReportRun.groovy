package allure.report.email

/**
 * Created by naitse on 11/13/16.
 */
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo( name = "report", defaultPhase = LifecyclePhase.SITE, requiresDependencyResolution = ResolutionScope.TEST)
public class ReportRun extends AbstractMojo {

    @Parameter(property = 'to')
    private String to

    @Parameter(property = 'send')
    private Boolean send

    @Parameter(property = 'from')
    private String from

    @Parameter(property = 'password')
    private String password

    @Parameter(property = 'ignore.broken')
    private Boolean ignoreBroken

    private FileUtils fileUtils
    private Map total
    private Map features
    private Map environment
    private Map defects
    private def failedTests
    private def brokenTests

    public void execute() throws MojoExecutionException {
        System.out.print( "#############################################\nEmail report\n#############################################\n\n" );
        String content

        try{
            fileUtils = new FileUtils()
            loadDataFiles()
            failedTests = defects.defectsList.find{it.status == 'FAILED'}
            brokenTests = defects.defectsList.find{it.status == 'BROKEN'}

            Map build = getBuild()

            if(build.buildNumber == 'local' || !to || !from || !password || (send != null && send == false)){
                throw new AssertionError("")
            }

            if((failedTests?.defects?.size() == 0 && brokenTests?.defects?.size() == 0)){
                throw new AssertionError("No failed test, exiting")
            }

            content = buildOveral(reorderStatistics(total.statistic)) +
                    buildFeatures(features.features) +
                    buildEnvTable(environment.parameter) +
                    buildReportLink(build)

            if(failedTests?.defects?.size() > 0){
                content += buildDefectsTable(features.features, defects.defectsList)
            }

            if(brokenTests?.defects?.size() > 0){
                content += buildDefectsTable(features.features, defects.defectsList, 'Suspicious Failures', 'BROKEN')
            }

            String subject = "[${environment.parameter.find{it.name == 'Product'}?.value?.toUpperCase()}] - [${environment.parameter.find{it.name == 'Environment'}?.value?.toUpperCase()}] - [${environment.parameter.find{it.name == 'Suite'}?.value?.toUpperCase()}] - [AUTOMATION] - Build ${build.buildNumber}"
            HTMLEmail email = new HTMLEmail()
            if(ignoreBroken != null && ignoreBroken == true){
                if(failedTests?.defects?.size() > 0){
                    email.send(from, password, to, subject, content)
                }
            }else{
                email.send(from, password, to, subject, content)
            }


        }catch (AssertionError e){
            getLog().info("")
        }catch(Exception e) {
            getLog().error("Unable to generate email: ${e.message}")
        }

    }

    private Map getBuild(){
        [
            buildUrl:System.getProperty('BUILD_URL') ? System.getProperty('BUILD_URL') : 'local',
            buildNumber: System.getProperty('BUILD_NUMBER') ? System.getProperty('BUILD_NUMBER') : 'local'
        ]
    }

    private loadDataFiles(){
        total = loadDataFile('total.json')
        features = loadDataFile('behaviors.json')
        environment = loadDataFile('environment.json')
        defects = loadDataFile('defects.json')
    }

    private loadDataFile(String fileName){
        fileUtils.getReportFileAsObject(fileName)
    }

    private String buildReportLink(build){
        def buildUrl = "${build.buildUrl}HTML_Report/index.html#behaviors".replace('http://','')
        "<div style=\"height:40px\"></div></br><a href=\"$buildUrl\" target=\"blank\">FULL REPORT LINK</a></br>"
    }

    private String buildOveral(def runStatistics){
        "<h3>Overall:  ${Math.ceil((runStatistics.passed * 100) / (runStatistics.total - runStatistics.pending))} %</h3>${buildProgressBar(runStatistics)}"
    }

    private String buildProgressBar(def runStatistics, def styles = null) {

        styles = (!styles) ? [
                progress: [
                        container: "display:inline-block;height: 18px;max-height: 17px;overflow: hidden;background-color: #f7f7f7;width: 70%;max-width: 650px;min-width: 370px;border-radius:4px;line-height: 1.4;",
                        bar      : "height: 20px;font-size: 14px;color: #ffffff;text-align: center;display: inline-block;",
                        failed   : "background-color: #FD5A3E;",
                        broken   : "background-color: #FFD050;",
                        passed   : "background-color: #97CC64;",
                        pending  : "background-color: #D35EBE;",
                        canceled : "background-color: #AAAAAA;"
                ]
        ]
                : styles;

        String out = "<div style=\"${styles.progress.container}\">"

        runStatistics.total = runStatistics.total - runStatistics.pending;

        runStatistics.each { statistic ->
            if (statistic.key != 'total' && statistic.key != 'pending' && statistic.value > 0) {
                def width = (statistic.value * 100) / runStatistics.total
                out += "<div style=\"width:${width}%;${styles.progress.bar}${styles.progress[statistic.key]}\">${statistic.value}</div>"
            }
        }

        out += '</div>'
    }

    private String buildFeature(def feature) {

        Map featureStyle = [
                progress: [
                        container: "display:inline-block;height: 18px;max-height: 17px;overflow: hidden;background-color: #f7f7f7;width: 100%;border-radius:3px;line-height: 1.4;",
                        bar      : "height: 20px;font-size: 14px;color: #ffffff;text-align: center;display: inline-block;",
                        failed   : "background-color: #FD5A3E;",
                        broken   : "background-color: #FFD050;",
                        passed   : "background-color: #97CC64;",
                        pending  : "background-color: #D35EBE;",
                        canceled : "background-color: #AAAAAA;"
                ]
        ]

        String out =   "<tr>" +
                "<td style=\"min-width:100px;width:20%;border-bottom: 1px solid #E5E5E5;padding:5px;\">${feature.title}</td>" +
                "<td style=\"border-bottom: 1px solid #E5E5E5;padding:5px;\">${buildProgressBar(reorderStatistics(feature.statistic), featureStyle)}</td>" +
                "</tr>"
        out
    }

    private String buildFeatures(def features) {

        String out = "</br><h4>Features</h4><table style=\"width:70%;max-width: 650px;min-width: 370px;border-spacing: 0;border-collapse: collapse;\">"
        features.each { feature ->
            out += buildFeature(feature)
        }
        out + '</table>'
    }

    private String buildDefectsTable(features, defects, String tableName = 'Failures', String status = 'FAILED') {


        String out = "</br><h4>${tableName}</h4><table style=\"width:70%;max-width: 650px;min-width: 370px;border-spacing: 0;border-collapse: collapse;\">"+
                "<tr>" +
                "<th>Feature</th>" +
                "<th>Test</th>" +
                "<th>Failure</th>" +
                "</tr>";

        getFailedTestsList(features, defects, status).each { test ->
            out += "<tr>" +
                    "<td style=\"border-bottom: 1px solid #E5E5E5;padding:5px;\">${test.featureName}</td>" +
                    "<td style=\"border-bottom: 1px solid #E5E5E5;padding:5px;\">${test.title}</td>" +
                    "<td style=\"border-bottom: 1px solid #E5E5E5;padding:5px;\">${test.message}</td>" +
                    "</tr>"

        }

        out + '</table>'

    }

    private def getFailedTestsList(def features, def defects, String status = 'FAILED'){
        List<Map> failedTest = []

        def defectList = defects.find{it.status == status}

        defectList.defects.each{ defect ->
            defect.testCases.each{ test ->
                failedTest.add([featureName:getFeatureName(features, test.uid),title: test.title.split('-').last(), message:defect.failure.message, status:test.status])
            }
        }

        return failedTest

    }

    private String getFeatureName(features, String uid){
        String out = ''
        features.each { feature ->
            feature.stories.each{ story ->
                def found = story.testCases.find{ tc ->
                    tc.uid == uid
                }
                if(found){
                    out = feature.title
                    return;
                }
            }
        }
        out
    }

    private String buildEnvTable(def parameters){
        String out = "</br><h4>Environment</h4><table style=\"width:370px;\">"
        parameters.each{ parameter ->
            out +="<tr>" +
                    "<td style=\"width:370px;border:1px solid #E5E5E5;\">${parameter.name}</td>" +
                    "<td style=\"width:370px;border:1px solid #E5E5E5;\">${parameter.value}</td>" +
                    "</tr>"
        }
        out + '</table>'
    }

    private reorderStatistics(Map statistics){
        Map out = new HashMap()
        out.failed = statistics.failed
        out.canceled = statistics.canceled
        out.broken = statistics.broken
        out.pending = statistics.pending
        out.passed = statistics.passed
        out.total = statistics.total
        out
    }





}
