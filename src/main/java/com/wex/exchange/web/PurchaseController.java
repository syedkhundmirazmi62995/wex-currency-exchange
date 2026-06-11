package com.wex.exchange.web;

import com.wex.exchange.domain.Purchase;
import com.wex.exchange.service.CurrencyConversionService;
import com.wex.exchange.service.PurchaseService;
import com.wex.exchange.web.dto.ConvertedPurchaseResponse;
import com.wex.exchange.web.dto.CreatePurchaseRequest;
import com.wex.exchange.web.dto.PurchaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@Validated
@Tag(name = "Purchases", description = "Store USD purchases and retrieve them in other currencies")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final CurrencyConversionService conversionService;

    public PurchaseController(PurchaseService purchaseService,
                              CurrencyConversionService conversionService) {
        this.purchaseService = purchaseService;
        this.conversionService = conversionService;
    }

    @PostMapping
    @Operation(summary = "Store a purchase",
            description = "Stores a USD purchase and returns it with a generated id. The amount is "
                    + "rounded to the nearest cent.")
    public ResponseEntity<PurchaseResponse> store(@Valid @RequestBody CreatePurchaseRequest request) {
        Purchase purchase = purchaseService.store(
                request.description(), request.transactionDate(), request.amount());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(purchase.id())
                .toUri();
        return ResponseEntity.created(location).body(PurchaseResponse.from(purchase));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a stored purchase")
    public PurchaseResponse getById(@PathVariable UUID id) {
        return PurchaseResponse.from(purchaseService.getById(id));
    }

    @GetMapping("/{id}/converted")
    @Operation(summary = "Get a stored purchase converted to a currency",
            description = "Converts the purchase using the most recent Treasury rate on or before the "
                    + "transaction date, within the prior six months.")
    public ConvertedPurchaseResponse getConverted(
            @PathVariable UUID id,
            @Parameter(description = "Treasury country-currency descriptor, e.g. \"Canada-Dollar\", "
                    + "\"Mexico-Peso\", \"Euro Zone-Euro\"", example = "Canada-Dollar")
            @RequestParam @NotBlank String currency) {
        return ConvertedPurchaseResponse.from(conversionService.convert(id, currency));
    }
}
