package stoneboard;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {
  public static final String ENV_GITHUB_CLIENT_ID = "GITHUB_CLIENT_ID";
  public static final String ENV_GITHUB_CLIENT_SECRET = "GITHUB_CLIENT_SECRET";
  public static final String ENV_REPOSITORIES = "REPOSITORIES";
  public static final String ENV_GITHUB_HOSTNAME = "GITHUB_HOSTNAME";
  public static final String GITHUB_COM_HOST = "github.com";

  @Value("${" + ENV_REPOSITORIES + '}')
  private String repositories;

  @Value("${" + ENV_GITHUB_HOSTNAME + ":" + GITHUB_COM_HOST + '}')
  private String hostname;

  public String hostname() {
    return requireNonNull(this.hostname).trim().isEmpty() ? GITHUB_COM_HOST : this.hostname;
  }

  public List<String> repositories() {
    return asList(requireNonNull(this.repositories).split(","));
  }
}
