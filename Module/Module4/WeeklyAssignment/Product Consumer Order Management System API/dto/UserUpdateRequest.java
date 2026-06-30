package org.nbfc.productwa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fields a user is allowed to update on their own profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @NotBlank(message = "First name must not be blank")
    private String firstName;

    @NotBlank(message = "Last name must not be blank")
    private String lastName;

    @NotBlank(message = "Mobile must not be blank")
    @Pattern(regexp = "\\d{10}", message = "Mobile must be exactly 10 digits")
    private String mobile;

    @NotBlank(message = "Address must not be blank")
    private String address;
}
