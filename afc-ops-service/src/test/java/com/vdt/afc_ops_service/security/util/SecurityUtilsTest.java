package com.vdt.afc_ops_service.security.util;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    OperatorRepository operatorRepository;

    SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = new SecurityUtils(operatorRepository);
        AfcUserDetails principal = AfcUserDetails.builder()
                .id(" account-1 ")
                .username("manager")
                .operatorCode(" HCMC-METRO ")
                .authorities(List.of(new SimpleGrantedAuthority("MASTER_DATA_READ")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("MASTER_DATA_READ")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRequiredCurrentAccountId_TrimsValue() {
        assertEquals("account-1", SecurityUtils.getRequiredCurrentAccountId());
    }

    @Test
    void getCurrentUsername_ReturnsUsername() {
        assertEquals("manager", SecurityUtils.getCurrentUsername());
    }

    @Test
    void getCurrentAuthorities_ReturnsAuthorityNames() {
        assertEquals(List.of("MASTER_DATA_READ"), SecurityUtils.getCurrentAuthorities());
    }

    @Test
    void isAuthenticated_WithCurrentUser_ReturnsTrue() {
        assertEquals(true, SecurityUtils.isAuthenticated());
    }

    @Test
    void getRequiredCurrentOperatorCode_TrimsValue() {
        assertEquals("HCMC-METRO", SecurityUtils.getRequiredCurrentOperatorCode());
    }

    @Test
    void getRequiredCurrentOperator_ReturnsOperator() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        assertEquals(operator, securityUtils.getRequiredCurrentOperator());
    }

    @Test
    void getRequiredCurrentOperator_WhenMissing_ThrowsOperatorNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> securityUtils.getRequiredCurrentOperator());

        assertEquals(ErrorCode.OPERATOR_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getRequiredCurrentOperatorCode_WhenMissingAuthentication_ThrowsOperatorScopeRequired() {
        SecurityContextHolder.clearContext();

        AppException exception = assertThrows(AppException.class, SecurityUtils::getRequiredCurrentOperatorCode);

        assertEquals(ErrorCode.OPERATOR_SCOPE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void getRequiredCurrentAccountId_WhenMissingAuthentication_ThrowsUnauthenticated() {
        SecurityContextHolder.clearContext();

        AppException exception = assertThrows(AppException.class, SecurityUtils::getRequiredCurrentAccountId);

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void getCurrentUser_WhenPrincipalIsNotAfcUserDetails_ReturnsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        assertEquals(null, SecurityUtils.getCurrentUser());
        assertEquals(false, SecurityUtils.isAuthenticated());
    }

    @Test
    void getCurrentAuthorities_WhenAuthenticationMissing_ReturnsEmptyList() {
        SecurityContextHolder.clearContext();

        assertEquals(List.of(), SecurityUtils.getCurrentAuthorities());
    }

    @Test
    void getCurrentUser_WhenAuthenticationNotAuthenticated_ReturnsNull() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                AfcUserDetails.builder().id("account-1").build(),
                null
        );
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(null, SecurityUtils.getCurrentUser());
    }
}
