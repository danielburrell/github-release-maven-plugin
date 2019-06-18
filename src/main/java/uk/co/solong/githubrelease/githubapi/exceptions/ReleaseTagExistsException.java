package uk.co.solong.githubrelease.githubapi.exceptions;

import co.uk.solong.githubapi.pojo.GithubError;

public class ReleaseTagExistsException extends GithubApiException {

    @Override
    public boolean isApplicable(GithubError n) {

        return n.getErrors().stream().anyMatch(x -> {
            return "already_exists".equals(x.getCode()) && "tag_name".equals(x.getField()) && "Release".equals(x.getResource());
        });
    }
}
