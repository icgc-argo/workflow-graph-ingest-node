/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc_argo.workflowingestionnode.config;

import lombok.val;
import org.icgc_argo.workflowingestionnode.components.GqlAnalysesFetcher;
import org.icgc_argo.workflowingestionnode.components.GraphEventConverterSupplier;
import org.icgc_argo.workflowingestionnode.components.MessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppBeanConfig {

  @Profile("!oauth")
  @Bean
  public GqlAnalysesFetcher defaultGqlAnalysesFetcher(@Value("${rdpc.url}") String rdpcUrl) {
    return new GqlAnalysesFetcher(rdpcUrl, new RestTemplate());
  }

  @Profile("oauth")
  @Bean
  public GqlAnalysesFetcher oauthGqlAnalysesFetcher(
      @Value("${rdpc.oauth.clientId}") String clientId,
      @Value("${rdpc.oauth.clientSecret}") String clientSecret,
      @Value("${rdpc.oauth.tokenUrl}") String tokenUrl,
      @Value("${rdpc.url}") String rdpcUrl) {

    val clientCredentialResource = new ClientCredentialsResourceDetails();
    clientCredentialResource.setClientAuthenticationScheme(AuthenticationScheme.query);
    clientCredentialResource.setAccessTokenUri(tokenUrl);
    clientCredentialResource.setClientId(clientId);
    clientCredentialResource.setClientSecret(clientSecret);
    clientCredentialResource.setGrantType("client_credentials");

    val atr = new DefaultAccessTokenRequest();
    val ctx = new DefaultOAuth2ClientContext(atr);
    val oauth2RestTemplate = new OAuth2RestTemplate(clientCredentialResource, ctx);
    oauth2RestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    return new GqlAnalysesFetcher(rdpcUrl, oauth2RestTemplate);
  }

  @Bean
  public MessageProcessor processor(@Autowired GqlAnalysesFetcher analysesFetcher) {
    return new MessageProcessor(analysesFetcher);
  }

  @Bean
  public MessageConverter graphEventConverter() {
    return new GraphEventConverterSupplier().get();
  }
}
