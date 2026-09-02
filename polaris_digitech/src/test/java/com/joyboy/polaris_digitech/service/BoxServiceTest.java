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
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("Should load items successfully")
    void loadBox_Success() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("Medicine");
        itemRequest.setWeight(150);
        itemRequest.setCode("MED_001");

        LoadBoxRequest loadRequest = new LoadBoxRequest();
        loadRequest.setItems(List.of(itemRequest));

        when(boxRepository.findByTxref("BOX001")).thenReturn(Optional.of(sampleBox));
        when(itemRepository.existsByCode("MED_001")).thenReturn(false);
        when(boxRepository.save(any(Box.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoxResponse response = boxService.loadBox("BOX001", loadRequest);

        assertEquals(BoxState.LOADED, response.getState());
        assertEquals(150, response.getCurrentWeight());
        assertEquals(1, response.getItems().size());
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
        // Removed the unnecessary existsByCode stubbing

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
}