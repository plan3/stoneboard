package planning;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

@TestConfiguration
public class GithubMocks {

  public static GitHub client = mock(GitHub.class);
  public static GHRepository repository = mock(GHRepository.class);

  @Bean
  public GithubClientFactory factory() throws IOException {
    final GithubClientFactory mock = mock(GithubClientFactory.class);
    when(mock.create(any(OAuth2AuthorizedClientService.class), anyString())).thenReturn(client);
    when(client.getRepository(anyString())).thenReturn(repository);
    return mock;
  }
}
