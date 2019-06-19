package uk.co.solong.githubrelease;

import co.uk.solong.githubapi.pojo.CreateReleaseRequest;
import co.uk.solong.githubapi.pojo.CreateReleaseResponse;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import uk.co.solong.githubrelease.githubapi.ReleaseApi;
import uk.co.solong.githubrelease.githubapi.exceptions.AssetAlreadyExistsException;
import uk.co.solong.githubrelease.githubapi.exceptions.GithubApiException;
import uk.co.solong.githubrelease.githubapi.exceptions.ReleaseTagExistsException;
import uk.co.solong.githubrelease.githubapi.exceptions.UnknownGitHubApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.retry.policy.SimpleRetryPolicy.DEFAULT_MAX_ATTEMPTS;

@Mojo(name = "github-release", defaultPhase = LifecyclePhase.DEPLOY)
public class GitHubRelease extends AbstractMojo {

    @Parameter
    private List<Artifact> artifacts;

    @Parameter(defaultValue = "${project.version} release")
    private String description;

    @Parameter(defaultValue = "${project.version}")
    private String releaseName;

    @Parameter(defaultValue = "${project.version}")
    private String tag;

    @Parameter(defaultValue = "")
    private String serverId;

    @Parameter(defaultValue = "")
    private String owner;

    @Parameter(defaultValue = "")
    private String repo;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "master")
    private String commitish;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;

    private String token;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skip set to true. Skipping");
            return;
        }
        validate();

        //TODO get list of files
        //TODO ensure files exist.
        //TODO create release
        //TODO attach artifacts
        //TODO handle edge cases. What if the release already exists, what if the upload fails.
        getLog().info("Preparing to publish to GitHub. Plan:");
        getLog().info("GitHub Repo: "+owner+"/"+repo);
        if (isValidToken(token)) {
            getLog().info("Token: OK");
        } else {
            getLog().error("Your GitHub 'Personal Access Token' was not valid");
        }
        getLog().info("Tag: "+tag);
        getLog().info("Release Name: "+releaseName);
        getLog().info("Description: "+description);
        getLog().info("Artifact(s): ");

        List<File> missingFiles = new ArrayList<>();
        artifacts.forEach(x -> {
            getLog().info(x.getFile());
            File f = new File(x.getFile());
            boolean exists = f.exists();
            if (!exists) {
                getLog().error("Artifact file not found: "+x.getFile());
                missingFiles.add(f);
            }
        });

        if (missingFiles.size() > 0) {
            throw new MojoExecutionException("Could not publish the following artefacts as they do not exist:"+System.lineSeparator()+missingFiles.stream().map(File::getAbsolutePath).collect(Collectors.joining(System.lineSeparator())));
        }

        try {


            final ReleaseApi api = new ReleaseApi(token);

            getLog().info("Configuring GitHub release");
            CreateReleaseRequest createReleaseRequest = new CreateReleaseRequest();
            createReleaseRequest.setTagName(tag);
            createReleaseRequest.setDraft(false);
            createReleaseRequest.setName(releaseName);
            createReleaseRequest.setBody(description);
            createReleaseRequest.setPrerelease(false);
            createReleaseRequest.setTargetCommitish(commitish);

            getLog().info("Creating GitHub release");
            CreateReleaseResponse release = api.createRelease(owner, repo, createReleaseRequest);
            getLog().info("Attaching Artifacts:");
            for (Artifact artifact : artifacts) {
                getLog().info("Attaching Artifact: " + artifact.getFile());
                File file = new File(artifact.getFile());
                try {
                    api.uploadAssetResponse(release, file, artifact.getLabel());
                } catch (AssetAlreadyExistsException e) {
                    throw new MojoFailureException("The release could not be created because asset "+file+" already exists in repository: " + owner + "/" + repo);
                }
            }
            getLog().info("GitHub Release Completed Successfully. Artifacts Uploaded.");
        } catch (UnknownGitHubApiException e) {
            throw new MojoFailureException("The GitHub API returned an unknown exception: ", e);
        } catch (ReleaseTagExistsException e) {
            throw new MojoFailureException("The release could not be created because release tag " + tag + " already exists in repository: " + owner + "/" + repo);
        } catch (GithubApiException e) {
            throw new MojoFailureException("The release could not be created. The reason is "+e.getApiMessage());
        } catch(MojoFailureException e) {
            throw e;
        } catch (Throwable e) {
            getLog().error("GitHub publication failed.");
            throw new MojoFailureException("Unknown exception occured whilst running GitHub Release",e);
        }



    }

    private void validate() throws MojoExecutionException {
        if (StringUtils.isEmpty(repo)) {
            throw new MojoExecutionException("<repo> tag must be provided");
        }
        if (StringUtils.isEmpty(owner)) {
            throw new MojoExecutionException("<owner> tag must be provided");
        }
        if (StringUtils.isEmpty(serverId)) {
            throw new MojoExecutionException("<serverId> tag must be provided");
        } else {
            if (settings == null) {
                throw new MojoExecutionException("settings.xml not found");
            }
            if (settings.getServer(serverId) == null) {
                throw new MojoExecutionException("No serverId found in settings.xml for: "+serverId);
            } else {
                String privateKey = settings.getServer(serverId).getPrivateKey();
                if (StringUtils.isEmpty(privateKey)) {
                    throw new MojoExecutionException("No <privateKey> provided for serverId: "+serverId);
                } else {
                    token = privateKey;
                }
            }
        }
        if (artifacts == null) {
            throw new MojoExecutionException("<artifacts> tag must be present");
        }
        for (Artifact a : artifacts) {
            if (StringUtils.isEmpty(a.getFile())) {
                throw new MojoExecutionException("<artifact> tag must have a <file> tag");
            }
        }
    }

    private boolean isValidToken(String token) {
        //FIXME: call github no-op to check the token
        return true;
    }


    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

}