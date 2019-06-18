package uk.co.solong.githubrelease.githubapi.exceptions;

import co.uk.solong.githubapi.pojo.GithubError;

public class UnknownGitHubApiException extends GithubApiException {

    private String message;

    public UnknownGitHubApiException(String messageJson) {
        super();
        this.message = messageJson;
    }

    @Override
    public boolean isApplicable(GithubError n) {
        return false;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
