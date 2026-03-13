package com.quark.autosave.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.quark.QuarkFileItem;
import com.quark.autosave.model.quark.ShareParseResult;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DefaultQuarkClient implements QuarkClient {

    private static final String BASE_URL = "https://drive-pc.quark.cn";
    private static final String BASE_URL_APP = "https://drive-m.quark.cn";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        + "AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/3.14.2 "
        + "Chrome/112.0.5615.165 Electron/24.1.3.8 Safari/537.36 Channel/pckk_other_ch";

    private static final Pattern KPS_PATTERN = Pattern.compile("(?<!\\w)kps=([a-zA-Z0-9%+/=]+)[;&]?");
    private static final Pattern SIGN_PATTERN = Pattern.compile("(?<!\\w)sign=([a-zA-Z0-9%+/=]+)[;&]?");
    private static final Pattern VCODE_PATTERN = Pattern.compile("(?<!\\w)vcode=([a-zA-Z0-9%+/=]+)[;&]?");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DefaultQuarkClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
            .requestInterceptor((request, body, execution) -> {
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                request.getHeaders().set("User-Agent", USER_AGENT);
                return execution.execute(request, body);
            })
            .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getShareToken(AccountConfig accountConfig, ShareParseResult shareParseResult) {
        JsonNode response = request(accountConfig.getCookie(), HttpMethod.POST,
            BASE_URL + "/1/clouddrive/share/sharepage/token",
            baseShareParams(),
            Map.of("pwd_id", shareParseResult.getPwdId(), "passcode", shareParseResult.getPasscode()));
        return requiredData(response, "/data/stoken").asText();
    }

    @Override
    public List<QuarkFileItem> listShareFiles(AccountConfig accountConfig, ShareParseResult shareParseResult, String stoken) {
        List<QuarkFileItem> result = new ArrayList<>();
        int page = 1;
        while (true) {
            Map<String, Object> params = new LinkedHashMap<>(baseShareParams());
            params.put("pwd_id", shareParseResult.getPwdId());
            params.put("stoken", stoken);
            params.put("pdir_fid", shareParseResult.getPdirFid());
            params.put("force", "0");
            params.put("_page", page);
            params.put("_size", "50");
            params.put("_fetch_banner", "0");
            params.put("_fetch_share", "0");
            params.put("_fetch_total", "1");
            params.put("_sort", "file_type:asc,updated_at:desc");
            params.put("ver", "2");
            params.put("fetch_share_full_path", "0");

            JsonNode response = request(accountConfig.getCookie(), HttpMethod.GET,
                BASE_URL + "/1/clouddrive/share/sharepage/detail",
                params,
                null);
            JsonNode listNode = requiredData(response, "/data/list");
            if (!listNode.isArray() || listNode.isEmpty()) {
                break;
            }
            for (JsonNode item : listNode) {
                result.add(toFileItem(item));
            }
            if (listNode.size() < 50) {
                break;
            }
            page++;
        }
        if (shareParseResult.getPdirFid().equals("0") && result.size() == 1 && result.get(0).isDir()) {
            ShareParseResult subDirectoryResult = new ShareParseResult();
            subDirectoryResult.setPwdId(shareParseResult.getPwdId());
            subDirectoryResult.setPasscode(shareParseResult.getPasscode());
            subDirectoryResult.setPdirFid(result.get(0).getFid());
            return listShareFiles(accountConfig, subDirectoryResult, stoken);
        }
        return result;
    }

    @Override
    public String ensureDirectory(AccountConfig accountConfig, String savePath) {
        JsonNode pathNode = getPathInfo(accountConfig, savePath);
        if (pathNode != null) {
            return pathNode.path("fid").asText();
        }
        JsonNode response = request(accountConfig.getCookie(), HttpMethod.POST,
            BASE_URL + "/1/clouddrive/file",
            baseDriveParams(),
            Map.of(
                "pdir_fid", "0",
                "file_name", "",
                "dir_path", savePath,
                "dir_init_lock", false
            ));
        return requiredData(response, "/data/fid").asText();
    }

    @Override
    public List<String> listTargetFileNames(AccountConfig accountConfig, String directoryFid) {
        List<String> result = new ArrayList<>();
        int page = 1;
        while (true) {
            Map<String, Object> params = new LinkedHashMap<>(baseDriveParams());
            params.put("pdir_fid", directoryFid);
            params.put("_page", page);
            params.put("_size", "50");
            params.put("_fetch_total", "1");
            params.put("_fetch_sub_dirs", "0");
            params.put("_sort", "file_type:asc,updated_at:desc");
            params.put("_fetch_full_path", "0");
            params.put("fetch_all_file", "1");
            params.put("fetch_risk_file_name", "1");
            JsonNode response = request(accountConfig.getCookie(), HttpMethod.GET,
                BASE_URL + "/1/clouddrive/file/sort",
                params,
                null);
            JsonNode listNode = requiredData(response, "/data/list");
            if (!listNode.isArray() || listNode.isEmpty()) {
                break;
            }
            for (JsonNode item : listNode) {
                result.add(item.path("file_name").asText());
            }
            if (listNode.size() < 50) {
                break;
            }
            page++;
        }
        return result;
    }

    @Override
    public List<String> saveFiles(AccountConfig accountConfig, ShareParseResult shareParseResult, String stoken,
                                  String directoryFid, List<QuarkFileItem> fileItems) {
        List<String> savedIds = new ArrayList<>();
        List<String> fidList = fileItems.stream().map(QuarkFileItem::getFid).toList();
        List<String> fidTokenList = fileItems.stream().map(QuarkFileItem::getShareFidToken).toList();

        JsonNode response = request(accountConfig.getCookie(), HttpMethod.POST,
            BASE_URL + "/1/clouddrive/share/sharepage/save",
            saveParams(),
            Map.of(
                "fid_list", fidList,
                "fid_token_list", fidTokenList,
                "to_pdir_fid", directoryFid,
                "pwd_id", shareParseResult.getPwdId(),
                "stoken", stoken,
                "pdir_fid", "0",
                "scene", "link"
            ));
        String taskId = requiredData(response, "/data/task_id").asText();
        JsonNode taskResponse = queryTask(accountConfig, taskId);
        JsonNode saveIdsNode = requiredData(taskResponse, "/data/save_as/save_as_top_fids");
        saveIdsNode.forEach(item -> savedIds.add(item.asText()));
        return savedIds;
    }

    @Override
    public void renameFile(AccountConfig accountConfig, String fileId, String targetFileName) {
        request(accountConfig.getCookie(), HttpMethod.POST,
            BASE_URL + "/1/clouddrive/file/rename",
            baseDriveParams(),
            Map.of("fid", fileId, "file_name", targetFileName));
    }

    private JsonNode queryTask(AccountConfig accountConfig, String taskId) {
        for (int retryIndex = 0; retryIndex < 60; retryIndex++) {
            Map<String, Object> params = new LinkedHashMap<>(baseDriveParams());
            params.put("task_id", taskId);
            params.put("retry_index", retryIndex);
            params.put("__dt", ThreadLocalRandom.current().nextInt(60_000, 300_001));
            params.put("__t", System.currentTimeMillis());
            JsonNode response = request(accountConfig.getCookie(), HttpMethod.GET,
                BASE_URL + "/1/clouddrive/task",
                params,
                null);
            if (requiredData(response, "/data/status").asInt() == 2) {
                return response;
            }
            sleepSilently(Duration.ofMillis(500));
        }
        throw new IllegalStateException("查询夸克转存任务超时");
    }

    private JsonNode getPathInfo(AccountConfig accountConfig, String savePath) {
        JsonNode response = request(accountConfig.getCookie(), HttpMethod.POST,
            BASE_URL + "/1/clouddrive/file/info/path_list",
            baseShareParams(),
            Map.of("file_path", List.of(savePath), "namespace", "0"));
        JsonNode dataNode = requiredData(response, "/data");
        if (!dataNode.isArray() || dataNode.isEmpty()) {
            return null;
        }
        return dataNode.get(0);
    }

    private QuarkFileItem toFileItem(JsonNode jsonNode) {
        QuarkFileItem fileItem = new QuarkFileItem();
        fileItem.setFid(jsonNode.path("fid").asText());
        fileItem.setShareFidToken(jsonNode.path("share_fid_token").asText());
        fileItem.setFileName(jsonNode.path("file_name").asText());
        fileItem.setDir(jsonNode.path("dir").asBoolean(false));
        return fileItem;
    }

    private JsonNode request(String cookie, HttpMethod method, String url, Map<String, Object> params, Object body) {
        try {
            RequestContext requestContext = buildRequestContext(cookie, url, params);
            String responseText = restClient.method(method)
                .uri(requestContext.uri())
                .headers(headers -> requestContext.headers().forEach(headers::set))
                .body(body == null ? "" : body)
                .retrieve()
                .body(String.class);
            JsonNode jsonNode = objectMapper.readTree(responseText == null ? "{}" : responseText);
            if (jsonNode.path("code").asInt(0) != 0 && jsonNode.path("status").asInt(200) != 200) {
                throw new IllegalStateException(jsonNode.path("message").asText("夸克接口返回异常"));
            }
            return jsonNode;
        } catch (Exception exception) {
            throw new IllegalStateException("请求夸克接口失败: " + url, exception);
        }
    }

    private RequestContext buildRequestContext(String cookie, String url, Map<String, Object> params) {
        Map<String, String> headers = new HashMap<>();
        headers.put("cookie", cookie);

        Map<String, Object> requestParams = new LinkedHashMap<>(params);
        Map<String, String> mparam = extractMobileParams(cookie);
        String requestUrl = url;
        if (!CollectionUtils.isEmpty(mparam) && url.contains("/share/")) {
            requestUrl = url.replace(BASE_URL, BASE_URL_APP);
            headers.remove("cookie");
            requestParams.put("device_model", "M2011K2C");
            requestParams.put("entry", "default_clouddrive");
            requestParams.put("_t_group", "0%3A_s_vp%3A1");
            requestParams.put("dmn", "Mi%2B11");
            requestParams.put("fr", "android");
            requestParams.put("pf", "3300");
            requestParams.put("bi", "35937");
            requestParams.put("ve", "7.4.5.680");
            requestParams.put("ss", "411x875");
            requestParams.put("mi", "M2011K2C");
            requestParams.put("nt", "5");
            requestParams.put("nw", "0");
            requestParams.put("kt", "4");
            requestParams.put("pr", "ucpro");
            requestParams.put("sv", "release");
            requestParams.put("dt", "phone");
            requestParams.put("data_from", "ucapi");
            requestParams.put("app", "clouddrive");
            requestParams.put("kkkk", "1");
            requestParams.putAll(mparam);
        }
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(requestUrl);
        requestParams.forEach(uriBuilder::queryParam);
        URI uri = uriBuilder.build(false).encode(StandardCharsets.UTF_8).toUri();
        return new RequestContext(uri, headers);
    }

    private JsonNode requiredData(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalStateException("夸克接口返回缺少字段: " + pointer);
        }
        return node;
    }

    private Map<String, Object> baseShareParams() {
        return new LinkedHashMap<>(Map.of("pr", "ucpro", "fr", "pc"));
    }

    private Map<String, Object> baseDriveParams() {
        return new LinkedHashMap<>(Map.of("pr", "ucpro", "fr", "pc", "uc_param_str", ""));
    }

    private Map<String, Object> saveParams() {
        Map<String, Object> params = baseDriveParams();
        params.put("app", "clouddrive");
        params.put("__dt", ThreadLocalRandom.current().nextInt(60_000, 300_001));
        params.put("__t", System.currentTimeMillis());
        return params;
    }

    private Map<String, String> extractMobileParams(String cookie) {
        String kps = match(cookie, KPS_PATTERN);
        String sign = match(cookie, SIGN_PATTERN);
        String vcode = match(cookie, VCODE_PATTERN);
        if (kps == null || sign == null || vcode == null) {
            return Map.of();
        }
        return Map.of(
            "kps", kps.replace("%25", "%"),
            "sign", sign.replace("%25", "%"),
            "vcode", vcode.replace("%25", "%")
        );
    }

    private String match(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void sleepSilently(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待夸克任务结果时线程被中断", interruptedException);
        }
    }

    private record RequestContext(URI uri, Map<String, String> headers) {
    }
}
