/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.SubscriptionServiceImpl;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceTest {

    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String APPLICATION_ID = "my-application";
    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";
    private static final String PAGE_ID = "my-page-gcu";
    private static final String USER_ID = "user";
    private static final String SUBSCRIBER_ID = "subscriber";

    @InjectMocks
    private SubscriptionService subscriptionService = new SubscriptionServiceImpl();

    @Mock
    private PlanService planService;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private ApiService apiService;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private Subscription subscription;
    @Mock
    private PlanEntity plan;
    @Mock
    private ApplicationEntity application;
    @Mock
    private ApiEntity apiEntity;
    @Mock
    private ApiModelEntity apiModelEntity;
    @Mock
    private ApiKeyEntity apiKeyEntity;
    @Mock
    private AuditService auditService;
    @Mock
    private NotifierService notifierService;
    @Mock
    private GroupService groupService;
    @Mock
    private ParameterService parameterService;
    @Mock
    private UserService userService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return null;
            }
            @Override
            public void setAuthentication(Authentication authentication) {
            }
        });
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(subscription.getStatus()).thenReturn(Subscription.Status.ACCEPTED);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(SUBSCRIPTION_ID);

        assertNotNull(subscriptionEntity);
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.findById(SUBSCRIPTION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenThrow(TechnicalException.class);

        subscriptionService.findById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldFindByApplication() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(Subscription.Status.ACCEPTED);
        sub1.setApplication(APPLICATION_ID);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(Subscription.Status.REJECTED);
        sub2.setApplication(APPLICATION_ID);

        when(subscriptionRepository.search(new SubscriptionCriteria.Builder()
                .applications(singleton(APPLICATION_ID)).build())).thenReturn(
                asList(sub1, sub2));

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(APPLICATION_ID, null);

        assertEquals(2, subscriptions.size());
    }


    @Test
    public void shouldFindByApi() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(Subscription.Status.ACCEPTED);
        sub1.setApi(API_ID);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(Subscription.Status.REJECTED);
        sub2.setApi(API_ID);

        when(subscriptionRepository.search(new SubscriptionCriteria.Builder()
                .apis(singleton(API_ID)).applications(null).build())).thenReturn(
                asList(sub1, sub2));

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApi(API_ID);

        assertEquals(2, subscriptions.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApplicationBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.search(any(SubscriptionCriteria.class))).thenThrow(TechnicalException.class);

        subscriptionService.findByApplicationAndPlan(APPLICATION_ID, null);
    }

    @Test
    public void shouldFindByPlan() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(Subscription.Status.ACCEPTED);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(Subscription.Status.REJECTED);

        when(subscriptionRepository.search(new SubscriptionCriteria.Builder()
                .plans(singleton(PLAN_ID)).build())).thenReturn(
                asList(sub1, sub2));

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByPlan(PLAN_ID);

        assertEquals(2, subscriptions.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByPlanBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.search(any(SubscriptionCriteria.class))).thenThrow(TechnicalException.class);

        subscriptionService.findByPlan(PLAN_ID);
    }

    @Test(expected = PlanNotYetPublishedException.class)
    public void shouldNotCreateBecausePlanNotPublished() throws Exception {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.STAGING);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCreateBecausePlanAlreadyClosed() throws Exception {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanAlreadyDeprecated() throws Exception {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.DEPRECATED);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanKeyless() throws Exception {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.KEY_LESS);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test
    public void shouldCreateWithoutProcess() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.MANUAL);

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(any());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithoutProcess_AcceptedGCU() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.MANUAL);

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(new PageEntity.PageRevisionId(plan.getGeneralConditions()+"-1", 2));
        newSubscriptionEntity.setGeneralConditionsAccepted(true);
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(newSubscriptionEntity);

        // Verify
        ArgumentCaptor<Subscription> subscriptionCapture = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(1)).create(subscriptionCapture.capture());
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(any());

        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());

        Subscription capturedSuscription = subscriptionCapture.getValue();
        assertNotNull(capturedSuscription);
        assertEquals(subscriptionEntity.getId(), capturedSuscription.getId());
        assertEquals(subscriptionEntity.getApplication(), capturedSuscription.getApplication());
        assertEquals(newSubscriptionEntity.getGeneralConditionsAccepted(), capturedSuscription.getGeneralConditionsAccepted());
        assertEquals(newSubscriptionEntity.getGeneralConditionsContentRevision().getPageId(), capturedSuscription.getGeneralConditionsContentPageId());
        assertEquals(Integer.valueOf(newSubscriptionEntity.getGeneralConditionsContentRevision().getRevision()), capturedSuscription.getGeneralConditionsContentRevision());

    }

    @Test(expected = PlanGeneralConditionAcceptedException.class)
    public void shouldNotCreateWithoutProcess_NotAcceptedGCU() throws Exception {
        // Prepare data
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(new PageEntity.PageRevisionId(plan.getGeneralConditions()+"-1", 2));
        newSubscriptionEntity.setGeneralConditionsAccepted(false);
        subscriptionService.create(newSubscriptionEntity);
    }

    @Test(expected = PlanGeneralConditionAcceptedException.class)
    public void shouldNotCreateWithoutProcess_AcceptedGCU_WithoutGCUContent() throws Exception {
        // Prepare data
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(null);
        newSubscriptionEntity.setGeneralConditionsAccepted(true);
        subscriptionService.create(newSubscriptionEntity);
    }

    @Test
    public void shouldCreateWithAutomaticSubscription_forApiKey() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID+"@acme.net");
        when(userService.findById(SUBSCRIBER_ID)).thenReturn(subscriberUser);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(subscriptionRepository.create(any())).thenAnswer(new Answer<Subscription>() {
            @Override
            public Subscription answer(InvocationOnMock invocation) throws Throwable {
                Subscription subscription = (Subscription) invocation.getArguments()[0];
                subscription.setId(SUBSCRIPTION_ID);
                return subscription;
            }
        });

        when(subscriptionRepository.findById(SUBSCRIPTION_ID))
                .thenAnswer(new Answer<Optional<Subscription>>() {
                    @Override
                    public Optional<Subscription> answer(InvocationOnMock invocation) throws Throwable {
                        subscription.setCreatedAt(new Date());
                        return Optional.of(subscription);
                    }
                });

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, times(1)).generate(any());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithAutomaticSubscription_notApiKey() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);

        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setClientId("my-client-id");
        settings.setoAuthClient(clientSettings);
        when(application.getSettings()).thenReturn(settings);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID+"@acme.net");
        when(userService.findById(SUBSCRIBER_ID)).thenReturn(subscriberUser);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(subscriptionRepository.create(any())).thenAnswer(new Answer<Subscription>() {
            @Override
            public Subscription answer(InvocationOnMock invocation) throws Throwable {
                Subscription subscription = (Subscription) invocation.getArguments()[0];
                subscription.setId(SUBSCRIPTION_ID);
                return subscription;
            }
        });

        when(subscriptionRepository.findById(SUBSCRIPTION_ID))
                .thenAnswer(new Answer<Optional<Subscription>>() {
                    @Override
                    public Optional<Subscription> answer(InvocationOnMock invocation) throws Throwable {
                        subscription.setCreatedAt(new Date());
                        return Optional.of(subscription);
                    }
                });

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(any());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    private SecurityContext generateSecurityContext() {
        return new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new Authentication() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return null;
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return new UserDetails("tester", "password", Collections.emptyList());
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return false;
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
            }

            @Override
            public void setAuthentication(Authentication authentication) {

            }
        };
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotSubscribe_applicationWithoutClientId() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotUpdateSubscriptionBecauseDoesNoExist() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.update(updatedSubscription);
    }

    @Test(expected = SubscriptionNotUpdatableException.class)
    public void shouldNotUpdateSubscriptionBecauseBadStatus() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        // Run
        subscriptionService.update(updatedSubscription);
    }

    @Test
    public void shouldUpdateSubscriptionWithoutEndingDate() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).findBySubscription(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDate() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(singletonList(apiKeyEntity));
        when(apiKeyEntity.isRevoked()).thenReturn(false);
        when(apiKeyEntity.getExpireAt()).thenReturn(null);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(SUBSCRIPTION_ID);
        verify(apiKeyService, times(1)).update(apiKeyEntity);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDateButRevokedApiKey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenAnswer(returnsFirstArg());
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(singletonList(apiKeyEntity));
        when(apiKeyEntity.isRevoked()).thenReturn(true);
        when(apiKeyEntity.getExpireAt()).thenReturn(null);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(SUBSCRIPTION_ID);
        verify(apiKeyService, never()).update(apiKeyEntity);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDateButExpiredApiKey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenAnswer(returnsFirstArg());
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(singletonList(apiKeyEntity));
        when(apiKeyEntity.isRevoked()).thenReturn(false);
        when(apiKeyEntity.getExpireAt()).thenReturn(new Date(updatedSubscription.getEndingAt().getTime() + 10000));
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(SUBSCRIPTION_ID);
        verify(apiKeyService, times(1)).update(apiKeyEntity);
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotCloseSubscriptionBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.close(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldCloseSubscription() throws Exception {
        final Date now = new Date();

        final Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(now);
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);

        final ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKey("api-key");
        apiKey.setRevoked(false);

        when(plan.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(singletonList(apiKey));
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(application.getPrimaryOwner()).thenReturn(mock(PrimaryOwnerEntity.class));

        subscriptionService.close(SUBSCRIPTION_ID);

        verify(apiKeyService).revoke("api-key", false);
        verify(notifierService).trigger(eq(ApiHook.SUBSCRIPTION_CLOSED), anyString(), anyMap());
        verify(notifierService).trigger(eq(ApplicationHook.SUBSCRIPTION_CLOSED), nullable(String.class), anyMap());
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotPauseSubscriptionBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.pause(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldPauseSubscription() throws Exception {
        final Date now = new Date();

        final Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(now);
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);

        final ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKey("api-key");
        apiKey.setRevoked(false);

        when(plan.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(singletonList(apiKey));
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(application.getPrimaryOwner()).thenReturn(mock(PrimaryOwnerEntity.class));

        subscriptionService.pause(SUBSCRIPTION_ID);

        verify(apiKeyService).update(apiKey);
        verify(notifierService).trigger(eq(ApiHook.SUBSCRIPTION_PAUSED), anyString(), anyMap());
        verify(notifierService).trigger(eq(ApplicationHook.SUBSCRIPTION_PAUSED), nullable(String.class), anyMap());
    }

    @Test
    public void shouldProcessButReject() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID+"@acme.net");
        when(userService.findById(SUBSCRIBER_ID)).thenReturn(subscriberUser);

        when(plan.getApi()).thenReturn(API_ID);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(processSubscription, USER_ID);

        // Verify
        verify(apiKeyService, never()).generate(any());
        verify(userService).findById(SUBSCRIBER_ID);
        verify(notifierService).triggerEmail(any(), anyString(), anyMap(), anyString());

        assertEquals(SubscriptionStatus.REJECTED, subscriptionEntity.getStatus());
        assertEquals(USER_ID, subscriptionEntity.getProcessedBy());
        assertNotNull(subscriptionEntity.getProcessedAt());
    }

    @Test
    public void shouldProcessWithAvailableCustomApiKeyForAcceptedSubscription() throws Exception {
        // Prepare data
        final String customApiKey = "customApiKey";

        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(true);
        processSubscription.setCustomApiKey(customApiKey);

        Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        when(apiKeyService.exists(customApiKey)).thenReturn(false);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID+"@acme.net");
        when(userService.findById(SUBSCRIBER_ID)).thenReturn(subscriberUser);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(processSubscription, USER_ID);

        // Verify
        verify(apiKeyService, times(1)).generate(any(), any());
        assertEquals(SubscriptionStatus.ACCEPTED, subscriptionEntity.getStatus());
        assertEquals(USER_ID, subscriptionEntity.getProcessedBy());
        assertNotNull(subscriptionEntity.getProcessedAt());
    }

    @Test(expected = ApiKeyAlreadyActivatedException.class)
    public void shouldProcessWithExistingCustomApiKeyForAcceptedSubscription() throws Exception {
        // Prepare data
        final String customApiKey = "customApiKey";

        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(true);
        processSubscription.setCustomApiKey(customApiKey);

        Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        when(apiKeyService.exists(customApiKey)).thenReturn(true);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID+"@acme.net");
        when(userService.findById(SUBSCRIBER_ID)).thenReturn(subscriberUser);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(processSubscription, USER_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotProcessBecauseClosedPlan() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.process(processSubscription, USER_ID);
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseNoClientId_oauth2() throws Exception {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseNoClientId_jwt() throws Exception {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseExistingSubscription_oauth2() throws Exception {
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);

        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test
    public void shouldTransferSubscription() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getStatus()).thenReturn(Subscription.Status.ACCEPTED);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenReturn(subscription);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);

        subscriptionService.transfer(transferSubscription, USER_ID);

        verify(notifierService).trigger(eq(ApiHook.SUBSCRIPTION_TRANSFERRED), anyString(), anyMap());
        verify(notifierService).trigger(eq(ApplicationHook.SUBSCRIPTION_TRANSFERRED), nullable(String.class), anyMap());
        verify(subscription).setUpdatedAt(any());
    }


    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onPlanWithGeneralConditions() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);
        when(plan.getGeneralConditions()).thenReturn("SOME_PAGE");
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        subscriptionService.transfer(transferSubscription, USER_ID);
    }

    @Test(expected = PlanRestrictedException.class)
    public void shouldNotCreateBecauseRestricted() {
        // Stub
        when(plan.getExcludedGroups()).thenReturn(asList("excl1", "excl2"));
        when(plan.getApi()).thenReturn("api1");
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        final GroupEntity group = new GroupEntity();
        group.setId("excl2");

        // Run
        subscriptionService.create(new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }
}
