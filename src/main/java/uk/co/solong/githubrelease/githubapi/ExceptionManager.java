package uk.co.solong.githubrelease.githubapi;

import co.uk.solong.githubapi.pojo.GithubError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.co.solong.githubrelease.githubapi.exceptions.GithubApiException;
import uk.co.solong.githubrelease.githubapi.exceptions.ReleaseTagExistsException;
import uk.co.solong.githubrelease.githubapi.exceptions.UnknownGitHubApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExceptionManager {
    private final List<GithubApiException> list = new ArrayList<>();
    public void add(GithubApiException e) {
        list.add(e);
    }


    public GithubApiException findOrThrow(String messageJson) throws UnknownGitHubApiException, IOException {
        ObjectMapper m = new ObjectMapper();
        GithubError error = m.readValue(messageJson, GithubError.class);
        UnknownGitHubApiException e = new UnknownGitHubApiException(messageJson);
        GithubApiException githubApiException = list.stream().filter(x -> x.isApplicable(error)).findFirst().orElseThrow(() -> e);
        githubApiException.setApiMessage(messageJson);
        githubApiException.setGithubError(error);
        return githubApiException;
    }
}
