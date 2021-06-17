package fr.redfroggy.keycloak.invitation.requiredactions;


import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class WebhookRegistrationSuccessTest {

    @Mock
    private RealmModel realm;

    @Mock
    AuthenticatorConfigModel config;

    @Mock
    private UserModel user;

    @Mock
    private KeycloakSession session;

    @Mock
    private HttpClientProvider httpClientProvider;

    @Mock
    private HttpClient httpClient;

    @Mock
    private EventBuilder event;

    @Mock
    private AuthenticationSessionModel authSession;

    @Mock
    private LoginFormsProvider form;

    @Mock
    private EmailTemplateProvider templateProvider;

    @Mock
    private Response response;

    @Mock
    private RequiredActionContext requiredActionContext;

    private WebhookRegistrationSuccess action = new WebhookRegistrationSuccess();

    @Test
    public void shouldReturnThisWhenCreate() {
        assertThat(action.create(session)).isEqualTo(action);
    }

    @Test
    public void shouldReturnGetId() {
        assertThat(action.getId()).isEqualTo("WEBHOOK_REGISTRATION_SUCCESS");
    }

    @Test
    public void shouldReturnSuccessOnChallengeWhenRegistrationReponseIsOk() throws IOException {
        mockChallenge(Maps.newHashMap("invitation.id", Arrays.asList("my-invitation.id")));
        when(httpClient.execute(any())).thenReturn(
                new BasicHttpResponse(new ProtocolVersion("http", 1, 0), 201, "reason is 201"));

        action.requiredActionChallenge(requiredActionContext);

        verify(requiredActionContext).success();
    }

    @Test
    public void shouldChallengeErrorWhenResponseIs400() throws IOException {
        mockChallenge(null);
        when(httpClient.execute(any())).thenReturn(
                new BasicHttpResponse(new ProtocolVersion("http", 1, 0), 400, "reason is 400"));

        when(requiredActionContext.getEvent()).thenReturn(event);
        when(event.clone()).thenReturn(event);
        when(event.event(EventType.EXECUTE_ACTIONS_ERROR)).thenReturn(event);
        when(event.detail(any(), any())).thenReturn(event);

        action.requiredActionChallenge(requiredActionContext);

        verify(form).setError("WebhookRegistrationSuccessError400", "reason is 400");
        verify(event).detail(Details.EMAIL, "keycloak@redfroggy.fr");
        verify(event).detail(Details.ACTION, WebhookRegistrationSuccess.WEBHOOK_REGISTRATION_SUCCESS);
    }

    @Test
    public void shouldChallengeErrorWhenUnexceptedException() throws IOException {
        mockChallenge(null);
        when(requiredActionContext.getEvent()).thenReturn(event);
        when(event.clone()).thenReturn(event);
        when(event.event(EventType.EXECUTE_ACTIONS_ERROR)).thenReturn(event);
        when(event.detail(any(), any())).thenReturn(event);

        doThrow(new IOException("unexcepted exception"))
                .when(httpClient).execute(any());

        action.requiredActionChallenge(requiredActionContext);

        verify(form).setError("WebhookRegistrationSuccessError", "unexcepted exception");
        verify(event).detail(Details.EMAIL, "keycloak@redfroggy.fr");
        verify(event).detail(Details.ACTION, WebhookRegistrationSuccess.WEBHOOK_REGISTRATION_SUCCESS);
    }

    private void mockChallenge(Map<String, List<String>> userAttributes) throws IOException {
        when(requiredActionContext.form()).thenReturn(form);
        when(requiredActionContext.getRealm()).thenReturn(realm);
        when(realm.getAuthenticatorConfigByAlias("invitation")).thenReturn(config);
        when(config.getConfig()).thenReturn(Maps.newHashMap("registration-success-url", "http://registration"));
        when(requiredActionContext.getSession()).thenReturn(session);
        when(session.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
        when(httpClientProvider.getHttpClient()).thenReturn(httpClient);
        when(requiredActionContext.getUser()).thenReturn(user);
        when(user.getEmail()).thenReturn("keycloak@redfroggy.fr");
        when(user.getAttributes()).thenReturn(userAttributes);
    }
}