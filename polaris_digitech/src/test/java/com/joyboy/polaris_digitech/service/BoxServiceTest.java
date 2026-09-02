package com.joyboy.polaris_digitech.service;

import com.joyboy.polaris_digitech.dto.*;
import com.joyboy.polaris_digitech.exception.BusinessException;
import com.joyboy.polaris_digitech.exception.ResourceNotFoundException;
import com.joyboy.polaris_digitech.model.Box;
import com.joyboy.polaris_digitech.model.BoxState;
import com.joyboy.polaris_digitech.model.Item;
import com.joyboy.polaris_digitech.repository.BoxRepository;
import com.joyboy.polaris_digitech.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoxServiceTest {

    @Mock
    private BoxRepository boxRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BoxService boxService;

    private Box sampleBox;

    @BeforeEach
    void setUp() {
        sampleBox = Box.builder()
                .id(1L)
                .txref("BOX001")
                .weightLimit(500)
                .batteryCapacity(100)
                .state(BoxState.IDLE)
                .build();
    }

    // ==================== CREATE BOX ====================

    @Test
    @DisplayName("Should create box successfully")
    void createBox_Success() {
        BoxRequest request = new BoxRequest();
        request.setTxref("BOX010");
        request.setWeightLimit(400);
        request.setBatteryCapacity(90);

        when(boxRepository.existsByTxref("BOX010")).thenReturn(false);
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.createBox(request);

        assertNotNull(response);
        assertEquals("BOX010", response.getTxref());
        assertEquals(BoxState.IDLE, response.getState());
        verify(boxRepository).save(any(Box.class));
    }

    @Test
    @DisplayName("Should throw exception when txref already exists")
    void createBox_DuplicateTxref() {
        BoxRequest request = new BoxRequest();
        request.setTxref("BOX001");
        request.setWeightLimit(400);
        request.setBatteryCapacity(90);

        when(boxRepository.existsByTxref("BOX001")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.createBox(request));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(boxRepository, never()).save(any());
    }

    // ==================== LOAD BOX ====================

    @Test
    @DisplayName("Should stay in LOADING state when box is partially loaded")
    void loadBox_PartialLoad_StaysLoading() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("Medicine");
        itemRequest.setWeight(150); // well under weightLimit of 500
        itemRequest.setCode("MED_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(itemRepository.existsByCode("MED_001")).thenReturn(false);
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.loadBox("BOX001", loadRequest);

        assertEquals(BoxState.LOADING, response.getState());
        assertEquals(150, response.getCurrentWeight());
        assertEquals(1, response.getItems().size());
    }

    @Test
    @DisplayName("Should become LOADED when box reaches its weight limit")
    void loadBox_FullLoad_BecomesLoaded() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("Medicine");
        itemRequest.setWeight(500); // exactly the weightLimit
        itemRequest.setCode("MED_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(itemRepository.existsByCode("MED_001")).thenReturn(false);
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.loadBox("BOX001", loadRequest);

        assertEquals(BoxState.LOADED, response.getState());
        assertEquals(500, response.getCurrentWeight());
    }

    @Test
    @DisplayName("Should throw exception when battery is below 25%")
    void loadBox_BatteryTooLow() {
        sampleBox.setBatteryCapacity(20); // Below 25%

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("Medicine");
        itemRequest.setWeight(100);
        itemRequest.setCode("MED_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.loadBox("BOX001", loadRequest));

        assertTrue(exception.getMessage().contains("Battery level is below 25%"));
        verify(boxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when weight limit is exceeded")
    void loadBox_WeightExceeded() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("HeavyItem");
        itemRequest.setWeight(600); // Exceeds 500
        itemRequest.setCode("HVY_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.loadBox("BOX001", loadRequest));

        assertTrue(exception.getMessage().contains("exceeds weight limit"));
        verify(boxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when box is not in loadable state")
    void loadBox_InvalidState() {
        sampleBox.setState(BoxState.DELIVERING);

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("Medicine");
        itemRequest.setWeight(100);
        itemRequest.setCode("MED_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.loadBox("BOX001", loadRequest));

        assertTrue(exception.getMessage().contains("cannot be loaded"));
    }

    @Test
    @DisplayName("Should throw exception when item code already exists")
    void loadBox_DuplicateItemCode() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("Medicine");
        itemRequest.setWeight(100);
        itemRequest.setCode("MED_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(itemRepository.existsByCode("MED_001")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.loadBox("BOX001", loadRequest));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(boxRepository, never()).save(any());
    }

    // ==================== GET BATTERY ====================

    @Test
    @DisplayName("Should return battery level")
    void getBatteryLevel_Success() {
        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BatteryResponse response = boxService.getBatteryLevel("BOX001");

        assertEquals("BOX001", response.getTxref());
        assertEquals(100, response.getBatteryCapacity());
    }

    @Test
    @DisplayName("Should throw exception when box not found")
    void getBatteryLevel_NotFound() {
        when(boxRepository.findByTxref("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> boxService.getBatteryLevel("INVALID"));
    }

    // ==================== GET LOADED ITEMS ====================

    @Test
    @DisplayName("Should return loaded items")
    void getLoadedItems_Success() {
        Item item = Item.builder()
                .id(1L)
                .name("Medicine")
                .weight(150)
                .code("MED_001")
                .build();
        sampleBox.addItem(item);

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        List<ItemResponse> items = boxService.getLoadedItems("BOX001");

        assertEquals(1, items.size());
        assertEquals("MED_001", items.get(0).getCode());
    }

    // ==================== GET AVAILABLE BOXES ====================

    @Test
    @DisplayName("Should return boxes with remaining capacity")
    void getAvailableBoxes_ReturnsBoxesWithCapacity() {
        sampleBox.setState(BoxState.LOADING);
        Item item = Item.builder().id(1L).name("Medicine").weight(100).code("MED_001").build();
        sampleBox.addItem(item); // currentWeight 100 < weightLimit 500

        when(boxRepository.findAvailableBoxes(anyList())).thenReturn(List.of(sampleBox));

        List<BoxResponse> result = boxService.getAvailableBoxes();

        assertEquals(1, result.size());
        assertEquals("BOX001", result.get(0).getTxref());
    }

    @Test
    @DisplayName("Should exclude boxes with no remaining capacity")
    void getAvailableBoxes_ExcludesFullBox() {
        sampleBox.setState(BoxState.LOADING);
        Item item = Item.builder().id(1L).name("Medicine").weight(500).code("MED_001").build();
        sampleBox.addItem(item); // currentWeight == weightLimit, no capacity left

        when(boxRepository.findAvailableBoxes(anyList())).thenReturn(List.of(sampleBox));

        List<BoxResponse> result = boxService.getAvailableBoxes();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when repository finds no eligible boxes")
    void getAvailableBoxes_NoneEligible() {
        // e.g. all boxes are DELIVERING or below 25% battery — filtered at the query level
        when(boxRepository.findAvailableBoxes(anyList())).thenReturn(Collections.emptyList());

        List<BoxResponse> result = boxService.getAvailableBoxes();

        assertTrue(result.isEmpty());
    }

    // ==================== DELETE BOX ====================

    @Test
    @DisplayName("Should delete box successfully when in a deletable state")
    void deleteBox_Success() {
        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        boxService.deleteBox("BOX001");

        verify(boxRepository).delete(sampleBox);
    }

    @Test
    @DisplayName("Should throw exception when deleting a box that is DELIVERING")
    void deleteBox_BlockedWhileDelivering() {
        sampleBox.setState(BoxState.DELIVERING);

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.deleteBox("BOX001"));

        assertTrue(exception.getMessage().contains("Cannot delete box"));
        verify(boxRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting a box that is LOADED")
    void deleteBox_BlockedWhileLoaded() {
        sampleBox.setState(BoxState.LOADED);

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.deleteBox("BOX001"));

        assertTrue(exception.getMessage().contains("Cannot delete box"));
        verify(boxRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting a box that does not exist")
    void deleteBox_NotFound() {
        when(boxRepository.findByTxref("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> boxService.deleteBox("INVALID"));
    }

    // ==================== REMOVE ITEMS (via BoxUpdateRequest) ====================

    @Test
    @DisplayName("Should remove specific items and revert LOADED box to LOADING")
    void removeItems_PartialRemoval_RevertsToLoading() {
        sampleBox.setState(BoxState.LOADED);
        Item item1 = Item.builder().id(1L).name("Medicine").weight(300).code("MED_001").build();
        Item item2 = Item.builder().id(2L).name("Bandage").weight(200).code("BND_001").build();
        sampleBox.addItem(item1);
        sampleBox.addItem(item2); // currentWeight = 500 = weightLimit -> LOADED

        BoxUpdateRequest request = new BoxUpdateRequest();
        request.setItemCodes(List.of("BND_001"));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.removeItems("BOX001", request);

        assertEquals(BoxState.LOADING, response.getState());
        assertEquals(300, response.getCurrentWeight());
        assertEquals(1, response.getItems().size());
    }

    @Test
    @DisplayName("Should set state to IDLE when removing the last remaining item")
    void removeItems_RemovingLastItem_SetsIdle() {
        sampleBox.setState(BoxState.LOADING);
        Item item = Item.builder().id(1L).name("Medicine").weight(150).code("MED_001").build();
        sampleBox.addItem(item);

        BoxUpdateRequest request = new BoxUpdateRequest();
        request.setItemCodes(List.of("MED_001"));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.removeItems("BOX001", request);

        assertEquals(BoxState.IDLE, response.getState());
        assertEquals(0, response.getCurrentWeight());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when removing a code not present on the box")
    void removeItems_CodeNotFound() {
        Item item = Item.builder().id(1L).name("Medicine").weight(150).code("MED_001").build();
        sampleBox.addItem(item);

        BoxUpdateRequest request = new BoxUpdateRequest();
        request.setItemCodes(List.of("DOES_NOT_EXIST"));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        assertThrows(ResourceNotFoundException.class,
                () -> boxService.removeItems("BOX001", request));

        verify(boxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when removing items while box is DELIVERING")
    void removeItems_BlockedWhileDelivering() {
        sampleBox.setState(BoxState.DELIVERING);
        Item item = Item.builder().id(1L).name("Medicine").weight(150).code("MED_001").build();
        sampleBox.addItem(item);

        BoxUpdateRequest request = new BoxUpdateRequest();
        request.setItemCodes(List.of("MED_001"));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.removeItems("BOX001", request));

        assertTrue(exception.getMessage().contains("DELIVERING"));
        verify(boxRepository, never()).save(any());
    }

    // ==================== REMOVE ALL ITEMS ====================

    @Test
    @DisplayName("Should remove all items and set state to IDLE")
    void removeAllItems_Success() {
        sampleBox.setState(BoxState.LOADED);
        Item item1 = Item.builder().id(1L).name("Medicine").weight(300).code("MED_001").build();
        Item item2 = Item.builder().id(2L).name("Bandage").weight(200).code("BND_001").build();
        sampleBox.addItem(item1);
        sampleBox.addItem(item2);

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.removeAllItems("BOX001");

        assertEquals(BoxState.IDLE, response.getState());
        assertEquals(0, response.getCurrentWeight());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when removing all items while box is DELIVERING")
    void removeAllItems_BlockedWhileDelivering() {
        sampleBox.setState(BoxState.DELIVERING);

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> boxService.removeAllItems("BOX001"));

        assertTrue(exception.getMessage().contains("DELIVERING"));
        verify(boxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when removing all items from a box that does not exist")
    void removeAllItems_NotFound() {
        when(boxRepository.findByTxref("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> boxService.removeAllItems("INVALID"));
    }
}