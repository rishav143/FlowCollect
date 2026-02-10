package com.cashclarity.api.v1.organization;

import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequestTestData;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequestTestData;
import com.cashclarity.application.organization.OrganizationService;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.organization.OrganizationTestData;
import com.cashclarity.domain.organization.OrganizationTestUtils;
import com.cashclarity.exception.InvalidCurrencyException;
import com.cashclarity.exception.InvalidOrganizationFieldException;
import com.cashclarity.exception.InvalidOrganizationIdException;
import com.cashclarity.exception.InvalidTimezoneException;
import com.cashclarity.exception.OrganizationAlreadyActiveException;
import com.cashclarity.exception.OrganizationAlreadyArchivedException;
import com.cashclarity.exception.OrganizationAlreadyExpiredException;
import com.cashclarity.exception.OrganizationAlreadyInTrialException;
import com.cashclarity.exception.OrganizationAlreadySuspendedException;
import com.cashclarity.exception.OrganizationAlreadyExistsException;
import com.cashclarity.exception.OrganizationLogoNotFoundException;
import com.cashclarity.exception.OrganizationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Organization Controller Tests")
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @Test
    @DisplayName("POST /api/v1/organizations - Should create organization successfully")
    void create_WithValidRequest_ShouldReturn201Created() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.validWithAllFields();
        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);
        savedOrganization.setPhone(request.getPhone());
        savedOrganization.setAddress(request.getAddress());

        when(organizationService.create(any(OrganizationCreateRequest.class)))
                .thenReturn(savedOrganization);

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/organizations/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-0100"))
                .andExpect(jsonPath("$.address").value("123 Main Street, New York, NY 10001"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id} - Should return organization when it exists")
    void getById_WithExistingId_ShouldReturn200() throws Exception {
        // Arrange
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        organization.setPhone("+1-555-0100");
        organization.setAddress("123 Main Street, New York, NY 10001");

        when(organizationService.getById(2L)).thenReturn(organization);

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations/{id}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-0100"))
                .andExpect(jsonPath("$.address").value("123 Main Street, New York, NY 10001"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id} - Should return 404 when organization not found")
    void getById_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.getById(999L))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id} - Should return 400 when id is invalid")
    void getById_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.getById(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations/{id}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("GET /api/v1/organizations - Should return paged organizations")
    void list_WithPagination_ShouldReturn200() throws Exception {
        // Arrange
        Organization org1 = OrganizationTestData.valid();
        OrganizationTestUtils.setId(org1, 1L);
        Organization org2 = OrganizationTestData.valid();
        OrganizationTestUtils.setId(org2, 2L);
        org2.setEmail("billing@acme.com");

        Page<Organization> page = new PageImpl<>(
                List.of(org1, org2),
                PageRequest.of(0, 2, Sort.by("name").ascending()),
                2
        );

        when(organizationService.list(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].email").value("billing@acme.com"))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/organizations - Should return filtered results")
    void list_WithFilters_ShouldReturn200() throws Exception {
        // Arrange
        Organization org = OrganizationTestData.valid();
        OrganizationTestUtils.setId(org, 3L);
        Page<Organization> page = new PageImpl<>(List.of(org), PageRequest.of(0, 1), 1);

        LocalDate createdFrom = LocalDate.parse("2024-01-01");
        LocalDate createdTo = LocalDate.parse("2024-12-31");

        when(organizationService.list(
                eq("ACTIVE"),
                eq("contact"),
                eq("Acme"),
                eq(createdFrom),
                eq(createdTo),
                any(Pageable.class)
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations")
                        .param("status", "ACTIVE")
                        .param("email", "contact")
                        .param("name", "Acme")
                .param("createdFrom", "2024-01-01")
                .param("createdTo", "2024-12-31")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/organizations - Should return 400 for invalid status")
    void list_WithInvalidStatus_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.list(
                eq("INVALID"),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenThrow(new InvalidOrganizationFieldException("status", "unsupported value 'INVALID'"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations")
                        .param("status", "INVALID")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));
    }

    @Test
    @DisplayName("GET /api/v1/organizations - Should return 400 for invalid date range")
    void list_WithInvalidDateRange_ShouldReturn400() throws Exception {
        // Arrange
        LocalDate createdFrom = LocalDate.parse("2024-12-31");
        LocalDate createdTo = LocalDate.parse("2024-01-01");

        when(organizationService.list(
                any(),
                any(),
                any(),
                eq(createdFrom),
                eq(createdTo),
                any(Pageable.class)
        )).thenThrow(new InvalidOrganizationFieldException("createdFrom", "must be before or equal to createdTo"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations")
                .param("createdFrom", "2024-12-31")
                .param("createdTo", "2024-01-01")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("createdFrom")));
    }

    @Test
    @DisplayName("GET /api/v1/organizations - Should return 400 for invalid page size")
    void list_WithInvalidPageSize_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.list(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenThrow(new InvalidOrganizationFieldException("size", "must be between 1 and 100"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations")
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("size")));
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id} - Should return 204 when delete succeeds")
    void delete_WithExistingId_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(organizationService).delete(2L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}", 2L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id} - Should return 404 when organization not found")
    void delete_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        doThrow(new OrganizationNotFoundException(999L))
                .when(organizationService).delete(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id} - Should return 400 when id is invalid")
    void delete_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new InvalidOrganizationIdException(-1L))
                .when(organizationService).delete(-1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }


    @Test
    @DisplayName("POST /api/v1/organizations/{id}/activate - Should return 200 when activation succeeds")
    void activate_WithExistingId_ShouldReturn200() throws Exception {
        // Arrange
        Organization activated = OrganizationTestData.valid();
        OrganizationTestUtils.setId(activated, 2L);

        when(organizationService.activate(2L)).thenReturn(activated);

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/activate", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/activate - Should return 400 when id is invalid")
    void activate_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.activate(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/activate", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/activate - Should return 404 when organization not found")
    void activate_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.activate(999L))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/activate", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/activate - Should return 409 when already archived")
    void activate_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.activate(2L))
                .thenThrow(new OrganizationAlreadyArchivedException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/activate", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/activate - Should return 409 when already active")
    void activate_WithActiveOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.activate(2L))
                .thenThrow(new OrganizationAlreadyActiveException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/activate", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already active")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/suspend - Should return 200 when suspension succeeds")
    void suspend_WithExistingId_ShouldReturn200() throws Exception {
        // Arrange
        Organization suspended = OrganizationTestData.valid();
        OrganizationTestUtils.setId(suspended, 2L);
        suspended.setStatus(com.cashclarity.domain.organization.OrganizationStatus.SUSPENDED);

        when(organizationService.suspend(2L)).thenReturn(suspended);

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/suspend", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/suspend - Should return 400 when id is invalid")
    void suspend_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.suspend(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/suspend", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/suspend - Should return 404 when organization not found")
    void suspend_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.suspend(999L))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/suspend", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/suspend - Should return 409 when already archived")
    void suspend_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.suspend(2L))
                .thenThrow(new OrganizationAlreadyArchivedException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/suspend", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/suspend - Should return 409 when already suspended")
    void suspend_WithSuspendedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.suspend(2L))
                .thenThrow(new OrganizationAlreadySuspendedException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/suspend", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already suspended")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/archive - Should return 200 when archive succeeds")
    void archive_WithExistingId_ShouldReturn200() throws Exception {
        // Arrange
        Organization archived = OrganizationTestData.archived();
        OrganizationTestUtils.setId(archived, 2L);

        when(organizationService.archive(2L)).thenReturn(archived);

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/archive", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/archive - Should return 400 when id is invalid")
    void archive_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.archive(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/archive", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/archive - Should return 404 when organization not found")
    void archive_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.archive(999L))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/archive", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/archive - Should return 409 when already archived")
    void archive_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.archive(2L))
                .thenThrow(new OrganizationAlreadyArchivedException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/archive", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/trial - Should return 200 when trial succeeds")
    void trial_WithExistingId_ShouldReturn200() throws Exception {
        // Arrange
        Organization trial = OrganizationTestData.valid();
        OrganizationTestUtils.setId(trial, 2L);
        trial.setStatus(com.cashclarity.domain.organization.OrganizationStatus.TRIAL);

        when(organizationService.trial(2L)).thenReturn(trial);

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/trial", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("TRIAL"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/trial - Should return 400 when id is invalid")
    void trial_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.trial(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/trial", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/trial - Should return 404 when organization not found")
    void trial_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.trial(999L))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/trial", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/trial - Should return 409 when already archived")
    void trial_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.trial(2L))
                .thenThrow(new OrganizationAlreadyArchivedException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/trial", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/trial - Should return 409 when already in trial")
    void trial_WithAlreadyInTrial_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.trial(2L))
                .thenThrow(new OrganizationAlreadyInTrialException(2L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/trial", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already in trial")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/expired - Should return 200 when expired succeeds")
    void expired_WithExistingId_ShouldReturn200() throws Exception {
        // Arrange
        Organization expired = OrganizationTestData.valid();
        OrganizationTestUtils.setId(expired, 3L);
        expired.setStatus(com.cashclarity.domain.organization.OrganizationStatus.EXPIRED);

        when(organizationService.expired(3L)).thenReturn(expired);

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/expired", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/expired - Should return 400 when id is invalid")
    void expired_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.expired(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/expired", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/expired - Should return 404 when organization not found")
    void expired_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.expired(999L))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/expired", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/expired - Should return 409 when already archived")
    void expired_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.expired(3L))
                .thenThrow(new OrganizationAlreadyArchivedException(3L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/expired", 3L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/expired - Should return 409 when already expired")
    void expired_WithAlreadyExpired_ShouldReturn409() throws Exception {
        // Arrange
        when(organizationService.expired(3L))
                .thenThrow(new OrganizationAlreadyExpiredException(3L));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations/{id}/expired", 3L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already expired")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/logo - Should return 200 when upload succeeds")
    void uploadLogo_WithValidFile_ShouldReturn200() throws Exception {
        // Arrange
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 4L);
        organization.setLogoUrl("/uploads/organizations/4/logo.png");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "fake-image".getBytes()
        );

        when(organizationService.uploadLogo(eq(4L), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn(organization);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/organizations/{id}/logo", 4L)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.logoUrl").value("/uploads/organizations/4/logo.png"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/logo - Should return 400 when id is invalid")
    void uploadLogo_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "fake-image".getBytes()
        );

        when(organizationService.uploadLogo(eq(-1L), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/organizations/{id}/logo", -1L)
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/logo - Should return 404 when organization not found")
    void uploadLogo_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "fake-image".getBytes()
        );

        when(organizationService.uploadLogo(eq(999L), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/organizations/{id}/logo", 999L)
                        .file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations/{id}/logo - Should return 409 when archived")
    void uploadLogo_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "fake-image".getBytes()
        );

        when(organizationService.uploadLogo(eq(4L), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new OrganizationAlreadyArchivedException(4L));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/organizations/{id}/logo", 4L)
                        .file(file))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id}/logo - Should return logoUrl when present")
    void getLogo_WithExistingLogo_ShouldReturn200() throws Exception {
        // Arrange
        when(organizationService.getLogoUrl(4L))
                .thenReturn("/uploads/organizations/4/logo.png");

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations/{id}/logo", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.logoUrl").value("/uploads/organizations/4/logo.png"));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id}/logo - Should return 404 when logo missing")
    void getLogo_WithMissingLogo_ShouldReturn404() throws Exception {
        // Arrange
        when(organizationService.getLogoUrl(4L))
                .thenThrow(new OrganizationLogoNotFoundException(4L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations/{id}/logo", 4L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Logo not found")));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id}/logo - Should return 400 when id is invalid")
    void getLogo_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        when(organizationService.getLogoUrl(-1L))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/organizations/{id}/logo", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id}/logo - Should return 204 when removed")
    void removeLogo_WithExistingLogo_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(organizationService).removeLogo(4L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}/logo", 4L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id}/logo - Should return 404 when logo missing")
    void removeLogo_WithMissingLogo_ShouldReturn404() throws Exception {
        // Arrange
        doThrow(new OrganizationLogoNotFoundException(4L))
                .when(organizationService).removeLogo(4L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}/logo", 4L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Logo not found")));
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id}/logo - Should return 400 when id is invalid")
    void removeLogo_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new InvalidOrganizationIdException(-1L))
                .when(organizationService).removeLogo(-1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}/logo", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("DELETE /api/v1/organizations/{id}/logo - Should return 404 when organization not found")
    void removeLogo_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        doThrow(new OrganizationNotFoundException(999L))
                .when(organizationService).removeLogo(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}/logo", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should update organization successfully")
    void update_WithValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        Organization updated = OrganizationTestData.valid();
        OrganizationTestUtils.setId(updated, 2L);
        updated.setName("Updated Org");
        updated.setEmail("updated@acme.com");
        updated.setPhone("+44-20-0000");
        updated.setAddress("221B Baker Street, London");
        updated.setLogoUrl("https://example.com/logo.png");
        updated.setTimezone(java.time.ZoneId.of("Europe/London"));
        updated.setCurrency(java.util.Currency.getInstance("GBP"));

        when(organizationService.update(eq(2L), any(OrganizationUpdateRequest.class)))
                .thenReturn(updated);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Updated Org"))
                .andExpect(jsonPath("$.email").value("updated@acme.com"))
                .andExpect(jsonPath("$.phone").value("+44-20-0000"))
                .andExpect(jsonPath("$.address").value("221B Baker Street, London"))
                .andExpect(jsonPath("$.timezone").value("Europe/London"))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 404 when organization not found")
    void update_WithMissingId_ShouldReturn404() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        when(organizationService.update(eq(999L), any(OrganizationUpdateRequest.class)))
                .thenThrow(new OrganizationNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Organization not found")));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 400 when id is invalid")
    void update_WithInvalidId_ShouldReturn400() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        when(organizationService.update(eq(-1L), any(OrganizationUpdateRequest.class)))
                .thenThrow(new InvalidOrganizationIdException(-1L));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", -1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organizationId")));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 400 when email is invalid")
    void update_WithInvalidEmail_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.emailInvalid();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 400 when currency length invalid")
    void update_WithInvalidCurrencyLength_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.currencyInvalidLength();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 400 when field is blank")
    void update_WithBlankName_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.nameBlank();
        when(organizationService.update(eq(2L), any(OrganizationUpdateRequest.class)))
                .thenThrow(new InvalidOrganizationFieldException("name", "must not be blank"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid organization field")));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 400 when timezone is invalid")
    void update_WithInvalidTimezone_ShouldReturn400() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.timezoneInvalid();
        when(organizationService.update(eq(2L), any(OrganizationUpdateRequest.class)))
                .thenThrow(new InvalidTimezoneException("Invalid/Timezone"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid timezone")));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 400 when currency is invalid")
    void update_WithInvalidCurrency_ShouldReturn400() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.currencyInvalid();
        when(organizationService.update(eq(2L), any(OrganizationUpdateRequest.class)))
                .thenThrow(new InvalidCurrencyException("ZZZ"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid currency")));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id} - Should return 409 when email already exists")
    void update_WithDuplicateEmail_ShouldReturn409() throws Exception {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        when(organizationService.update(eq(2L), any(OrganizationUpdateRequest.class)))
                .thenThrow(new OrganizationAlreadyExistsException("updated@acme.com"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/organizations/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when name is blank")
    void create_WithBlankName_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withBlankName();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when name is null")
    void create_WithNullName_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withNullName();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when name exceeds max length")
    void create_WithNameTooLong_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withNameTooLong();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when email is invalid")
    void create_WithInvalidEmail_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withInvalidEmail();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when email is blank")
    void create_WithBlankEmail_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withBlankEmail();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when timezone is blank")
    void create_WithBlankTimezone_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withBlankTimezone();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.timezone").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when currency is blank")
    void create_WithBlankCurrency_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withBlankCurrency();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when currency length is invalid")
    void create_WithInvalidCurrencyLength_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withInvalidCurrencyLength();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when phone exceeds max length")
    void create_WithPhoneTooLong_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withPhoneTooLong();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when address exceeds max length")
    void create_WithAddressTooLong_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withAddressTooLong();

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.address").exists());
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when timezone is invalid")
    void create_WithInvalidTimezone_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withInvalidTimezone();
        when(organizationService.create(any(OrganizationCreateRequest.class)))
                .thenThrow(new InvalidTimezoneException("Invalid/Timezone"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid timezone")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 400 when currency is invalid")
    void create_WithInvalidCurrency_ShouldReturn400BadRequest() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withInvalidCurrency();
        when(organizationService.create(any(OrganizationCreateRequest.class)))
                .thenThrow(new InvalidCurrencyException("XXX"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid currency")));
    }

    @Test
    @DisplayName("POST /api/v1/organizations - Should return 409 when email already exists")
    void create_WithDuplicateEmail_ShouldReturn409Conflict() throws Exception {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        when(organizationService.create(any(OrganizationCreateRequest.class)))
                .thenThrow(new OrganizationAlreadyExistsException("contact@acme.com"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    private String toJson(OrganizationCreateRequest request) {
        return """
                {
                    "name": %s,
                    "email": %s,
                    "timezone": %s,
                    "currency": %s,
                    "phone": %s,
                    "address": %s
                }
                """.formatted(
                request.getName() != null ? "\"" + request.getName() + "\"" : "null",
                request.getEmail() != null ? "\"" + request.getEmail() + "\"" : "null",
                request.getTimezone() != null ? "\"" + request.getTimezone() + "\"" : "null",
                request.getCurrency() != null ? "\"" + request.getCurrency() + "\"" : "null",
                request.getPhone() != null ? "\"" + request.getPhone() + "\"" : "null",
                request.getAddress() != null ? "\"" + request.getAddress() + "\"" : "null"
        );
    }

    private String toJson(OrganizationUpdateRequest request) {
        return """
                {
                    "name": %s,
                    "email": %s,
                    "timezone": %s,
                    "currency": %s,
                    "phone": %s,
                    "address": %s,
                    "logoUrl": %s
                }
                """.formatted(
                request.getName() != null ? "\"" + request.getName() + "\"" : "null",
                request.getEmail() != null ? "\"" + request.getEmail() + "\"" : "null",
                request.getTimezone() != null ? "\"" + request.getTimezone() + "\"" : "null",
                request.getCurrency() != null ? "\"" + request.getCurrency() + "\"" : "null",
                request.getPhone() != null ? "\"" + request.getPhone() + "\"" : "null",
                request.getAddress() != null ? "\"" + request.getAddress() + "\"" : "null",
                request.getLogoUrl() != null ? "\"" + request.getLogoUrl() + "\"" : "null"
        );
    }
}
