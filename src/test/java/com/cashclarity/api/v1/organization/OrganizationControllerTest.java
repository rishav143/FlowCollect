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
import com.cashclarity.exception.OrganizationAlreadyArchivedException;
import com.cashclarity.exception.OrganizationAlreadyExistsException;
import com.cashclarity.exception.OrganizationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    @DisplayName("DELETE /api/v1/organizations/{id} - Should return 409 when already archived")
    void delete_WithArchivedOrganization_ShouldReturn409() throws Exception {
        // Arrange
        doThrow(new OrganizationAlreadyArchivedException(2L))
                .when(organizationService).delete(2L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/organizations/{id}", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already archived")));
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

        when(organizationService.update(org.mockito.ArgumentMatchers.eq(2L), any(OrganizationUpdateRequest.class)))
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
        when(organizationService.update(org.mockito.ArgumentMatchers.eq(999L), any(OrganizationUpdateRequest.class)))
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
        when(organizationService.update(org.mockito.ArgumentMatchers.eq(-1L), any(OrganizationUpdateRequest.class)))
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
        when(organizationService.update(org.mockito.ArgumentMatchers.eq(2L), any(OrganizationUpdateRequest.class)))
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
        when(organizationService.update(org.mockito.ArgumentMatchers.eq(2L), any(OrganizationUpdateRequest.class)))
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
        when(organizationService.update(org.mockito.ArgumentMatchers.eq(2L), any(OrganizationUpdateRequest.class)))
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
        when(organizationService.update(org.mockito.ArgumentMatchers.eq(2L), any(OrganizationUpdateRequest.class)))
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
