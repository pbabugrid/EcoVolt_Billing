package com.ecovolt.billing.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must not exceed 120 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @Pattern(regexp = "^$|^[0-9+\\-\\s]{7,15}$",
                message = "phone must contain 7-15 digits/+/-/space characters")
        String phone,

        @Size(max = 500, message = "address must not exceed 500 characters")
        String address
) {
}
