package org.apache.seatunnel.admin.thirdparty.client;

import org.apache.seatunnel.admin.thirdparty.exceptions.SeatunnelClientException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A lightweight REST client for interacting with the SeaTunnel engine REST API.
 *
 * <p>This client wraps {@link RestTemplate} and provides:
 * <ul>
 *   <li>Convenient methods for common engine endpoints</li>
 *   <li>Simple URL construction based on a configured base API URL</li>
 *   <li>Unified exception wrapping into {@link SeatunnelClientException}</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This class currently returns raw {@link Map}/{@link List}
 * for flexibility. Consider introducing typed DTOs in the future for better safety.</p>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Service
public class SeatunnelRestClient {

    private final RestTemplate restTemplate;
    private final String baseApiUrl;

    /**
     * @param restTemplate Spring RestTemplate instance
     * @param baseApiUrl base URL for the engine API, e.g. http://host:port
     */
    public SeatunnelRestClient(RestTemplate restTemplate, String baseApiUrl) {
        this.restTemplate = restTemplate;
        this.baseApiUrl = baseApiUrl;
    }

    /**
     * Builds a full URL by joining {@code baseApiUrl} and an endpoint path.
     *
     * @param path endpoint path such as "/overview" or "overview"
     * @return full URL
     */
    private String url(String path) {
        if (path == null || path.isEmpty()) return baseApiUrl;
        if (!path.startsWith("/")) path = "/" + path;
        return baseApiUrl + path;
    }

    /**
     * Wraps exceptions thrown by RestTemplate into a {@link SeatunnelClientException}.
     * If the exception contains an HTTP status + response body, they will be included.
     *
     * @param e original exception
     * @param hint human-readable hint (endpoint + operation)
     * @return runtime exception to throw
     */
    private RuntimeException wrap(Exception e, String hint) {
        if (e instanceof HttpStatusCodeException) {
            HttpStatusCodeException he = (HttpStatusCodeException) e;
            return new SeatunnelClientException(
                    hint,
                    he.getRawStatusCode(),
                    safe(he.getResponseBodyAsString()),
                    he
            );
        }
        return new SeatunnelClientException(hint, -1, "", e);
    }

    /** Null-safe string helper. */
    private String safe(String s) { return s == null ? "" : s; }

