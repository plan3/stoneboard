package planning;

import static java.util.Arrays.asList;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static planning.ServiceConfig.ENV_GITHUB_CLIENT_ID;
import static planning.ServiceConfig.ENV_GITHUB_CLIENT_SECRET;
import static planning.ServiceConfig.ENV_GITHUB_HOSTNAME;
import static planning.ServiceConfig.ENV_REPOSITORIES;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest
@Import({ServiceConfig.class, GithubMocks.class})
@ActiveProfiles("githubCom")
@TestPropertySource(
    properties = {
      ENV_GITHUB_CLIENT_ID + "such-id",
      ENV_GITHUB_CLIENT_SECRET + "much-secret",
      ENV_REPOSITORIES + "foo/bar",
      ENV_GITHUB_HOSTNAME + "does.not.exist.com"
    })
public abstract class AbstractOAuthTest {
  protected MockMvc mvc;

  @MockBean protected OAuth2AuthorizedClientService service;

  @Autowired WebApplicationContext context;
  protected MockHttpSession session = new MockHttpSession();

  @BeforeEach
  public void setup() {
    this.mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    this.session.setAttribute(
        SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(buildPrincipal()));
  }

  private static OAuth2AuthenticationToken buildPrincipal() {
    final Map<String, Object> attributes = new HashMap<>();
    attributes.put("login", "foo");
    attributes.put("avatar_url", "https://some.non-existant/image.jpg");
    final List<GrantedAuthority> authorities = asList(new OAuth2UserAuthority(attributes));
    final OAuth2User user = new DefaultOAuth2User(authorities, attributes, "login");
    return new OAuth2AuthenticationToken(user, authorities, "whatever");
  }
}
