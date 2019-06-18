package uk.co.solong.githubrelease.githubapi.exceptions;

import co.uk.solong.githubapi.pojo.GithubError;

public abstract class GithubApiException extends Throwable {

    private String apiMessage;
    private GithubError githubError;

    public abstract boolean isApplicable(GithubError n);

    public String getApiMessage() {
        return apiMessage;
    }

    public void setApiMessage(String apiMessage) {
        this.apiMessage = apiMessage;
    }

    public GithubError getGithubError() {
        return githubError;
    }

    public void setGithubError(GithubError githubError) {
        this.githubError = githubError;
    }
}
