package com.flowcollect.application.organization;

import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.api.v1.organization.dto.OrganizationUpdateRequest;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.OrganizationStatus;
import com.flowcollect.domain.organization.PaymentCollectionMode;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.ForbiddenException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.flowcollect.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationServiceTest {

    @Mock
    private OrganizationJpaRepository organizationRepository;

    @Mock
    private FollowUpJpaRepository followUpRepository;

    private OrganizationService organizationService;

    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID OTHER_ORG_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        organizationService = new OrganizationService(organizationRepository, followUpRepository);
        AuthContext.clear();
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // Real org — getId() returns null (no JPA). Only use where getId() is not checked.
    private Organization realOrg() {
        return new Organization("Test Org", "test@example.com", ZoneId.of("UTC"), Currency.getInstance("USD"));
    }

    // Spy org — getId() returns the given id. Use where getById() auth check is involved.
    private Organization spyOrg(UUID id) {
        Organization org = spy(new Organization("Test Org", "test@example.com", ZoneId.of("UTC"), Currency.getInstance("USD")));
        doReturn(id).when(org).getId();
        return org;
    }

    private void setAuth(UUID orgId) {
        AuthContext.set(new AuthContext(USER_ID, orgId, UserRole.ADMIN));
    }

    private OrganizationCreateRequest createRequest(String name, String email, String tz, String currency) {
        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setName(name);
        req.setEmail(email);
        req.setTimezone(tz);
        req.setCurrency(currency);
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void throwsForbidden_WhenAuthenticated() {
            setAuth(ORG_ID);
            assertThrows(ForbiddenException.class,
                    () -> organizationService.create(new OrganizationCreateRequest()));
            verifyNoInteractions(organizationRepository);
        }

        @Test
        void succeeds_WhenNotAuthenticated() {
            OrganizationCreateRequest req = createRequest("New Org", "new@example.com", "UTC", "USD");
            when(organizationRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.create(req);

            assertNotNull(result);
            assertEquals("New Org", result.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createForRegistration() — internal path used by auth/register
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class CreateForRegistration {

        @Test
        void succeeds_WithMinimalFields() {
            OrganizationCreateRequest req = createRequest("My Org", "org@example.com", "America/New_York", "INR");
            when(organizationRepository.existsByEmail("org@example.com")).thenReturn(false);
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.createForRegistration(req);

            assertEquals("My Org", result.getName());
            assertEquals("org@example.com", result.getEmail());
            assertEquals(PaymentCollectionMode.PAYMENT_LINK, result.getPaymentCollectionMode()); // default
        }

        @Test
        void setsConfirmationFlow_WhenSpecified() {
            OrganizationCreateRequest req = createRequest("Org", "org@example.com", "UTC", "USD");
            req.setPaymentCollectionMode("CONFIRMATION_FLOW");
            when(organizationRepository.existsByEmail("org@example.com")).thenReturn(false);
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.createForRegistration(req);

            assertEquals(PaymentCollectionMode.CONFIRMATION_FLOW, result.getPaymentCollectionMode());
        }

        @Test
        void acceptsPaymentModeCase_Insensitive() {
            OrganizationCreateRequest req = createRequest("Org", "org@example.com", "UTC", "USD");
            req.setPaymentCollectionMode("payment_link");
            when(organizationRepository.existsByEmail("org@example.com")).thenReturn(false);
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.createForRegistration(req);

            assertEquals(PaymentCollectionMode.PAYMENT_LINK, result.getPaymentCollectionMode());
        }

        @Test
        void trimsWhitespace_OnAllStringFields() {
            OrganizationCreateRequest req = createRequest("  My Org  ", "  org@example.com  ", "UTC", "USD");
            req.setPhone("  +911234567890  ");
            req.setAddress("  123 Main St  ");
            when(organizationRepository.existsByEmail("org@example.com")).thenReturn(false);
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.createForRegistration(req);

            assertEquals("My Org", result.getName());
            assertEquals("org@example.com", result.getEmail());
            assertEquals("+911234567890", result.getPhone());
            assertEquals("123 Main St", result.getAddress());
        }

        @Test
        void skipsPhone_WhenBlank() {
            OrganizationCreateRequest req = createRequest("Org", "org@example.com", "UTC", "USD");
            req.setPhone("   ");
            when(organizationRepository.existsByEmail("org@example.com")).thenReturn(false);
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.createForRegistration(req);

            assertNull(result.getPhone());
        }

        @Test
        void throwsValidation_WhenTimezoneNull() {
            OrganizationCreateRequest req = createRequest("Org", "e@example.com", null, "USD");
            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
        }

        @Test
        void throwsValidation_WhenTimezoneBlank() {
            OrganizationCreateRequest req = createRequest("Org", "e@example.com", "   ", "USD");
            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
        }

        @Test
        void throwsValidation_WhenTimezoneInvalid() {
            OrganizationCreateRequest req = createRequest("Org", "e@example.com", "INVALID/ZONE", "USD");
            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
        }

        @Test
        void throwsValidation_WhenCurrencyNull() {
            OrganizationCreateRequest req = createRequest("Org", "e@example.com", "UTC", null);
            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
        }

        @Test
        void throwsValidation_WhenCurrencyBlank() {
            OrganizationCreateRequest req = createRequest("Org", "e@example.com", "UTC", "   ");
            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
        }

        @Test
        void throwsValidation_WhenCurrencyInvalid() {
            OrganizationCreateRequest req = createRequest("Org", "e@example.com", "UTC", "XYZ");
            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
        }

        @Test
        void throwsValidation_WhenEmailAlreadyTaken() {
            OrganizationCreateRequest req = createRequest("Org", "taken@example.com", "UTC", "USD");
            when(organizationRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
            verify(organizationRepository, never()).save(any());
        }

        @Test
        void throwsValidation_WhenPaymentCollectionModeInvalid() {
            OrganizationCreateRequest req = createRequest("Org", "org@example.com", "UTC", "USD");
            req.setPaymentCollectionMode("INVALID_MODE");
            when(organizationRepository.existsByEmail("org@example.com")).thenReturn(false);

            assertThrows(ValidationException.class, () -> organizationService.createForRegistration(req));
            verify(organizationRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAuthorizedById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class GetAuthorizedById {

        @Test
        void succeeds_WhenAccessingOwnOrg() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            Organization result = organizationService.getAuthorizedById(ORG_ID);

            assertNotNull(result);
            assertEquals(ORG_ID, result.getId());
        }

        @Test
        void throwsForbidden_WhenAccessingOtherOrg() {
            setAuth(ORG_ID);
            Organization otherOrg = spyOrg(OTHER_ORG_ID);
            when(organizationRepository.findById(OTHER_ORG_ID)).thenReturn(Optional.of(otherOrg));

            assertThrows(ForbiddenException.class,
                    () -> organizationService.getAuthorizedById(OTHER_ORG_ID));
        }

        @Test
        void throwsForbidden_WhenNoAuthContext() {
            // AuthContext is cleared in setUp
            assertThrows(ForbiddenException.class,
                    () -> organizationService.getAuthorizedById(ORG_ID));
        }

        @Test
        void throwsNotFound_WhenOrgDoesNotExist() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> organizationService.getAuthorizedById(ORG_ID));
        }

        @Test
        void throwsNotFound_WhenOrgIsArchived() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            doReturn(true).when(org).isDeleted();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(NotFoundException.class,
                    () -> organizationService.getAuthorizedById(ORG_ID));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAuthorized()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class UpdateAuthorized {

        @Test
        void throwsForbidden_WhenAccessingOtherOrg() {
            setAuth(ORG_ID);

            assertThrows(ForbiddenException.class,
                    () -> organizationService.updateAuthorized(OTHER_ORG_ID, new OrganizationUpdateRequest()));
            verifyNoInteractions(organizationRepository);
        }

        @Test
        void noSave_WhenNoFieldsChanged() {
            setAuth(ORG_ID);
            Organization org = realOrg();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            organizationService.updateAuthorized(ORG_ID, new OrganizationUpdateRequest()); // all nulls

            verify(organizationRepository, never()).save(any());
        }

        @Test
        void noSave_WhenSameValuesProvided() {
            setAuth(ORG_ID);
            Organization org = realOrg(); // name="Test Org", email="test@example.com", tz=UTC, currency=USD
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setName("Test Org");   // same
            req.setCurrency("USD");    // same
            req.setTimezone("UTC");    // same

            organizationService.updateAuthorized(ORG_ID, req);

            verify(organizationRepository, never()).save(any());
        }

        @Test
        void updatesName() {
            setAuth(ORG_ID);
            Organization org = realOrg();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setName("Updated Name");

            Organization result = organizationService.updateAuthorized(ORG_ID, req);

            assertEquals("Updated Name", result.getName());
            verify(organizationRepository).save(org);
        }

        @Test
        void updatesPaymentCollectionMode() {
            setAuth(ORG_ID);
            Organization org = realOrg(); // default PAYMENT_LINK
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setPaymentCollectionMode("CONFIRMATION_FLOW");

            Organization result = organizationService.updateAuthorized(ORG_ID, req);

            assertEquals(PaymentCollectionMode.CONFIRMATION_FLOW, result.getPaymentCollectionMode());
        }

        @Test
        void clearsPhone_WhenSetToBlank() {
            setAuth(ORG_ID);
            Organization org = realOrg();
            org.setPhone("+1234567890");
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setPhone("   ");

            Organization result = organizationService.updateAuthorized(ORG_ID, req);

            assertNull(result.getPhone());
        }

        @Test
        void clearsAddress_WhenSetToBlank() {
            setAuth(ORG_ID);
            Organization org = realOrg();
            org.setAddress("123 Main St");
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setAddress("  ");

            Organization result = organizationService.updateAuthorized(ORG_ID, req);

            assertNull(result.getAddress());
        }

        @Test
        void clearsLogoUrl_WhenSetToBlank() {
            setAuth(ORG_ID);
            Organization org = realOrg();
            org.setLogoUrl("https://example.com/logo.png");
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setLogoUrl("  ");

            Organization result = organizationService.updateAuthorized(ORG_ID, req);

            assertNull(result.getLogoUrl());
        }

        @Test
        void throwsValidation_WhenNameBlank() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(realOrg()));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setName("   ");

            assertThrows(ValidationException.class,
                    () -> organizationService.updateAuthorized(ORG_ID, req));
        }

        @Test
        void throwsValidation_WhenEmailBlank() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(realOrg()));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setEmail("   ");

            assertThrows(ValidationException.class,
                    () -> organizationService.updateAuthorized(ORG_ID, req));
        }

        @Test
        void throwsConflict_WhenEmailTakenByAnotherOrg() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(realOrg()));
            when(organizationRepository.existsByEmailAndIdNot("taken@example.com", ORG_ID)).thenReturn(true);

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setEmail("taken@example.com");

            assertThrows(ConflictException.class,
                    () -> organizationService.updateAuthorized(ORG_ID, req));
        }

        @Test
        void throwsValidation_WhenTimezoneInvalid() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(realOrg()));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setTimezone("Not/ATimezone");

            assertThrows(ValidationException.class,
                    () -> organizationService.updateAuthorized(ORG_ID, req));
        }

        @Test
        void throwsValidation_WhenCurrencyInvalid() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(realOrg()));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setCurrency("XYZ");

            assertThrows(ValidationException.class,
                    () -> organizationService.updateAuthorized(ORG_ID, req));
        }

        @Test
        void throwsValidation_WhenPaymentCollectionModeInvalid() {
            setAuth(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(realOrg()));

            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setPaymentCollectionMode("INVALID");

            assertThrows(ValidationException.class,
                    () -> organizationService.updateAuthorized(ORG_ID, req));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAuthorized() — always blocked for authenticated users
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class DeleteAuthorized {

        @Test
        void alwaysThrowsForbidden() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ForbiddenException.class,
                    () -> organizationService.deleteAuthorized(ORG_ID));
            verify(organizationRepository, never()).delete(any(Organization.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State transition authorized methods — always blocked for authenticated users
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class StateTransitionAuthorized {

        @Test
        void activateAuthorized_AlwaysThrowsForbidden() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ForbiddenException.class,
                    () -> organizationService.activateAuthorized(ORG_ID));
        }

        @Test
        void suspendAuthorized_AlwaysThrowsForbidden() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ForbiddenException.class,
                    () -> organizationService.suspendAuthorized(ORG_ID));
        }

        @Test
        void archiveAuthorized_AlwaysThrowsForbidden() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ForbiddenException.class,
                    () -> organizationService.archiveAuthorized(ORG_ID));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // activate() — internal method
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class Activate {

        @Test
        void succeeds_FromSuspended() {
            Organization org = realOrg();
            org.suspend();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.activate(ORG_ID);

            assertEquals(OrganizationStatus.ACTIVE, result.getStatus());
        }

        @Test
        void succeeds_FromTrial() {
            Organization org = realOrg();
            org.trial();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.activate(ORG_ID);

            assertEquals(OrganizationStatus.ACTIVE, result.getStatus());
        }

        @Test
        void throwsConflict_WhenAlreadyActive() {
            Organization org = realOrg(); // default ACTIVE
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ConflictException.class, () -> organizationService.activate(ORG_ID));
            verify(organizationRepository, never()).save(any());
        }

        @Test
        void throwsNotFound_WhenArchived() {
            // getOrganizationOrThrow detects isDeleted=true and throws NotFoundException
            Organization org = realOrg();
            org.archive(); // sets deletedAt
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(NotFoundException.class, () -> organizationService.activate(ORG_ID));
        }

        @Test
        void throwsNotFound_WhenOrgDoesNotExist() {
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> organizationService.activate(ORG_ID));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // suspend() — internal method
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class Suspend {

        @Test
        void succeeds_FromActive() {
            Organization org = realOrg();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.suspend(ORG_ID);

            assertEquals(OrganizationStatus.SUSPENDED, result.getStatus());
        }

        @Test
        void throwsConflict_WhenAlreadySuspended() {
            Organization org = realOrg();
            org.suspend();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ConflictException.class, () -> organizationService.suspend(ORG_ID));
            verify(organizationRepository, never()).save(any());
        }

        @Test
        void throwsNotFound_WhenArchived() {
            Organization org = realOrg();
            org.archive();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(NotFoundException.class, () -> organizationService.suspend(ORG_ID));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // archive() — internal method
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class Archive {

        @Test
        void succeeds_SetsDeletedAtAndStatus() {
            Organization org = realOrg();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Organization result = organizationService.archive(ORG_ID);

            assertEquals(OrganizationStatus.ARCHIVED, result.getStatus());
            assertTrue(result.isDeleted());
            assertNotNull(result.getDeletedAt());
        }

        @Test
        void throwsNotFound_WhenAlreadyArchived() {
            Organization org = realOrg();
            org.archive();
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            // getOrganizationOrThrow throws NotFoundException since isDeleted=true
            assertThrows(NotFoundException.class, () -> organizationService.archive(ORG_ID));
        }

        @Test
        void throwsNotFound_WhenOrgDoesNotExist() {
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> organizationService.archive(ORG_ID));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listAuthorized()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ListAuthorized {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        void throwsForbidden_WhenNoAuthContext() {
            assertThrows(ForbiddenException.class,
                    () -> organizationService.listAuthorized(null, null, null, null, null, pageable));
        }

        @Test
        void throwsValidation_WhenDateRangeInvalid() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            LocalDate from = LocalDate.of(2025, 6, 1);
            LocalDate to   = LocalDate.of(2025, 1, 1); // to < from

            assertThrows(ValidationException.class,
                    () -> organizationService.listAuthorized(null, null, null, from, to, pageable));
        }

        @Test
        void throwsValidation_WhenStatusInvalid() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

            assertThrows(ValidationException.class,
                    () -> organizationService.listAuthorized("INVALID_STATUS", null, null, null, null, pageable));
        }

        @Test
        void acceptsAllValidStatuses() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            for (OrganizationStatus status : OrganizationStatus.values()) {
                assertDoesNotThrow(() ->
                        organizationService.listAuthorized(status.name(), null, null, null, null, pageable));
            }
        }

        @Test
        void returnsResults_WithNoFilters() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(org)));

            Page<Organization> result = organizationService.listAuthorized(
                    null, null, null, null, null, pageable);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void acceptsSameDateRange() {
            setAuth(ORG_ID);
            Organization org = spyOrg(ORG_ID);
            when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            LocalDate date = LocalDate.of(2025, 3, 1);

            assertDoesNotThrow(() ->
                    organizationService.listAuthorized(null, null, null, date, date, pageable));
        }
    }
}
