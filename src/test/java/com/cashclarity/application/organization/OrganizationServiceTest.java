package com.cashclarity.application.organization;

import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequestTestData;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequestTestData;
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
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization Service Tests")
class OrganizationServiceTest {

    @Mock
    private OrganizationJpaRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    @DisplayName("create - Should create organization successfully with valid request")
    void create_WithValidRequest_ShouldReturnSavedOrganization() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(savedOrganization);

        // Act
        Organization result = organizationService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Acme Corporation");
        assertThat(result.getEmail()).isEqualTo("contact@acme.com");
        assertThat(result.getTimezone()).isEqualTo(ZoneId.of("America/New_York"));
        assertThat(result.getCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(result.getStatus()).isNotNull();

        verify(organizationRepository).existsByEmail(request.getEmail().trim());
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should create organization with optional fields")
    void create_WithAllFields_ShouldIncludeOptionalFields() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.validWithAllFields();
        Organization savedOrganization = OrganizationTestData.withAllFields();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(savedOrganization);

        // Act
        Organization result = organizationService.create(request);

        // Assert
        assertThat(result.getPhone()).isEqualTo("+1-555-0100");
        assertThat(result.getAddress()).isEqualTo("123 Main Street, New York, NY 10001");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should trim whitespace from input fields")
    void create_WithWhitespaceInFields_ShouldTrimValues() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        request.setName("  Acme Corporation  ");
        request.setEmail("  contact@acme.com  ");
        request.setPhone("  +1-555-0100  ");
        request.setAddress("  123 Main Street  ");

        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail("contact@acme.com"))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    assertThat(org.getName()).isEqualTo("Acme Corporation");
                    assertThat(org.getEmail()).isEqualTo("contact@acme.com");
                    assertThat(org.getPhone()).isEqualTo("+1-555-0100");
                    assertThat(org.getAddress()).isEqualTo("123 Main Street");
                    return savedOrganization;
                });

        // Act
        organizationService.create(request);

        // Assert
        verify(organizationRepository).existsByEmail("contact@acme.com");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when email already exists")
    void create_WithDuplicateEmail_ShouldThrowOrganizationAlreadyExistsException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(OrganizationAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(organizationRepository).existsByEmail(request.getEmail().trim());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when timezone is invalid")
    void create_WithInvalidTimezone_ShouldThrowInvalidTimezoneException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withInvalidTimezone();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(InvalidTimezoneException.class)
                .hasMessageContaining("Invalid timezone");

        verify(organizationRepository, never()).existsByEmail(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when timezone is null")
    void create_WithNullTimezone_ShouldThrowInvalidTimezoneException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withNullTimezone();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(InvalidTimezoneException.class);

        verify(organizationRepository, never()).existsByEmail(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when timezone is blank")
    void create_WithBlankTimezone_ShouldThrowInvalidTimezoneException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withBlankTimezone();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(InvalidTimezoneException.class);

        verify(organizationRepository, never()).existsByEmail(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when currency is invalid")
    void create_WithInvalidCurrency_ShouldThrowInvalidCurrencyException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withInvalidCurrency();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Invalid currency");

        verify(organizationRepository, never()).existsByEmail(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when currency is null")
    void create_WithNullCurrency_ShouldThrowInvalidCurrencyException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withNullCurrency();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(InvalidCurrencyException.class);

        verify(organizationRepository, never()).existsByEmail(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should throw exception when currency is blank")
    void create_WithBlankCurrency_ShouldThrowInvalidCurrencyException() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.withBlankCurrency();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(InvalidCurrencyException.class);

        verify(organizationRepository, never()).existsByEmail(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should handle currency code case insensitivity")
    void create_WithLowerCaseCurrency_ShouldConvertToUpperCase() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        request.setCurrency("usd");

        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    assertThat(org.getCurrency().getCurrencyCode()).isEqualTo("USD");
                    return savedOrganization;
                });

        // Act
        organizationService.create(request);

        // Assert
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should not set phone when null")
    void create_WithNullPhone_ShouldNotSetPhone() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        request.setPhone(null);

        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    assertThat(org.getPhone()).isNull();
                    return savedOrganization;
                });

        // Act
        organizationService.create(request);

        // Assert
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should not set address when null")
    void create_WithNullAddress_ShouldNotSetAddress() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        request.setAddress(null);

        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    assertThat(org.getAddress()).isNull();
                    return savedOrganization;
                });

        // Act
        organizationService.create(request);

        // Assert
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("create - Should not set phone when blank")
    void create_WithBlankPhone_ShouldNotSetPhone() {
        // Arrange
        OrganizationCreateRequest request = OrganizationCreateRequestTestData.valid();
        request.setPhone("   ");

        Organization savedOrganization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(savedOrganization, 1L);

        when(organizationRepository.existsByEmail(request.getEmail().trim()))
                .thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    assertThat(org.getPhone()).isNull();
                    return savedOrganization;
                });

        // Act
        organizationService.create(request);

        // Assert
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("getById - Should return organization when id exists")
    void getById_WithExistingId_ShouldReturnOrganization() {
        // Arrange
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act
        Organization result = organizationService.getById(2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getEmail()).isEqualTo("contact@acme.com");
        verify(organizationRepository).findById(2L);
    }

    @Test
    @DisplayName("getById - Should throw when id is null")
    void getById_WithNullId_ShouldThrowInvalidOrganizationIdException() {
        // Arrange
        Long id = null;

        // Act & Assert
        assertThatThrownBy(() -> organizationService.getById(id))
                .isInstanceOf(InvalidOrganizationIdException.class)
                .hasMessageContaining("Invalid organizationId");

        verify(organizationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById - Should throw when id is zero")
    void getById_WithZeroId_ShouldThrowInvalidOrganizationIdException() {
        // Arrange
        Long id = 0L;

        // Act & Assert
        assertThatThrownBy(() -> organizationService.getById(id))
                .isInstanceOf(InvalidOrganizationIdException.class)
                .hasMessageContaining("Invalid organizationId");

        verify(organizationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById - Should throw when id is negative")
    void getById_WithNegativeId_ShouldThrowInvalidOrganizationIdException() {
        // Arrange
        Long id = -10L;

        // Act & Assert
        assertThatThrownBy(() -> organizationService.getById(id))
                .isInstanceOf(InvalidOrganizationIdException.class)
                .hasMessageContaining("Invalid organizationId");

        verify(organizationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getById - Should throw when organization not found")
    void getById_WithMissingId_ShouldThrowOrganizationNotFoundException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.getById(999L))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessageContaining("Organization not found");

        verify(organizationRepository).findById(999L);
    }

    @Test
    @DisplayName("delete - Should archive organization when id exists")
    void delete_WithExistingId_ShouldArchiveOrganization() {
        // Arrange
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        organizationService.delete(2L);

        // Assert
        assertThat(organization.isDeleted()).isTrue();
        assertThat(organization.getStatus()).isNotNull();
        verify(organizationRepository).findById(2L);
        verify(organizationRepository).save(organization);
    }

    @Test
    @DisplayName("delete - Should throw when id is null")
    void delete_WithNullId_ShouldThrowInvalidOrganizationIdException() {
        // Arrange
        Long id = null;

        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(InvalidOrganizationIdException.class)
                .hasMessageContaining("Invalid organizationId");

        verify(organizationRepository, never()).findById(any());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - Should throw when id is negative")
    void delete_WithNegativeId_ShouldThrowInvalidOrganizationIdException() {
        // Arrange
        Long id = -5L;

        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(InvalidOrganizationIdException.class)
                .hasMessageContaining("Invalid organizationId");

        verify(organizationRepository, never()).findById(any());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - Should throw when organization not found")
    void delete_WithMissingId_ShouldThrowOrganizationNotFoundException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(999L))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessageContaining("Organization not found");

        verify(organizationRepository).findById(999L);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - Should throw when organization already archived")
    void delete_WithArchivedOrganization_ShouldThrowOrganizationAlreadyArchivedException() {
        // Arrange
        Organization organization = OrganizationTestData.archived();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(2L))
                .isInstanceOf(OrganizationAlreadyArchivedException.class)
                .hasMessageContaining("already archived");

        verify(organizationRepository).findById(2L);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - Should update mutable fields when valid")
    void update_WithValidRequest_ShouldReturnUpdatedOrganization() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);

        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));
        when(organizationRepository.existsByEmailAndIdNot("updated@acme.com", 2L)).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Organization result = organizationService.update(2L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Org");
        assertThat(result.getEmail()).isEqualTo("updated@acme.com");
        assertThat(result.getPhone()).isEqualTo("+44-20-0000");
        assertThat(result.getAddress()).isEqualTo("221B Baker Street, London");
        assertThat(result.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(result.getTimezone()).isEqualTo(java.time.ZoneId.of("Europe/London"));
        assertThat(result.getCurrency()).isEqualTo(java.util.Currency.getInstance("GBP"));
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("update - Should ignore null fields")
    void update_WithNullFields_ShouldNotOverwrite() {
        // Arrange
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);

        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));
        // Act
        Organization result = organizationService.update(2L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Acme Corporation");
        assertThat(result.getEmail()).isEqualTo("contact@acme.com");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("update - Should not save when no changes detected")
    void update_WithSameValues_ShouldNotSave() {
        // Arrange
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("Acme Corporation");
        request.setEmail("contact@acme.com");
        request.setTimezone("America/New_York");
        request.setCurrency("USD");
        request.setPhone(null);
        request.setAddress(null);

        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act
        Organization result = organizationService.update(2L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Acme Corporation");
        assertThat(result.getEmail()).isEqualTo("contact@acme.com");
        assertThat(result.getTimezone()).isEqualTo(java.time.ZoneId.of("America/New_York"));
        assertThat(result.getCurrency()).isEqualTo(java.util.Currency.getInstance("USD"));
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("update - Should clear optional fields when blank")
    void update_WithBlankOptionalFields_ShouldClearValues() {
        // Arrange
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setPhone("   ");
        request.setAddress("   ");
        request.setLogoUrl("   ");

        Organization organization = OrganizationTestData.withAllFields();
        OrganizationTestUtils.setId(organization, 2L);

        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Organization result = organizationService.update(2L, request);

        // Assert
        assertThat(result.getPhone()).isNull();
        assertThat(result.getAddress()).isNull();
        assertThat(result.getLogoUrl()).isNull();
    }

    @Test
    @DisplayName("update - Should throw when id is invalid")
    void update_WithInvalidId_ShouldThrowInvalidOrganizationIdException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(-1L, request))
                .isInstanceOf(InvalidOrganizationIdException.class);

        verify(organizationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update - Should throw when organization not found")
    void update_WithMissingId_ShouldThrowOrganizationNotFoundException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        when(organizationRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(999L, request))
                .isInstanceOf(OrganizationNotFoundException.class);

        verify(organizationRepository).findById(999L);
    }

    @Test
    @DisplayName("update - Should throw when name is blank")
    void update_WithBlankName_ShouldThrowInvalidOrganizationFieldException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.nameBlank();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(2L, request))
                .isInstanceOf(InvalidOrganizationFieldException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("update - Should throw when email is blank")
    void update_WithBlankEmail_ShouldThrowInvalidOrganizationFieldException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.emailBlank();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(2L, request))
                .isInstanceOf(InvalidOrganizationFieldException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("update - Should throw when timezone is invalid")
    void update_WithInvalidTimezone_ShouldThrowInvalidTimezoneException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.timezoneInvalid();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(2L, request))
                .isInstanceOf(InvalidTimezoneException.class);
    }

    @Test
    @DisplayName("update - Should throw when currency is invalid")
    void update_WithInvalidCurrency_ShouldThrowInvalidCurrencyException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.currencyInvalid();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);
        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(2L, request))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    @DisplayName("update - Should throw when email already exists")
    void update_WithDuplicateEmail_ShouldThrowOrganizationAlreadyExistsException() {
        // Arrange
        OrganizationUpdateRequest request = OrganizationUpdateRequestTestData.valid();
        Organization organization = OrganizationTestData.valid();
        OrganizationTestUtils.setId(organization, 2L);

        when(organizationRepository.findById(2L)).thenReturn(java.util.Optional.of(organization));
        when(organizationRepository.existsByEmailAndIdNot("updated@acme.com", 2L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(2L, request))
                .isInstanceOf(OrganizationAlreadyExistsException.class);
    }
}
