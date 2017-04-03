package mw.gov.health.lmis.upload;

import static mw.gov.health.lmis.upload.RequestHelper.createUri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import mw.gov.health.lmis.Configuration;

import java.util.Map;

public abstract class BaseCommunicationService {
  private static final String ACCESS_TOKEN = "access_token";

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected RestOperations restTemplate = new RestTemplate();

  @Autowired
  protected AuthService authService;

  @Autowired
  protected Configuration configuration;

  protected abstract String getUrl();

  /**
   * Return one object from service.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return one reference data T objects.
   */
  public Map findOne(String resourceUrl, RequestParameters parameters) {
    String url = configuration.getHost() + getUrl() + resourceUrl;

    RequestParameters params = RequestParameters
        .init()
        .setAll(parameters)
        .set(ACCESS_TOKEN, authService.obtainAccessToken());

    try {
      return restTemplate
          .getForEntity(createUri(url, params), Map.class)
          .getBody();
    } catch (HttpStatusCodeException ex) {
      // rest template will handle 404 as an exception, instead of returning null
      if (HttpStatus.NOT_FOUND == ex.getStatusCode()) {
        logger.warn(
            "{} matching params does not exist. Params: {}",
            Map.class.getSimpleName(), parameters
        );

        return null;
      }

      throw buildDataRetrievalException(ex);
    }
  }

  /**
   * Attempts to create a new resource in OpenLMIS.
   *
   * @param json JSON representation of the resource to create
   * @return whether the attempt was successful
   */
  public boolean createResource(String json) {
    String url = configuration.getHost() + getUrl();

    RequestParameters parameters = RequestParameters
        .init()
        .set(ACCESS_TOKEN, authService.obtainAccessToken());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> body = new HttpEntity<>(json, headers);

    try {
      restTemplate.postForEntity(createUri(url, parameters), body, Object.class);
    } catch (RestClientException ex) {
      logger.error("Can not create resource ", ex);
      return false;
    }
    return true;
  }

  private DataRetrievalException buildDataRetrievalException(HttpStatusCodeException ex) {
    return new DataRetrievalException(Map.class.getSimpleName(),
        ex.getStatusCode(),
        ex.getResponseBodyAsString());
  }
}