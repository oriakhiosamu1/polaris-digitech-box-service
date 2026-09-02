package com.joyboy.polaris_digitech.controller;

import com.joyboy.polaris_digitech.dto.*;
import com.joyboy.polaris_digitech.service.BoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boxes")
@RequiredArgsConstructor
@Tag(name = "Box Management", description = "APIs for managing delivery boxes")
public class BoxController {

    private final BoxService boxService;

    // 1. Create a box
    @PostMapping
    @Operation(summary = "Create a new box")
    public ResponseEntity<BoxResponse> createBox(@Valid @RequestBody BoxRequest request) {
        BoxResponse response = boxService.createBox(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. Load a box with items
    @PostMapping("/{txref}/load")
    @Operation(summary = "Load items into a box")
    public ResponseEntity<BoxResponse> loadBox(@PathVariable String txref, @Valid @RequestBody LoadBoxRequest request) {
        BoxResponse response = boxService.loadBox(txref, request);
        return ResponseEntity.ok(response);
    }

    // 3. Check loaded items for a given box
    @GetMapping("/{txref}/items")
    @Operation(summary = "Get all items loaded in a box")
    public ResponseEntity<List<ItemResponse>> getLoadedItems(@PathVariable String txref) {
        List<ItemResponse> items = boxService.getLoadedItems(txref);
        return ResponseEntity.ok(items);
    }

    // 4. Check available boxes for loading
    @GetMapping("/available")
    @Operation(summary = "Get all boxes available for loading")
    public ResponseEntity<List<BoxResponse>> getAvailableBoxes() {
        List<BoxResponse> boxes = boxService.getAvailableBoxes();
        return ResponseEntity.ok(boxes);
    }

    // 5. Check battery level for a given box
    @GetMapping("/{txref}/battery")
    @Operation(summary = "Get battery level of a box")
    public ResponseEntity<BatteryResponse> getBatteryLevel(@PathVariable String txref) {
        BatteryResponse response = boxService.getBatteryLevel(txref);
        return ResponseEntity.ok(response);
    }

    // 6. Remove specific items from a box
    @PatchMapping("/{txref}/items")
    @Operation(summary = "Remove specific items from a box by item code")
    public ResponseEntity<BoxResponse> removeItems(@PathVariable String txref, @Valid @RequestBody BoxUpdateRequest request) {
        BoxResponse response = boxService.removeItems(txref, request);
        return ResponseEntity.ok(response);
    }

    // 7. Remove all items from a box
    @DeleteMapping("/{txref}/items")
    @Operation(summary = "Remove all items from a box")
    public ResponseEntity<BoxResponse> removeAllItems(@PathVariable String txref) {
        BoxResponse response = boxService.removeAllItems(txref);
        return ResponseEntity.ok(response);
    }

    // 8. Delete a box
    @DeleteMapping("/{txref}")
    @Operation(summary = "Delete a box")
    public ResponseEntity<Void> deleteBox(@PathVariable String txref) {
        boxService.deleteBox(txref);
        return ResponseEntity.noContent().build();
    }
}