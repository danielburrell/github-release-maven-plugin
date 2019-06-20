package uk.co.solong.githubrelease.githubapi;

import com.google.api.client.http.UriTemplate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import uk.co.solong.githubrelease.Artifact;
import uk.co.solong.githubrelease.GitHubRelease;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

public class ReleaseApiTest {

    @Test @Ignore
    public void test() throws MojoExecutionException, MojoFailureException {
        GitHubRelease gitHubRelease = new GitHubRelease();
        gitHubRelease.setTag("10.0");
        gitHubRelease.setRepo("testrepo");
        gitHubRelease.setReleaseName("8.0");
        gitHubRelease.setOwner("danielburrell");
        gitHubRelease.setDescription("1.0");
        gitHubRelease.setUseExistingTag(true);
        gitHubRelease.setServerId("github");
        Settings settings = new Settings();
        List<Server> servers = new ArrayList<>();
        Server server = new Server();
        servers.add(server);
        server.setId("github");
        server.setPrivateKey("");
        settings.setServers(servers);
        gitHubRelease.setSettings(settings);
        gitHubRelease.setArtifacts(Arrays.asList(new Artifact("/Users/danielburrell/p.zip", "test")));
        gitHubRelease.execute();
    }
}