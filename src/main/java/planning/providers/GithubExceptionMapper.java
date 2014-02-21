package planning.providers;

import planning.resources.OAuth;
import java.net.URI;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import org.eclipse.egit.github.core.client.RequestException;

public class GithubExceptionMapper implements ExceptionMapper<RequestException> {
    @Override
    public Response toResponse(RequestException e) {
        if (e.getStatus() == 401) {
            final URI login = UriBuilder.fromResource(OAuth.class).build();
            return Response.temporaryRedirect(login).build();
        }
        throw new WebApplicationException(Response.serverError().build());
    }
}
