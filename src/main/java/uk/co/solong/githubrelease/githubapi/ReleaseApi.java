package uk.co.solong.githubrelease.githubapi;

import co.uk.solong.githubapi.pojo.CreateReleaseRequest;
import co.uk.solong.githubapi.pojo.CreateReleaseResponse;
import co.uk.solong.githubapi.pojo.UploadAssetResponse;
import com.google.api.client.http.UriTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import uk.co.solong.githubrelease.githubapi.exceptions.AssetAlreadyExistsException;
import uk.co.solong.githubrelease.githubapi.exceptions.GithubApiException;
import uk.co.solong.githubrelease.githubapi.exceptions.ReleaseTagExistsException;
import uk.co.solong.githubrelease.githubapi.exceptions.UnknownGitHubApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReleaseApi {

    //POST


    private final String CREATE_URL = "/repos/{owner}/{repo}/releases";
    private final String BASE_URL = "https://api.github.com";
    private final String token;
    private final ExceptionManager exceptionManager;

    public ReleaseApi(String token) {
        this.token = token;
        exceptionManager = new ExceptionManager();
        exceptionManager.add(new ReleaseTagExistsException());
        exceptionManager.add(new AssetAlreadyExistsException());
    }

    public CreateReleaseResponse createRelease(String owner, String repo, CreateReleaseRequest createReleaseRequest) throws GithubApiException, IOException {

        RestTemplate r = new RestTemplate();
        Map<String, String> map = new HashMap<>();
        map.put("owner", owner);
        map.put("repo", repo);
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("token " + token));

        HttpEntity<CreateReleaseRequest> h = new HttpEntity<>(createReleaseRequest, headers);

        try {
            ResponseEntity<CreateReleaseResponse> responseEntity = r.exchange(BASE_URL + CREATE_URL, HttpMethod.POST, h, CreateReleaseResponse.class, map);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            if (e.getRawStatusCode() == 422) {
                throw exceptionManager.findOrThrow(e.getResponseBodyAsString());
            }
            throw e;
        }


    }

    public UploadAssetResponse uploadAssetResponse(CreateReleaseResponse releaseResponse, File asset, String label) throws IOException, GithubApiException {
        String mimeType = Files.probeContentType(asset.toPath());
        byte[] assetBytes = Files.readAllBytes(asset.toPath());
        String uploadUrl = releaseResponse.getUploadUrl();

        Map<String, String> urlParams = new HashMap<>();
        String assetName = asset.getName();
        urlParams.put("name", assetName);

        if (!StringUtils.isEmpty(label)) {
            urlParams.put("label", label);
        }

        //expand the uploadUrl
        uploadUrl = UriTemplate.expand(uploadUrl, urlParams, false);

        RestTemplate r = new RestTemplate();

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put(HttpHeaders.AUTHORIZATION, Collections.singletonList("token " + token));
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(mimeType));

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(assetBytes, headers);

        try {
            ResponseEntity<UploadAssetResponse> responseEntity = r.exchange(uploadUrl, HttpMethod.POST, httpEntity, UploadAssetResponse.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            if (e.getRawStatusCode() == 422) {
                throw exceptionManager.findOrThrow(e.getResponseBodyAsString());
            }
            throw e;
        }

    }
}
