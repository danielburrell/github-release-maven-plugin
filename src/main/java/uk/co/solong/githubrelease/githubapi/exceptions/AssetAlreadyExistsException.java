package uk.co.solong.githubrelease.githubapi.exceptions;

import co.uk.solong.githubapi.pojo.GithubError;

public class AssetAlreadyExistsException extends GithubApiException {

    @Override
    public boolean isApplicable(GithubError n) {

        return n.getErrors().stream().anyMatch(x -> {
            return "already_exists".equals(x.getCode()) && "name".equals(x.getField()) && "ReleaseAsset".equals(x.getResource());
        });
    }
}
