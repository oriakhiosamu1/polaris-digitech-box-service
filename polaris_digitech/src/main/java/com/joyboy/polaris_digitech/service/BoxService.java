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

        // After successful loading, move to LOADED
        box.setState(BoxState.LOADED);

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