    /** Default JSON headers used for JSON APIs. */
    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return h;
    }

    /**
     * Text payload headers.
     *
     * <p>Used for endpoints where request body is plain text (e.g. SQL/HOCON/JSON text),
     * but server response is JSON.</p>
     */
    private HttpHeaders textHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.TEXT_PLAIN);
        h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return h;
    }

    /* ===================== GET ===================== */

    /**
     * GET /overview?tag1=v1&tag2=v2
     *
     * @param tags optional tags as query parameters; may be null/empty
     * @return overview response as a map
     */
    public Map overview(Map<String, String> tags) {
        try {
            StringBuilder sb = new StringBuilder(url("/overview"));
            if (tags != null && !tags.isEmpty()) {
                sb.append("?");
                boolean first = true;
                for (Map.Entry<String, String> e : tags.entrySet()) {
                    if (!first) sb.append("&");
                    first = false;
                    sb.append(e.getKey()).append("=").append(e.getValue());
                }
            }
            return restTemplate.getForObject(sb.toString(), Map.class);
        } catch (Exception e) {
            throw wrap(e, "GET /overview failed");
        }
    }

    /** GET /running-jobs */
    public List runningJobs() {
        try {
            return restTemplate.getForObject(url("/running-jobs"), List.class);
        } catch (Exception e) {
            throw wrap(e, "GET /running-jobs failed");
        }
    }

    /** GET /job-info/{jobId} */
    public Map jobInfo(long jobId) {
        try {
            return restTemplate.getForObject(url("/job-info/" + jobId), Map.class);
        } catch (Exception e) {
            throw wrap(e, "GET /job-info/{jobId} failed");
        }
    }

    /**
     * GET /finished-jobs/{state}
     *
     * @param state job state string; if blank, a fallback value will be used
     */
    public List finishedJobs(String state) {
        try {
            if (state == null || state.trim().isEmpty()) state = "UNKNOWABLE";
            return restTemplate.getForObject(url("/finished-jobs/" + state), List.class);
        } catch (Exception e) {
            throw wrap(e, "GET /finished-jobs/{state} failed");
        }
    }

    /** GET /system-monitoring-information */
    public List systemMonitoringInformation() {
        try {
            return restTemplate.getForObject(url("/system-monitoring-information"), List.class);
        } catch (Exception e) {
            throw wrap(e, "GET /system-monitoring-information failed");
        }
    }

    /**
     * GET /logs or /logs/{jobId}
     *
     * <p>Supports optional query param: ?format=json</p>
     *
     * @param jobIdOrNull when null, fetches /logs; otherwise /logs/{jobId}
     * @param formatOrNull optional format query param
     * @return raw response (could be JSON or text depending on server)
     */
    public Object logs(Long jobIdOrNull, String formatOrNull) {
        try {
            String path = (jobIdOrNull == null) ? "/logs" : ("/logs/" + jobIdOrNull);
            String full = url(path);
            if (formatOrNull != null && !formatOrNull.trim().isEmpty()) {
                full = full + "?format=" + formatOrNull;
            }
            return restTemplate.getForObject(full, Object.class);
        } catch (Exception e) {
            throw wrap(e, "GET /logs failed");
        }
    }

    /** GET /log (single-node log list) */
    public Object nodeLogs() {
        try {
            return restTemplate.getForObject(url("/log"), Object.class);
        } catch (Exception e) {
            throw wrap(e, "GET /log failed");
        }
    }

    /**
     * GET /metrics or /openmetrics
     *
     * <p>These endpoints usually return plain text. We use exchange()
     * to retrieve the raw string body.</p>
     *
     * @param openMetrics whether to call /openmetrics instead of /metrics
     * @return metrics text body
     */
    public String metrics(boolean openMetrics) {
        try {
            String path = openMetrics ? "/openmetrics" : "/metrics";
            ResponseEntity<String> resp = restTemplate.exchange(
                    url(path),
                    HttpMethod.GET,
                    new HttpEntity<Void>((Void) null, new HttpHeaders()),
                    String.class
            );
            return resp.getBody();
        } catch (Exception e) {
            throw wrap(e, "GET /metrics failed");
        }
    }

    /* ===================== POST ===================== */

    /**
     * POST /submit-job?format=json|hocon|sql&jobId=..&jobName=..&isStartWithSavePoint=..
     *
     * <p>Request body is plain text (config content).</p>
     */
    public Map submitJobText(String configText, String format, String jobId, String jobName, Boolean isStartWithSavePoint) {
        try {
            if (format == null || format.trim().isEmpty()) format = "json";
            StringBuilder sb = new StringBuilder(url("/submit-job"));
            sb.append("?format=").append(format);

            if (jobId != null && !jobId.trim().isEmpty()) sb.append("&jobId=").append(jobId);
            if (jobName != null && !jobName.trim().isEmpty()) sb.append("&jobName=").append(jobName);
            if (isStartWithSavePoint != null) sb.append("&isStartWithSavePoint=").append(isStartWithSavePoint);

            HttpEntity<String> entity = new HttpEntity<>(configText == null ? "" : configText, textHeaders());
            return restTemplate.postForObject(sb.toString(), entity, Map.class);
        } catch (Exception e) {
            throw wrap(e, "POST /submit-job failed");
        }
    }

    /** POST /submit-job with JSON object body */
    public Map submitJobJson(Object configJsonObject, String jobId, String jobName, Boolean isStartWithSavePoint) {
        try {
            StringBuilder sb = new StringBuilder(url("/submit-job"));
            sb.append("?format=json");
            if (jobId != null && !jobId.trim().isEmpty()) sb.append("&jobId=").append(jobId);
            if (jobName != null && !jobName.trim().isEmpty()) sb.append("&jobName=").append(jobName);
            if (isStartWithSavePoint != null) sb.append("&isStartWithSavePoint=").append(isStartWithSavePoint);

            HttpEntity<Object> entity = new HttpEntity<>(configJsonObject, jsonHeaders());
            return restTemplate.postForObject(sb.toString(), entity, Map.class);
        } catch (Exception e) {
            throw wrap(e, "POST /submit-job(json) failed");
        }
    }

    /**
     * POST /submit-job/upload (multipart)
     *
     * <p>Form field name is "config_file".</p>
     */
    public Map submitJobUpload(byte[] fileBytes, String filename) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Wrap bytes as a Resource so RestTemplate can send it as multipart file
            ByteArrayResource resource = new ByteArrayResource(fileBytes == null ? new byte[0] : fileBytes) {
                @Override
                public String getFilename() {
                    return filename == null ? "job.conf" : filename;
                }
            };

            body.add("config_file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.exchange(
                    url("/submit-job/upload"),
                    HttpMethod.POST,
                    req,
                    Map.class
            );
            return resp.getBody();
        } catch (Exception e) {
            throw wrap(e, "POST /submit-job/upload failed");
        }
    }

    /**
     * POST /submit-jobs (batch submit)
     *
     * <p>Request body is a JSON array of job configs.</p>
     */
    public List submitJobsBatch(List jobConfigs) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(jobConfigs, jsonHeaders());
            ResponseEntity<List> resp = restTemplate.exchange(url("/submit-jobs"), HttpMethod.POST, entity, List.class);
            return resp.getBody();
        } catch (Exception e) {
            throw wrap(e, "POST /submit-jobs failed");
        }
    }

    /**
     * POST /stop-job
     *
     * @param jobId engine job id
     * @param isStopWithSavePoint whether to stop with savepoint
     */
    public Map stopJob(long jobId, boolean isStopWithSavePoint) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("jobId", jobId);
            body.put("isStopWithSavePoint", isStopWithSavePoint);

            HttpEntity<Object> entity = new HttpEntity<>(body, jsonHeaders());
            return restTemplate.postForObject(url("/stop-job"), entity, Map.class);
        } catch (Exception e) {
            throw wrap(e, "POST /stop-job failed");
        }
    }

    /** POST /stop-jobs (batch stop) */
    public List stopJobsBatch(List<Map<String, Object>> items) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(items, jsonHeaders());
            ResponseEntity<List> resp = restTemplate.exchange(url("/stop-jobs"), HttpMethod.POST, entity, List.class);
            return resp.getBody();
        } catch (Exception e) {
            throw wrap(e, "POST /stop-jobs failed");
        }
    }

    /** POST /encrypt-config */
    public Map encryptConfig(Object config) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(config, jsonHeaders());
            return restTemplate.postForObject(url("/encrypt-config"), entity, Map.class);
        } catch (Exception e) {
            throw wrap(e, "POST /encrypt-config failed");
        }
    }

    /**
     * POST /update-tags
     *
     * <p>Request body is a JSON map; an empty map indicates clearing tags.</p>
     */
    public Map updateTags(Map<String, String> tags) {
        try {
            Map<String, String> body = (tags == null) ? Collections.emptyMap() : tags;
            HttpEntity<Object> entity = new HttpEntity<>(body, jsonHeaders());
            return restTemplate.postForObject(url("/update-tags"), entity, Map.class);
        } catch (Exception e) {
            throw wrap(e, "POST /update-tags failed");
        }
    }

    /* ===================== Convenience ===================== */

    /** Submits a SQL job (format=sql). */
    public Map submitJobSql(String sql, String jobName) {
        return submitJobText(sql, "sql", null, jobName, null);
    }

    /** Submits a HOCON job (format=hocon). */
    public Map submitJobHocon(String hocon, String jobName) {
        return submitJobText(hocon, "hocon", null, jobName, null);
    }

    /** Submits a JSON text job (format=json). */
    public Map submitJobJsonText(String json, String jobName) {
        return submitJobText(json, "json", null, jobName, null);
    }

    /** Submits a job by uploading a config file generated from plain text. */
    public Map submitJobUploadText(String text, String filename) {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        return submitJobUpload(bytes, filename);
    }
}