package com.switchplatform.platform.controller.issuing;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCardRequest {
    @NotNull
    private UUID cardholderId;

    private String cardProduct;
}
