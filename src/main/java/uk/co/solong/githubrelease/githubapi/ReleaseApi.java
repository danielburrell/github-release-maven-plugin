package uk.co.solong.githubrelease.githubapi;

import co.uk.solong.githubapi.pojo.*;
import com.google.api.client.http.UriTemplate;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
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
import java.util.List;
import java.util.Map;

public class ReleaseApi {

    private final String CREATE_URL = "/repos/{owner}/{repo}/releases";
    private final String GET_RELEASE_BY_TAG_URL = "/repos/{owner}/{repo}/releases/tags/{tag}";
    private final String EDIT_RELEASE_URL = "/repos/{owner}/{repo}/releases/{release_id}";
    private final String BASE_URL = "https://api.github.com";
    private final String token;
    private final ExceptionManager exceptionManager;

    public ReleaseApi(String token) {
        this.token = token;
        exceptionManager = new ExceptionManager();
        exceptionManager.add(new ReleaseTagExistsException());
        exceptionManager.add(new AssetAlreadyExistsException());
    }


    public GetReleaseByTagNameResponse getReleaseByTag(String owner, String repo, String tag) throws IOException, GithubApiException {
        RestTemplate r = new RestTemplate();
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("owner", owner);
        urlParams.put("repo", repo);
        urlParams.put("tag", tag);
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("token " + token));

        HttpEntity h = new HttpEntity(headers);

        try {
            ResponseEntity<GetReleaseByTagNameResponse> responseEntity = r.exchange(BASE_URL + GET_RELEASE_BY_TAG_URL, HttpMethod.GET, h, GetReleaseByTagNameResponse.class, urlParams);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            if (e.getRawStatusCode() == 422) {
                throw exceptionManager.findOrThrow(e.getResponseBodyAsString());
            }
            throw e;
        }

    }

    public EditReleaseResponse editRelease(String owner, String repo, Integer releaseId, EditReleaseRequest editReleaseRequest) throws IOException, GithubApiException {

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        RestTemplate r = new RestTemplate(requestFactory);

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("owner", owner);
        urlParams.put("repo", repo);
        urlParams.put("release_id", releaseId.toString());
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("token " + token));

        HttpEntity<EditReleaseRequest> h = new HttpEntity<>(editReleaseRequest, headers);

        try {
            ResponseEntity<EditReleaseResponse> responseEntity = r.exchange(BASE_URL + EDIT_RELEASE_URL, HttpMethod.PATCH, h, EditReleaseResponse.class, urlParams);

            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            if (e.getRawStatusCode() == 422) {
                throw exceptionManager.findOrThrow(e.getResponseBodyAsString());
            }
            throw e;
        }
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

    public UploadAssetResponse uploadAssetResponse(String uploadUrl, File asset, String label) throws IOException, GithubApiException {
        String mimeType = Files.probeContentType(asset.toPath());
        byte[] assetBytes = Files.readAllBytes(asset.toPath());

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
