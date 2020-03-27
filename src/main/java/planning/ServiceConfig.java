package planning;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@Configuration
public class ServiceConfig {
  @Value("${REPOSITORIES}")
  private String repositories;

  @Value("${GITHUB_HOSTNAME}")
  private String hostname;

  public String hostname() {
    return requireNonNull(this.hostname);
  }

  public List<String> repositories() {
    return asList(requireNonNull(this.repositories).split(","));
  }
}
