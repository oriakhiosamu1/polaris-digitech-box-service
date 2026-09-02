package com.joyboy.polaris_digitech.config;

import com.joyboy.polaris_digitech.model.Box;
import com.joyboy.polaris_digitech.model.BoxState;
import com.joyboy.polaris_digitech.repository.BoxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final BoxRepository boxRepository;

    @Override
    public void run(String... args) {
        if (boxRepository.count() == 0) {
            log.info("Loading sample boxes...");

            Box box1 = Box.builder()
                    .txref("BOX001")
                    .weightLimit(500)
                    .batteryCapacity(100)
                    .state(BoxState.IDLE)
                    .build();

            Box box2 = Box.builder()
                    .txref("BOX002")
                    .weightLimit(400)
                    .batteryCapacity(80)
                    .state(BoxState.IDLE)
                    .build();

            Box box3 = Box.builder()
                    .txref("BOX003")
                    .weightLimit(300)
                    .batteryCapacity(20)          // Below 25% → cannot be loaded
                    .state(BoxState.IDLE)
                    .build();

            Box box4 = Box.builder()
                    .txref("BOX004")
                    .weightLimit(450)
                    .batteryCapacity(55)
                    .state(BoxState.IDLE)
                    .build();

            Box box5 = Box.builder()
                    .txref("BOX005")
                    .weightLimit(250)
                    .batteryCapacity(90)
                    .state(BoxState.IDLE)
                    .build();

            boxRepository.save(box1);
            boxRepository.save(box2);
            boxRepository.save(box3);
            boxRepository.save(box4);
            boxRepository.save(box5);

            log.info("Sample boxes loaded successfully!");
        } else {
            log.info("Boxes already exist. Skipping data loading.");
        }
    }
}