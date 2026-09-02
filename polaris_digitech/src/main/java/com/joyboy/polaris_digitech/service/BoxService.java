package com.joyboy.polaris_digitech.service;

import com.joyboy.polaris_digitech.dto.*;
import com.joyboy.polaris_digitech.exception.BusinessException;
import com.joyboy.polaris_digitech.exception.ResourceNotFoundException;
import com.joyboy.polaris_digitech.model.Box;
import com.joyboy.polaris_digitech.model.BoxState;
import com.joyboy.polaris_digitech.model.Item;
import com.joyboy.polaris_digitech.repository.BoxRepository;
import com.joyboy.polaris_digitech.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoxService {

    private final BoxRepository boxRepository;
    private final ItemRepository itemRepository;

    // 1. CREATE BOX
    @Transactional
    public BoxResponse createBox(BoxRequest request) {

        // step 1: Check if a box with the same txref already exists
        if (boxRepository.existsByTxref(request.getTxref())) {
            throw new BusinessException("Box with txref '" + request.getTxref() + "' already exists");
        }

        Box box = Box.builder()
                .txref(request.getTxref())
                .weightLimit(request.getWeightLimit())
                .batteryCapacity(request.getBatteryCapacity())
                .state(BoxState.IDLE)
                .build();

        Box savedBox = boxRepository.save(box);
        return mapToBoxResponse(savedBox);
    }

    // 2. LOAD BOX WITH ITEMS
    @Transactional
    public BoxResponse loadBox(String txref, LoadBoxRequest request) {
        Box box = boxRepository.findByTxref(txref)
                .orElseThrow(() -> new ResourceNotFoundException("Box not found with txref: " + txref));

        // Step 1: Battery must be >= 25% to be in LOADING state
        if (box.getBatteryCapacity() < 25) {
            throw new BusinessException("Cannot load box. Battery level is below 25%");
        }

        // Step 2: Box must be in IDLE or LOADING state
        if (box.getState() != BoxState.IDLE && box.getState() != BoxState.LOADING) {
            throw new BusinessException("Box cannot be loaded. Current state: " + box.getState());
        }

        // Calculate total weight of new items
        int newItemsWeight = request.getItems().stream()
                .mapToInt(ItemRequest::getWeight)
                .sum();

        int currentWeight = box.getCurrentWeight();
        int totalWeight = currentWeight + newItemsWeight;

        // Step 3: Prevent overloading
        if (totalWeight > box.getWeightLimit()) {
            throw new BusinessException(
                    "Cannot load items. Total weight (" + totalWeight + "gr) exceeds weight limit (" + box.getWeightLimit() + "gr)"
            );
        }

        // Check for duplicate item codes
        for (ItemRequest itemReq : request.getItems()) {
            if (itemRepository.existsByCode(itemReq.getCode())) {
                throw new BusinessException("Item with code '" + itemReq.getCode() + "' already exists");
            }
        }

        // Change state to LOADING
        box.setState(BoxState.LOADING);

        // Add items
        for (ItemRequest itemReq : request.getItems()) {
            Item item = Item.builder()
                    .name(itemReq.getName())
                    .weight(itemReq.getWeight())
                    .code(itemReq.getCode())
                    .build();

            box.addItem(item);
        }

        // Only mark LOADED if the box is actually full; otherwise leave it LOADING
        // so more items can still be added in a later call
        if (box.getCurrentWeight() >= box.getWeightLimit()) {
            box.setState(BoxState.LOADED);
        } else {
            box.setState(BoxState.LOADING);
        }

        Box updatedBox = boxRepository.save(box);
        return mapToBoxResponse(updatedBox);
    }

    // 3. CHECK LOADED ITEMS
    @Transactional(readOnly = true)
    public List<ItemResponse> getLoadedItems(String txref) {
        Box box = boxRepository.findByTxref(txref)
                .orElseThrow(() -> new ResourceNotFoundException("Box not found with txref: " + txref));

        return box.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    // 4. CHECK AVAILABLE BOXES FOR LOADING
    @Transactional(readOnly = true)
    public List<BoxResponse> getAvailableBoxes() {
        List<BoxState> allowedStates = Arrays.asList(BoxState.IDLE, BoxState.LOADING);

        return boxRepository.findAvailableBoxes(allowedStates).stream()
                .filter(box -> box.getCurrentWeight() < box.getWeightLimit()) // still has capacity
                .map(this::mapToBoxResponse)
                .collect(Collectors.toList());
    }

    // 5. CHECK BATTERY LEVEL
    @Transactional(readOnly = true)
    public BatteryResponse getBatteryLevel(String txref) {
        Box box = boxRepository.findByTxref(txref)
                .orElseThrow(() -> new ResourceNotFoundException("Box not found with txref: " + txref));

        return BatteryResponse.builder()
                .txref(box.getTxref())
                .batteryCapacity(box.getBatteryCapacity())
                .build();
    }

    // 6. REMOVE SPECIFIC ITEMS FROM A BOX
    @Transactional
    public BoxResponse removeItems(String txref, BoxUpdateRequest request) {
        Box box = boxRepository.findByTxref(txref)
                .orElseThrow(() -> new ResourceNotFoundException("Box not found with txref: " + txref));

        // Don't allow mutating a box that's out for delivery
        if (box.getState() == BoxState.DELIVERING) {
            throw new BusinessException("Cannot remove items while box is DELIVERING");
        }

        List<String> codesToRemove = request.getItemCodes();

        // Validate all requested codes actually exist on this box
        List<String> existingCodes = box.getItems().stream()
                .map(Item::getCode)
                .collect(Collectors.toList());

        List<String> notFound = codesToRemove.stream()
                .filter(code -> !existingCodes.contains(code))
                .collect(Collectors.toList());

        if (!notFound.isEmpty()) {
            throw new ResourceNotFoundException("Item code(s) not found on box '" + txref + "': " + notFound);
        }

        // Remove matching items — orphanRemoval=true deletes them from the DB
        box.getItems().removeIf(item -> codesToRemove.contains(item.getCode()));

        // Re-evaluate state based on what's left
        updateStateAfterRemoval(box);

        Box updatedBox = boxRepository.save(box);
        return mapToBoxResponse(updatedBox);
    }

    // 7. REMOVE ALL ITEMS FROM A BOX
    @Transactional
    public BoxResponse removeAllItems(String txref) {
        Box box = boxRepository.findByTxref(txref)
                .orElseThrow(() -> new ResourceNotFoundException("Box not found with txref: " + txref));

        if (box.getState() == BoxState.DELIVERING) {
            throw new BusinessException("Cannot remove items while box is DELIVERING");
        }

        if(box.getItems().isEmpty()) {
            throw new BusinessException("Box is already empty");
        }

        box.getItems().clear();
        box.setState(BoxState.IDLE);

        Box updatedBox = boxRepository.save(box);
        return mapToBoxResponse(updatedBox);
    }

    // Helper: decide state after a partial removal
    private void updateStateAfterRemoval(Box box) {
        if (box.getItems().isEmpty()) {
            box.setState(BoxState.IDLE);
        } else if (box.getState() == BoxState.LOADED) {
            box.setState(BoxState.LOADING);
        }
    }

    // 8. DELETE BOX
    @Transactional
    public void deleteBox(String txref) {
        Box box = boxRepository.findByTxref(txref)
                .orElseThrow(() -> new ResourceNotFoundException("Box not found with txref: " + txref));

        // Optional: Prevent deletion if box is currently delivering
        if (box.getState() == BoxState.DELIVERING || box.getState() == BoxState.LOADED) {
            throw new BusinessException("Cannot delete box while it is in " + box.getState() + " state");
        }

        boxRepository.delete(box);
    }

    // MAPPER METHODS
    private BoxResponse mapToBoxResponse(Box box) {
        List<ItemResponse> itemResponses = box.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());

        return BoxResponse.builder()
                .id(box.getId())
                .txref(box.getTxref())
                .weightLimit(box.getWeightLimit())
                .batteryCapacity(box.getBatteryCapacity())
                .state(box.getState())
                .currentWeight(box.getCurrentWeight())
                .items(itemResponses)
                .build();
    }

    private ItemResponse mapToItemResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .weight(item.getWeight())
                .code(item.getCode())
                .build();
    }
}