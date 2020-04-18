package planning;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GithubResult;
import org.springframework.http.MediaType;

public class ApiTest extends AbstractOAuthTest {

  @Test
  public void root() throws Exception {
    this.mvc
        .perform(get("/").session(session).accept(TEXT_HTML))
        .andExpect(status().isOk())
        .andExpect(content().contentType(new MediaType(TEXT_HTML, UTF_8)));
  }

  @Test
  public void milestones() throws Exception {
    when(GithubMocks.repository.listMilestones(any(GHIssueState.class)))
        .thenReturn(new GithubResult<>(emptyList()));
    this.mvc
        .perform(get("/milestones/foo/bar").session(session).accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }
}
